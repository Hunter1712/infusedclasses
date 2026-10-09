package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

/**
 * Impetus decisions: the Legionary's Gladius payoff on {@code G}. Pure gate-and-arm
 * shape on the shared oath lane: only Gladius fires, firing arms the lane for both
 * oath abilities and stamps a 6s window. Every melee hit inside the window earns
 * the bonus through the shared {@link BonusCurve}, so spam still earns chips.
 */
public final class ImpetusAbility {
	public static final String NAME = "Impetus";
	/** Hook bonus damage in HP inside the window, before the charge curve. */
	public static final float IMPETUS_BONUS = 3.0F;
	/** Tradeoff: melee taken x1.25 while live (x1.5 with the Gladius stance). Single source for the melee plan and the picker copy, so the two cannot drift. */
	public static final double IMPETUS_TAKEN_MULT = 1.25;
	/** Payoff window in ticks: 6 seconds, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 120;
	/** Cast sound id, resolved by the adapter; fail silent if missing. */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("entity.player.attack.sweep");
	/** Picker-copy tail evoking the cast, kept next to {@link #CAST_SOUND} so the two change together. */
	public static final String CAST_WORD = "Ha!";

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN
	}

	public record Result(Kind kind, long remainingMs, ChatLine announcement) {
		public static Result fired() {
			return new Result(Kind.FIRED, 0, new ChatLine(NAME + "!", ChatFormatting.RED));
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0, null);
		}

		public static Result onCooldown(long remainingMs) {
			return new Result(Kind.ON_COOLDOWN, remainingMs, ChatLine.cooldown(NAME, remainingMs));
		}
	}

	private ImpetusAbility() {
	}

	/** Window bonus at a charge: full swings pay +3, half charge pays a quarter. */
	public static float bonusFor(float charge) {
		return BonusCurve.bonusFor(IMPETUS_BONUS, charge);
	}

	/** Decides firing for Gladius Legionaries off the shared oath lane. Other stances and classes stay silent. Pure and read-only: only {@link #fire} arms the lane and the window. */
	public static Result trigger(ClassDefinition def, long oathRemaining, RotateAbility.Stance stance) {
		if (def == null || !def.hasAbility(ClassRegistry.IMPETUS_ABILITY)
				|| stance != RotateAbility.Stance.GLADIUS) {
			return Result.wrongClass();
		}
		if (oathRemaining > 0) {
			return Result.onCooldown(oathRemaining);
		}
		return Result.fired();
	}

	/**
	 * Gate and arm together: the production path. A fired press arms the shared
	 * oath lane and stamps the window together, locking Testudo for 50s too;
	 * rejected presses leave both untouched.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		Result result = trigger(def, runtime.oathRemaining(now), runtime.stance());
		return AbilityGate.gate(result, result.kind() == Kind.FIRED, () -> runtime.tryUseImpetus(now),
				() -> runtime.oathRemaining(now), Result::onCooldown);
	}
}
