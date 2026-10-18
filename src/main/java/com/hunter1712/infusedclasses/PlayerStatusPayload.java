package com.hunter1712.infusedclasses;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * One player's whole status stack as a single value: the snapshot decision and the wire
 * payload in one. Built from the caller's one runtime state read at a {@code now} — or from
 * no state at all for a classless player — so it can be asserted without a player; the S2C
 * codec carries it to the client verbatim.
 *
 * <p>Per-lane state is one {@link LaneRead} per lane, so the shape is the class id, the lane
 * map and the handful of facts that are not per-lane. {@code of}, the codec and
 * {@link #movedSince} all walk {@link Lane#values()}: a seventh window costs this file nothing.
 *
 * <p>{@link #movedSince} is the emission guard: it compares lane and window <em>armed flags</em>
 * (not raw remainders, which decay every rebuild) plus every discrete field — class, flip
 * target, Roulette outcome, Momentum stacks and grace stamp, stance and Streak. The Momentum
 * grace <em>stamp</em> is compared, never its remainder: an event the client cannot derive, a
 * hit at the stack cap re-stamping the clock without moving the count. A quiet second reads as
 * no move; a fire, a spend, a stack move, a switch, a respawn and a join all read as moves.
 *
 * <p>The empty class id is a state, not an absence: a classless join sends one so the
 * client hides the stack by fact. The id is always the resolved definition's own — the
 * registry lookup happens once, before the snapshot, so an unknown stored id has already
 * collapsed to no class.
 */
public record PlayerStatusPayload(
		String classId,
		Map<Lane, LaneRead> lanes,
		RotateAbility.Stance flipTarget,
		RouletteAbility.Outcome rouletteOutcome,
		int momentumStacks,
		long momentumGraceRemainingMs,
		long momentumGraceStampMs,
		RotateAbility.Stance stance,
		int streak
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
		return lanes.get(lane);
	}

	/**
	 * The snapshot at {@code now}: the caller's one runtime state read behind every lane
	 * read, the non-lane facts taken off the same read. The definition arrives already
	 * resolved — this never touches the registry — and is null only for a classless player,
	 * whose reads all come from {@link #NO_STATE} (a null state is that same empty shape)
	 * and whose class id reads empty. Every classed caller passes the state read it took.
	 * The lane map is copied, so the payload never shares the map it was built from.
	 */
	public static PlayerStatusPayload of(ClassDefinition def, PlayerRuntime runtime, PlayerRuntime.State state, long now) {
		ConditionalPassive momentum = def == null ? null : def.passiveForTrigger(Trigger.ON_HIT_DEALT);
		PlayerRuntime.State read = state == null ? NO_STATE : state;
		return new PlayerStatusPayload(
				def == null ? "" : def.id().toString(),
				Map.copyOf(read.lanes()),
				read.flipTarget(),
				read.rouletteOutcome(),
				momentum == null ? 0 : runtime.stacks(momentum),
				momentum == null ? 0 : runtime.graceRemaining(momentum, now),
				momentum == null ? 0 : runtime.graceStamp(momentum),
				read.stance(),
				read.streak());
	}

	/** The classless snapshot: empty class id, every lane and window untouched, no state read. */
	public static PlayerStatusPayload classless() {
		return of(null, null, null, 0L);
	}

	/**
	 * Whether anything a client cannot derive moved since the given snapshot: the class id,
	 * every lane's two armed flags and one-shot flag, then the discrete facts. Raw remainders
	 * decay with time and are never compared; a rebuild after a quiet second is no move. A
	 * null previous snapshot is always a move (nothing was sent).
	 */
	public boolean movedSince(PlayerStatusPayload prev) {
		if (prev == null) {
			return true;
		}
		if (!classId.equals(prev.classId())
				|| !Objects.equals(flipTarget, prev.flipTarget())
				|| !Objects.equals(rouletteOutcome, prev.rouletteOutcome())
				|| momentumStacks != prev.momentumStacks()
				|| momentumGraceStampMs != prev.momentumGraceStampMs()
				|| !Objects.equals(stance, prev.stance())
				|| streak != prev.streak()) {
			return true;
		}
		for (Lane lane : Lane.values()) {
			LaneRead self = lanes.get(lane);
			LaneRead before = prev.lane(lane);
			if (armed(self.remainingMs()) != armed(before.remainingMs())
					|| armed(self.windowRemainingMs()) != armed(before.windowRemainingMs())
					|| self.windowPending() != before.windowPending()) {
				return true;
			}
		}
		return false;
	}

	private static boolean armed(long remainingMs) {
		return remainingMs > 0;
	}

	/** Encodes the class id, one read per lane in lane order, then the discrete facts. */
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
		return new PlayerStatusPayload(classId, lanes, flipTarget, outcome,
				momentumStacks, graceRemaining, graceStamp, stance, streak);
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
