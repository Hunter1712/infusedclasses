package com.hunter1712.infusedclasses;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * One player's whole status strip as a single value: the snapshot decision and the wire
 * payload in one. Built only from the transient runtime plus a {@code now}, so it can be
 * asserted without a player; the S2C codec carries it to the client verbatim.
 *
 * <p>{@link #movedSince} is the emission guard: it compares lane and window <em>armed
 * flags</em> (not raw remainders, which decay every rebuild) plus every discrete field —
 * class, load flags, stacks, stance, flip target, Streak, outcome. A quiet second reads
 * as no move; a fire, a spend, a stack move, a switch, a respawn and a join all read as
 * moves.
 *
 * <p>The empty class id is a state, not an absence: a classless join sends one so the
 * client hides the strip by fact. A class id the registry does not know still travels
 * verbatim; only a missing choice reads as empty. {@code flipTarget} is the stance the
 * live flip window runs toward, null when no flip window is live: a flip lands in the
 * stance it targets, so the flip's identity is the stance while its window runs.
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
		RotateAbility.Stance stance,
		int streak
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PlayerStatusPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "player_status"));

	/**
	 * Hand-rolled rather than a composite: the snapshot is wider than the composite
	 * overloads and carries two nullable enums. Enums travel by name, so a reorder
	 * cannot silently re-band a client; an empty name is null.
	 */
	public static final StreamCodec<FriendlyByteBuf, PlayerStatusPayload> STREAM_CODEC =
			StreamCodec.of(PlayerStatusPayload::encode, PlayerStatusPayload::decode);

	/**
	 * The snapshot at {@code now}: every field is a direct runtime read, the two derived
	 * ones documented on the record. A missing class reads as the empty class id with
	 * every lane and window untouched.
	 */
	public static PlayerStatusPayload of(Identifier classId, PlayerRuntime runtime, long now) {
		ClassDefinition def = ClassRegistry.byId(classId);
		ConditionalPassive momentum = def == null ? null : def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
		return new PlayerStatusPayload(
				classId == null ? "" : classId.toString(),
				runtime.rushRemaining(now),
				runtime.rendRemaining(now),
				runtime.rotateRemaining(now),
				runtime.oathRemaining(now),
				runtime.rouletteRemaining(now),
				runtime.allInRemaining(now),
				runtime.rushWindowRemaining(now),
				runtime.hasRushBonusPending(now),
				runtime.rendWindowRemaining(now),
				runtime.hasRendBonusPending(now),
				runtime.flipWindowRemaining(now),
				runtime.flipWindowRemaining(now) > 0 ? runtime.stance() : null,
				runtime.hasImpetusPending(now),
				runtime.rouletteWindowRemaining(now),
				runtime.rouletteOutcome(now),
				runtime.aequilibriumWindowRemaining(now),
				runtime.allInWindowRemaining(now),
				momentum == null ? 0 : runtime.stacks(momentum),
				momentum == null ? 0 : runtime.graceRemaining(momentum, now),
				runtime.stance(),
				runtime.streak());
	}

	/**
	 * Whether anything a client cannot derive moved since the given snapshot: class,
	 * lane/window armed flags, load flags, stacks, stance, flip target, Streak, outcome.
	 * Raw remainders decay with time and are never compared; a rebuild after a quiet
	 * second is no move. A null previous snapshot is always a move (nothing was sent).
	 */
	public boolean movedSince(PlayerStatusPayload prev) {
		if (prev == null) {
			return true;
		}
		return !classId.equals(prev.classId)
				|| armed(rushRemainingMs) != armed(prev.rushRemainingMs)
				|| armed(rendRemainingMs) != armed(prev.rendRemainingMs)
				|| armed(rotateRemainingMs) != armed(prev.rotateRemainingMs)
				|| armed(oathRemainingMs) != armed(prev.oathRemainingMs)
				|| armed(rouletteRemainingMs) != armed(prev.rouletteRemainingMs)
				|| armed(allInRemainingMs) != armed(prev.allInRemainingMs)
				|| armed(rushWindowRemainingMs) != armed(prev.rushWindowRemainingMs)
				|| rushBonusPending != prev.rushBonusPending
				|| armed(rendWindowRemainingMs) != armed(prev.rendWindowRemainingMs)
				|| rendBonusPending != prev.rendBonusPending
				|| armed(flipWindowRemainingMs) != armed(prev.flipWindowRemainingMs)
				|| flipTarget != prev.flipTarget
				|| impetusPending != prev.impetusPending
				|| armed(rouletteWindowRemainingMs) != armed(prev.rouletteWindowRemainingMs)
				|| rouletteOutcome != prev.rouletteOutcome
				|| armed(aequilibriumWindowRemainingMs) != armed(prev.aequilibriumWindowRemainingMs)
				|| armed(allInWindowRemainingMs) != armed(prev.allInWindowRemainingMs)
				|| momentumStacks != prev.momentumStacks
				|| stance != prev.stance
				|| streak != prev.streak;
	}

	private static boolean armed(long remainingMs) {
		return remainingMs > 0;
	}

	private static void encode(FriendlyByteBuf buf, PlayerStatusPayload p) {
		buf.writeUtf(p.classId());
		buf.writeLong(p.rushRemainingMs());
		buf.writeLong(p.rendRemainingMs());
		buf.writeLong(p.rotateRemainingMs());
		buf.writeLong(p.oathRemainingMs());
		buf.writeLong(p.rouletteRemainingMs());
		buf.writeLong(p.allInRemainingMs());
		buf.writeLong(p.rushWindowRemainingMs());
		buf.writeBoolean(p.rushBonusPending());
		buf.writeLong(p.rendWindowRemainingMs());
		buf.writeBoolean(p.rendBonusPending());
		buf.writeLong(p.flipWindowRemainingMs());
		writeStance(buf, p.flipTarget());
		buf.writeBoolean(p.impetusPending());
		buf.writeLong(p.rouletteWindowRemainingMs());
		writeOutcome(buf, p.rouletteOutcome());
		buf.writeLong(p.aequilibriumWindowRemainingMs());
		buf.writeLong(p.allInWindowRemainingMs());
		buf.writeInt(p.momentumStacks());
		buf.writeLong(p.momentumGraceRemainingMs());
		writeStance(buf, p.stance());
		buf.writeInt(p.streak());
	}

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
