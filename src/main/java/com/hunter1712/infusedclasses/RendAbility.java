package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

/**
 * Rend decisions: the Juggernaut's Momentum spender on {@code G}. Pure gate-and-arm shape:
 * empowers the next melee hit landed inside the window for {@code BASE_HP + stacks *
 * HP_PER_STACK} bonus damage, and consumes every Momentum stack up front, on its own 60s
 * lane. Zero stacks fizzles with a {@code No Momentum} line and arms nothing; an empowered
 * window left unswung simply expires, stacks already spent. The consume plan rides in the
 * result ({@link Result#consume()}): the entry asks the passive applier to consume the
 * trigger-found passive, and the damage hook spends the empower snapshot on the next hit.
 */
public final class RendAbility {
	public static final String NAME = "Rend";
	/** Base empower damage in HP: three hearts before stacks enter. */
	public static final float BASE_HP = 6.0F;
	/** Empower damage per Momentum stack in HP: half a heart each, 16 HP at full stacks. */
	public static final float HP_PER_STACK = 1.0F;
	/** Empower window in ticks: 6 seconds to land the hit, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 120;
	/** Cast sound id, resolved by the adapter; the anvil land is the empower. */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("block.anvil.land");

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN,
		NO_MOMENTUM
	}

	/**
	 * What firing should do. Empower damage is 0 when nothing is granted; consume is true only
	 * on fire, telling the adapter to drop every Momentum stack; announcement is null only for
	 * wrong-class presses, which stay silent.
	 */
	public record Result(Kind kind, long remainingMs, float empowerHp, boolean consume, ChatLine announcement) {
		public static Result fired(float empowerHp) {
			return new Result(Kind.FIRED, 0, empowerHp, true,
					new ChatLine(NAME + "!", ChatFormatting.RED));
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0, 0, false, null);
		}

		public static Result onCooldown(long remainingMs) {
			// Unstyled on purpose: this line has never carried a colour of its own.
			return new Result(Kind.ON_COOLDOWN, remainingMs, 0, false, ChatLine.cooldown(NAME, remainingMs));
		}

		public static Result noMomentum() {
			return new Result(Kind.NO_MOMENTUM, 0, 0, false,
					new ChatLine("No Momentum", null));
		}
	}

	private RendAbility() {
	}

	/** Empower damage for a stack count: base plus stacks. Nothing at zero or below. */
	public static float empowerFor(int stacks) {
		if (stacks <= 0) {
			return 0;
		}
		return BASE_HP + stacks * HP_PER_STACK;
	}

	/**
	 * The passive Rend spends: the class's hit-dealt stacker, or null. Found by trigger
	 * through {@link ClassDefinition#passiveForTrigger}, never by position, so reordering
	 * passives cannot silently spend the wrong one. Kept as the decision's semantic
	 * alias; adapters resolve through the definition directly.
	 */
	public static ConditionalPassive momentumOf(ClassDefinition def) {
		return def == null ? null : def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
	}

	/** Decides firing for Juggernauts off cooldown holding stacks. Zero stacks fizzles before the cooldown is even read. Pure and read-only: only {@link #fire} arms the cooldown. */
	public static Result trigger(ClassDefinition def, Cooldowns cooldowns, int stacks, long now) {
		if (def == null || !def.activeAbilities().contains(ClassRegistry.REND_ABILITY)) {
			return Result.wrongClass();
		}
		if (stacks <= 0) {
			return Result.noMomentum();
		}
		long remaining = cooldowns.rendRemaining(now);
		if (remaining > 0) {
			return Result.onCooldown(remaining);
		}
		return Result.fired(empowerFor(stacks));
	}

	/**
	 * Gate and arm together: the production path. Resolves the spent stacks from the runtime
	 * itself, decides with {@link #trigger}, and arms the transient runtime's Rend lane plus
	 * the empower snapshot when it fires. Rejected presses leave the lane untouched. An arm
	 * that somehow fails reports on-cooldown instead of a fire it did not earn.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		ConditionalPassive spent = momentumOf(def);
		int stacks = spent == null ? 0 : runtime.stacks(spent);
		Result result = trigger(def, runtime.cooldowns(), stacks, now);
		if (result.kind() == Kind.FIRED && !runtime.tryUseRend(now, stacks)) {
			return Result.onCooldown(runtime.cooldowns().rendRemaining(now));
		}
		return result;
	}
}
