package com.hunter1712.infusedclasses;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * One player's whole status stack as a single value: the snapshot decision and the wire
 * payload in one. Built from one runtime state read at a {@code now} — or from no state at
 * all for a classless player — so it can be asserted without a player; the S2C codec
 * carries it to the client verbatim.
 *
 * <p>{@link #movedSince} is the emission guard: it compares through the {@link #FIELDS}
 * table — lane and window <em>armed flags</em> (not raw remainders, which decay every
 * rebuild) plus every discrete field — class, load flags, stacks, stance, flip target,
 * Streak, outcome, and the Momentum grace stamp, an event the client cannot derive: a
 * hit at the stack cap re-stamps the clock without moving the count, so the stamp moving
 * is the move. A quiet second reads as no move; a fire, a spend, a stack move, a switch,
 * a respawn and a join all read as moves.
 *
 * <p>The empty class id is a state, not an absence: a classless join sends one so the
 * client hides the stack by fact. The id is always the resolved definition's own — the
 * registry lookup happens once, before the snapshot, so an unknown stored id has already
 * collapsed to no class. {@code flipTarget} is the live flip window's landing, the flip
 * record's own read, null when no flip window is live.
 */
public record PlayerStatusPayload(
		String classId,
		long rushRemainingMs,
		long rendRemainingMs,
		long rotateRemainingMs,
		long oathRemainingMs,
		long rouletteRemainingMs,
		long allInRemainingMs,
		long rushWindowRemainingMs,
		boolean rushBonusPending,
		long rendWindowRemainingMs,
		boolean rendBonusPending,
		long flipWindowRemainingMs,
		RotateAbility.Stance flipTarget,
		boolean impetusPending,
		long rouletteWindowRemainingMs,
		RouletteAbility.Outcome rouletteOutcome,
		long aequilibriumWindowRemainingMs,
		long allInWindowRemainingMs,
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

	/**
	 * One snapshot field: its value, whether a change in it is movement worth a packet,
	 * and how it leaves one. The kind is a type, never a flag: a row's compare rule and
	 * its value always come from the same declaration.
	 */
	private interface Field {
		/** The field's value on a payload. */
		Object value(PlayerStatusPayload payload);

		boolean moved(PlayerStatusPayload self, PlayerStatusPayload prev);

		/** Writes the field's value; the table's shared encode path. */
		default void write(FriendlyByteBuf buf, PlayerStatusPayload payload) {
			writeValue(buf, value(payload));
		}
	}

	/** A decaying remainder: compared armed-or-not at one clock, never by the raw number. */
	private record ArmedField(ToLongFunction<PlayerStatusPayload> get) implements Field {
		@Override
		public Object value(PlayerStatusPayload payload) {
			return get.applyAsLong(payload);
		}

		@Override
		public boolean moved(PlayerStatusPayload self, PlayerStatusPayload prev) {
			return armed(get.applyAsLong(self)) != armed(get.applyAsLong(prev));
		}
	}

	/** A discrete fact: compared by equality. */
	private record ExactField<T>(Function<PlayerStatusPayload, T> get) implements Field {
		@Override
		public Object value(PlayerStatusPayload payload) {
			return get.apply(payload);
		}

		@Override
		public boolean moved(PlayerStatusPayload self, PlayerStatusPayload prev) {
			return !Objects.equals(get.apply(self), get.apply(prev));
		}
	}

	/** Derived from another field: written, never compared — the stamp is compared, its remainder is not. */
	private record IgnoredField<T>(Function<PlayerStatusPayload, T> get) implements Field {
		@Override
		public Object value(PlayerStatusPayload payload) {
			return get.apply(payload);
		}

		@Override
		public boolean moved(PlayerStatusPayload self, PlayerStatusPayload prev) {
			return false;
		}
	}

	/**
	 * The field table, in record and wire order: every component once. {@code encode} and
	 * {@code movedSince} walk it; {@code decode} reads in this same order by hand (records
	 * have no table-driven construction) and ProtocolTest's populated round trip pins the
	 * two orders to each other. The Momentum grace <em>remainder</em> is the one ignored
	 * field: it derives from the stamp, which is compared.
	 */
	private static final List<Field> FIELDS = List.of(
			new ExactField<>(PlayerStatusPayload::classId),
			new ArmedField(PlayerStatusPayload::rushRemainingMs),
			new ArmedField(PlayerStatusPayload::rendRemainingMs),
			new ArmedField(PlayerStatusPayload::rotateRemainingMs),
			new ArmedField(PlayerStatusPayload::oathRemainingMs),
			new ArmedField(PlayerStatusPayload::rouletteRemainingMs),
			new ArmedField(PlayerStatusPayload::allInRemainingMs),
			new ArmedField(PlayerStatusPayload::rushWindowRemainingMs),
			new ExactField<>(PlayerStatusPayload::rushBonusPending),
			new ArmedField(PlayerStatusPayload::rendWindowRemainingMs),
			new ExactField<>(PlayerStatusPayload::rendBonusPending),
			new ArmedField(PlayerStatusPayload::flipWindowRemainingMs),
			new ExactField<>(PlayerStatusPayload::flipTarget),
			new ExactField<>(PlayerStatusPayload::impetusPending),
			new ArmedField(PlayerStatusPayload::rouletteWindowRemainingMs),
			new ExactField<>(PlayerStatusPayload::rouletteOutcome),
			new ArmedField(PlayerStatusPayload::aequilibriumWindowRemainingMs),
			new ArmedField(PlayerStatusPayload::allInWindowRemainingMs),
			new ExactField<>(PlayerStatusPayload::momentumStacks),
			new IgnoredField<>(PlayerStatusPayload::momentumGraceRemainingMs),
			new ExactField<>(PlayerStatusPayload::momentumGraceStampMs),
			new ExactField<>(PlayerStatusPayload::stance),
			new ExactField<>(PlayerStatusPayload::streak));

	/**
	 * The snapshot at {@code now}: one runtime state read behind every lane and window
	 * field, the two derived ones documented on the record. The definition arrives
	 * already resolved — this never touches the registry — and is null only for a
	 * classless player, whose reads all come from {@link #NO_STATE} and whose class id
	 * reads empty.
	 */
	public static PlayerStatusPayload of(ClassDefinition def, PlayerRuntime runtime, long now) {
		ConditionalPassive momentum = def == null ? null : def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
		PlayerRuntime.State state = runtime == null ? NO_STATE : runtime.state(now);
		return new PlayerStatusPayload(
				def == null ? "" : def.id().toString(),
				state.rushRemaining(),
				state.rendRemaining(),
				state.rotateRemaining(),
				state.oathRemaining(),
				state.rouletteRemaining(),
				state.allInRemaining(),
				state.rushWindowRemaining(),
				state.rushBonusPending(),
				state.rendWindowRemaining(),
				// A fired Rend window always stamps at least one stack (a zero-stack press
				// fizzles before the arm), so the unspent stamp is the pending flag.
				state.rendBonusStacks() > 0,
				state.flipWindowRemaining(),
				state.flipTarget(),
				state.impetusPending(),
				state.rouletteWindowRemaining(),
				state.rouletteOutcome(),
				state.aequilibriumWindowRemaining(),
				state.allInWindowRemaining(),
				momentum == null ? 0 : runtime.stacks(momentum),
				momentum == null ? 0 : runtime.graceRemaining(momentum, now),
				momentum == null ? 0 : runtime.graceStamp(momentum),
				state.stance(),
				state.streak());
	}

	/** The classless snapshot: empty class id, every lane and window untouched, no state read. */
	public static PlayerStatusPayload classless() {
		return of(null, null, 0L);
	}

	/**
	 * Whether anything a client cannot derive moved since the given snapshot, walked
	 * through the field table: class, lane/window armed flags, load flags, stacks, stance,
	 * flip target, Streak, outcome, and the Momentum grace stamp. Raw remainders decay
	 * with time and are never compared; a rebuild after a quiet second is no move. A null
	 * previous snapshot is always a move (nothing was sent).
	 */
	public boolean movedSince(PlayerStatusPayload prev) {
		if (prev == null) {
			return true;
		}
		for (Field field : FIELDS) {
			if (field.moved(this, prev)) {
				return true;
			}
		}
		return false;
	}

	private static boolean armed(long remainingMs) {
		return remainingMs > 0;
	}

	/** Encodes in table order; the nullable enums travel as an empty name, decoded back to null. */
	private static void encode(FriendlyByteBuf buf, PlayerStatusPayload p) {
		for (Field field : FIELDS) {
			field.write(buf, p);
		}
	}

	/** One field value by its type: the codec's only type map, so the table owns the order. */
	private static void writeValue(FriendlyByteBuf buf, Object value) {
		switch (value) {
			case null -> buf.writeUtf("");
			case String s -> buf.writeUtf(s);
			case Long l -> buf.writeLong(l);
			case Boolean b -> buf.writeBoolean(b);
			case Integer i -> buf.writeInt(i);
			case RotateAbility.Stance s -> writeStance(buf, s);
			case RouletteAbility.Outcome o -> writeOutcome(buf, o);
			default -> throw new IllegalStateException("Unencodable snapshot field: " + value);
		}
	}

	/** Reads in the table's order by hand; the populated round trip in ProtocolTest pins the order. */
	private static PlayerStatusPayload decode(FriendlyByteBuf buf) {
		return new PlayerStatusPayload(
				buf.readUtf(),
				buf.readLong(),
				buf.readLong(),
				buf.readLong(),
				buf.readLong(),
				buf.readLong(),
				buf.readLong(),
				buf.readLong(),
				buf.readBoolean(),
				buf.readLong(),
				buf.readBoolean(),
				buf.readLong(),
				readStance(buf),
				buf.readBoolean(),
				buf.readLong(),
				readOutcome(buf),
				buf.readLong(),
				buf.readLong(),
				buf.readInt(),
				buf.readLong(),
				buf.readLong(),
				readStance(buf),
				buf.readInt());
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
