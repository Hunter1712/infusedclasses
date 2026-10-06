package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

/**
 * Second Wind decisions: the Warrior's Bulwark spender on {@code G}. Pure gate-and-arm shape:
 * shields {@code stacks * HP_PER_STACK} absorption HP for {@code ABSORPTION_TICKS} and
 * consumes every Bulwark stack, on its own 60s lane. Live HP is untouched, so Berserk
 * stays hot while the shield holds. Zero stacks
 * fizzles with a {@code No Bulwark} line and arms nothing. The consume plan rides in the result
 * ({@link Result#consume()}): the entry asks the passive applier to consume the trigger-found
 * passive.
 */
public final class SecondWindAbility {
	public static final String NAME = "Second Wind";
	/** Absorption HP per Bulwark stack: half a heart each, 20 HP at full stacks. */
	public static final float HP_PER_STACK = 1.0F;
	/** Shield window in ticks: 10 seconds, carried by the vanilla absorption effect. */
	public static final int ABSORPTION_TICKS = 200;
	/** Vanilla absorption effect id, resolved by the adapter; its 4 HP/level cap and expiry carry the window. */
	public static final Identifier ABSORPTION = Identifier.withDefaultNamespace("absorption");

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN,
		NO_BULWARK
	}

	/**
	 * What firing should do. Shield is 0 when nothing is granted; consume is true only on fire,
	 * telling the adapter to drop every Bulwark stack; announcement is null only for wrong-class
	 * presses, which stay silent.
	 */
	public record Result(Kind kind, long remainingMs, float shieldHp, boolean consume, ChatLine announcement) {
		public static Result fired(float shieldHp) {
			return new Result(Kind.FIRED, 0, shieldHp, true,
					new ChatLine(NAME + "!", ChatFormatting.GREEN));
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0, 0, false, null);
		}

		public static Result onCooldown(long remainingMs) {
			// Unstyled on purpose: this line has never carried a colour of its own.
			return new Result(Kind.ON_COOLDOWN, remainingMs, 0, false, ChatLine.cooldown(NAME, remainingMs));
		}

		public static Result noBulwark() {
			return new Result(Kind.NO_BULWARK, 0, 0, false,
					new ChatLine("No Bulwark", null));
		}
	}

	private SecondWindAbility() {
	}

	/** Shield HP for a stack count: half a heart per stack, nothing at zero or below. */
	public static float shieldAmount(int stacks) {
		return stacks <= 0 ? 0 : stacks * HP_PER_STACK;
	}

	/**
	 * Effect amplifier covering a shield: vanilla grants 4 HP per level, so the smallest level
	 * whose grant reaches the shield (1 HP → 0, 5 HP → 1, 20 HP → 4). The adapter trims the
	 * grant down to the exact shield right after, so the icon never promises less than it holds.
	 */
	public static int amplifierFor(float shieldHp) {
		return Math.max(0, (int) Math.ceil(shieldHp / 4.0F) - 1);
	}

	/**
	 * The passive Second Wind spends: the class's hit-taken stacker, or null. Found by trigger,
	 * never by position, so reordering passives cannot silently spend the wrong one.
	 */
	public static ConditionalPassive bulwarkOf(ClassDefinition def) {
		if (def == null) {
			return null;
		}
		for (ConditionalPassive passive : def.passives()) {
			if (ConditionalPassive.ON_HIT_TAKEN.equals(passive.trigger())) {
				return passive;
			}
		}
		return null;
	}

	/** Decides firing for Warriors off cooldown holding stacks. Zero stacks fizzles before the cooldown is even read. Pure and read-only: only {@link #fire} arms the cooldown. */
	public static Result trigger(ClassDefinition def, Cooldowns cooldowns, int stacks, long now) {
		if (def == null || !def.activeAbilities().contains(ClassRegistry.SECOND_WIND_ABILITY)) {
			return Result.wrongClass();
		}
		if (stacks <= 0) {
			return Result.noBulwark();
		}
		long remaining = cooldowns.secondWindRemaining(now);
		if (remaining > 0) {
			return Result.onCooldown(remaining);
		}
		return Result.fired(shieldAmount(stacks));
	}

	/**
	 * Gate and arm together: the production path. Resolves the spent stacks from the runtime
	 * itself, decides with {@link #trigger}, and arms the transient runtime's Second Wind lane
	 * when it fires. Rejected presses leave the lane untouched.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		ConditionalPassive spent = bulwarkOf(def);
		int stacks = spent == null ? 0 : runtime.stacks(spent);
		Result result = trigger(def, runtime.cooldowns(), stacks, now);
		if (result.kind() == Kind.FIRED) {
			runtime.tryUseSecondWind(now);
		}
		return result;
	}
}
