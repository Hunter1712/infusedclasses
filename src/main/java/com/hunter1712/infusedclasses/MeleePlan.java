package com.hunter1712.infusedclasses;

import java.util.function.Supplier;

/**
 * Melee plan decisions: what one melee hit earns and settles, decided purely from
 * definitions and transient state. The server entry maps, adapters apply; this owns
 * every ability-id check the hook needs, so the entry never compares ability ids to
 * gate rewards. The victim runtime arrives lazily: a non-Legionary victim costs no
 * transient state, as before.
 */
public final class MeleePlan {
	private MeleePlan() {
	}

	/** Whether this swing earns conditional-passive stacks: full swings only, spam earns zero. */
	public static boolean shouldBump(float charge) {
		return BonusCurve.isFullSwing(charge);
	}

	/** Outgoing melee multiplier for an attacker: the stance deal mult, 1.0 for anyone else. */
	public static double dealFor(ClassDefinition atkDef, PlayerRuntime runtime, long now) {
		if (atkDef == null || !atkDef.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return 1.0;
		}
		double base = RotateAbility.dealMult(runtime.stance());
		double window = runtime.isTestudoLive(now) ? TestudoAbility.TESTUDO_DEAL_MULT : 1.0;
		return base * window;
	}

	/** Incoming melee multiplier for a victim: the stance take mult, 1.0 for anyone else. */
	public static double takeFor(ClassDefinition victimDef, Supplier<PlayerRuntime> runtime, long now) {
		if (victimDef == null || !victimDef.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return 1.0;
		}
		PlayerRuntime r = runtime.get();
		double base = RotateAbility.takeMult(r.stance());
		double window = r.isImpetusLive(now) ? ImpetusAbility.IMPETUS_TAKEN_MULT : 1.0;
		return base * window;
	}

	/** Window bonus for an attacker inside a live Impetus window, through the charge curve. 0 otherwise. */
	public static float impetusBonusFor(ClassDefinition atkDef, PlayerRuntime runtime, long now, float charge) {
		if (atkDef == null || !atkDef.hasAbility(ClassRegistry.IMPETUS_ABILITY)
				|| !runtime.isImpetusLive(now)) {
			return 0;
		}
		return ImpetusAbility.bonusFor(charge);
	}

	/**
	 * What one melee hit earns and settles, decided in field order: stack bump, Rush credit,
	 * Rend spend, Impetus bonus, then the stance mults. The hit settle applies the first
	 * three in the same order and returns the stamped Rend bonus to land; bonuses land
	 * and the combined mult settles after. One-shot flags are peeked here and consumed
	 * there, at the same {@code now}.
	 */
	public record HitPlan(boolean bump, float charge, boolean rushBonus, int rendStacks, float rendHp, float impetusBonus,
			double deal, double take, double combined) {
	}

	/** Full hit decision from definitions and transient state. Victim parts are 1.0 without a victim. */
	public static HitPlan planHit(ClassDefinition atkDef, PlayerRuntime atkRuntime, ClassDefinition victimDef,
			Supplier<PlayerRuntime> victimRuntime, float charge, long now) {
		boolean bump = shouldBump(charge);
		boolean rushBonus = bump && atkRuntime != null && atkRuntime.hasRushBonusPending(now);
		int rendStacks = atkRuntime == null ? -1 : atkRuntime.peekRendEmpowerStacks(now);
		float rendHp = rendStacks > 0 ? RendAbility.bonusFor(rendStacks, charge) : 0;
		float impetus = atkRuntime == null ? 0 : impetusBonusFor(atkDef, atkRuntime, now, charge);
		double deal = atkDef == null || atkRuntime == null ? 1.0 : dealFor(atkDef, atkRuntime, now);
		double take = victimDef == null || victimRuntime == null ? 1.0 : takeFor(victimDef, victimRuntime, now);
		return new HitPlan(bump, charge, rushBonus, rendStacks, rendHp, impetus, deal, take, deal * take);
	}
}
