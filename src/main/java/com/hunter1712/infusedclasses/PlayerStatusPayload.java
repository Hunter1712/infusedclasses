package com.hunter1712.infusedclasses;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;

/**
 * One player's whole status stack as a single value: the snapshot decision and the wire
 * payload in one. It composes the two reads the runtime already answers — the lane and
 * discrete-fact {@link PlayerRuntime.State} and the bar's {@link PlayerRuntime.BarRead} —
 * plus the class id, so the schema is not restated field by field between builder, codec
 * and emission guard.
 *
 * <p>{@code of} takes the caller's one runtime state read at a {@code now} (or no state at
 * all for a classless player), so it can be asserted without a player; the S2C codec carries
 * it to the client verbatim. The accessors below keep the flat names the client reads, so a
 * composed snapshot costs consumers nothing.
 *
 * <p>{@link #movedSince} is the emission guard: the class id, the bar's count and feed
 * <em>stamp</em> (never its remainder), then {@link PlayerRuntime.State#movedSince}, which
 * compares lane and window <em>armed flags</em> rather than raw remainders. A quiet second
 * reads as no move; a fire, a spend, a stack move, a switch, a respawn and a join all read
 * as moves.
 *
 * <p>The empty class id is a state, not an absence: a classless join sends one so the
 * client hides the stack by fact. The id is always the resolved definition's own — the
 * registry lookup happens once, before the snapshot, so an unknown stored id has already
 * collapsed to no class.
 */
public record PlayerStatusPayload(
		String classId,
		PlayerRuntime.State state,
		PlayerRuntime.BarRead bar
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PlayerStatusPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "player_status"));

	/**
	 * Hand-rolled rather than a composite: the snapshot is wider than the composite
	 * overloads and carries two nullable enums. Enums travel by name, so a reorder
	 * cannot silently re-band a client; an empty name is null.
	 */
	public static final StreamCodec<FriendlyByteBuf, PlayerStatusPayload> STREAM_CODEC =
			StreamCodec.of(PlayerStatusPayload::encode, PlayerStatusPayload::decode);

	/** A fresh runtime's truth: what "no state" reads as. One shared value; nothing mutates it. */
	private static final PlayerRuntime.State NO_STATE = new PlayerRuntime().state(0L);

	/** A lane's whole read: the payload's half of the runtime state's own. */
	public LaneRead lane(Lane lane) {
		return state.lane(lane);
	}

	/** The live flip window's landing, null when no flip window is live. */
	public RotateAbility.Stance flipTarget() {
		return state.flipTarget();
	}

	/** The live spin's band, null when no outcome window is live. */
	public RouletteAbility.Outcome rouletteOutcome() {
		return state.rouletteOutcome();
	}

	/** The bar's count, 0 for a classless player or one carrying no bar. */
	public int momentumStacks() {
		return bar.stacks();
	}

	/** Millis until the bar's next stack is owed. A countdown, so the stack draws it; never compared. */
	public long momentumGraceRemainingMs() {
		return bar.graceRemainingMs();
	}

	/** The feed clock's last stamp; compared for movement, never read as time. */
	public long momentumGraceStampMs() {
		return bar.graceStampMs();
	}

	/** The Legionary's current stance; Scutum until the first Rotate. */
	public RotateAbility.Stance stance() {
		return state.stance();
	}

	/** The Gambler's loaded pocket. */
	public int streak() {
		return state.streak();
	}

	/**
	 * The snapshot at {@code now}: the caller's one runtime state read behind every lane
	 * read and every discrete fact, plus one bar read. The definition arrives already
	 * resolved — this never touches the registry — and is null only for a classless player,
	 * whose reads all come from {@link #NO_STATE} (a null state is that same empty shape)
	 * and whose class id reads empty. Every classed caller passes the state read it took.
	 */
	public static PlayerStatusPayload of(ClassDefinition def, PlayerRuntime runtime,
			PlayerRuntime.State state, long now) {
		ConditionalPassive bar = def == null ? null : def.passiveForTrigger(Trigger.ON_HIT_DEALT);
		PlayerRuntime.State read = state == null ? NO_STATE : state;
		return new PlayerStatusPayload(
				def == null ? "" : def.id().toString(),
				read,
				bar == null ? PlayerRuntime.BarRead.NONE : runtime.bar(bar, now));
	}

	/** The classless snapshot: empty class id, every lane and window untouched, no state read. */
	public static PlayerStatusPayload classless() {
		return of(null, null, null, 0L);
	}

	/** Whether anything a client cannot derive moved since the given snapshot; a null previous snapshot is always a move. */
	public boolean movedSince(PlayerStatusPayload prev) {
		if (prev == null) {
			return true;
		}
		return !classId.equals(prev.classId())
				|| bar.stacks() != prev.bar().stacks()
				|| bar.graceStampMs() != prev.bar().graceStampMs()
				|| state.movedSince(prev.state());
	}

	/** Encodes the class id, one read per lane in lane order, then the discrete facts and the bar. */
	private static void encode(FriendlyByteBuf buf, PlayerStatusPayload p) {
		buf.writeUtf(p.classId());
		for (Lane lane : Lane.values()) {
			LaneRead read = p.lane(lane);
			buf.writeLong(read.remainingMs());
			buf.writeLong(read.windowRemainingMs());
			buf.writeBoolean(read.windowPending());
		}
		writeStance(buf, p.flipTarget());
		writeOutcome(buf, p.rouletteOutcome());
		buf.writeInt(p.momentumStacks());
		buf.writeLong(p.momentumGraceRemainingMs());
		buf.writeLong(p.momentumGraceStampMs());
		writeStance(buf, p.stance());
		buf.writeInt(p.streak());
	}

	/** Reads what {@link #encode} writes, in the same order; ProtocolTest's populated round trip pins the two. */
	private static PlayerStatusPayload decode(FriendlyByteBuf buf) {
		String classId = buf.readUtf();
		EnumMap<Lane, LaneRead> lanes = new EnumMap<>(Lane.class);
		for (Lane lane : Lane.values()) {
			lanes.put(lane, new LaneRead(buf.readLong(), buf.readLong(), buf.readBoolean()));
		}
		RotateAbility.Stance flipTarget = readStance(buf);
		RouletteAbility.Outcome outcome = readOutcome(buf);
		int momentumStacks = buf.readInt();
		long graceRemaining = buf.readLong();
		long graceStamp = buf.readLong();
		RotateAbility.Stance stance = readStance(buf);
		int streak = buf.readInt();
		PlayerRuntime.State state = new PlayerRuntime.State(lanes, flipTarget, outcome, stance, streak);
		return new PlayerStatusPayload(classId, state,
				new PlayerRuntime.BarRead(momentumStacks, graceRemaining, graceStamp));
	}

	private static void writeStance(FriendlyByteBuf buf, RotateAbility.Stance stance) {
		buf.writeUtf(stance == null ? "" : stance.name());
	}

	private static RotateAbility.Stance readStance(FriendlyByteBuf buf) {
		String name = buf.readUtf();
		return name.isEmpty() ? null : RotateAbility.Stance.valueOf(name);
	}

	private static void writeOutcome(FriendlyByteBuf buf, RouletteAbility.Outcome outcome) {
		buf.writeUtf(outcome == null ? "" : outcome.name());
	}

	private static RouletteAbility.Outcome readOutcome(FriendlyByteBuf buf) {
		String name = buf.readUtf();
		return name.isEmpty() ? null : RouletteAbility.Outcome.valueOf(name);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
