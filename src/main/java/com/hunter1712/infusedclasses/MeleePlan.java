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

	/** Outgoing melee multiplier for an attacker: the stance deal mult times the pending Impetus flip buff, 1.0 for anyone else. */
	public static double dealFor(ClassDefinition atkDef, PlayerRuntime runtime, long now) {
		return dealWithImpetus(atkDef, runtime, impetusPendingFor(atkDef, runtime, now), now);
	}

	/** Outgoing mult sharing one Impetus peek: the plan peeks once, the spend decision and the mult read the same answer. */
	private static double dealWithImpetus(ClassDefinition atkDef, PlayerRuntime runtime, boolean impetus, long now) {
		if (atkDef == null || runtime == null || !atkDef.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return 1.0;
		}
		double base = RotateAbility.dealMult(runtime.stance());
		double flip = impetus ? RotateAbility.SWITCH_DEAL_MULT : 1.0;
		return base * flip;
	}

	/** Incoming melee multiplier for a victim: the stance take mult times the live Testudo flip catch, 1.0 for anyone else. */
	public static double takeFor(ClassDefinition victimDef, Supplier<PlayerRuntime> runtime, long now) {
		if (victimDef == null || !victimDef.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return 1.0;
		}
		PlayerRuntime r = runtime.get();
		double base = RotateAbility.takeMult(r.stance());
		double flip = r.isTestudoBuffLive(now) ? RotateAbility.SWITCH_TAKE_MULT : 1.0;
		return base * flip;
	}

	/** Whether the attacker's Impetus flip buff is owed on this hit: any charge, spent once by the settle. False for anyone else. */
	public static boolean impetusPendingFor(ClassDefinition atkDef, PlayerRuntime runtime, long now) {
		if (atkDef == null || !atkDef.hasAbility(ClassRegistry.ROTATE_ABILITY)
				|| runtime == null || !runtime.hasImpetusPending(now)) {
			return false;
		}
		return true;
	}

	/**
	 * What one melee hit earns and settles, decided in field order: stack bump, Rush credit,
	 * Rend spend, Impetus spend, then the stance mults. The hit settle applies the first
	 * four in the same order and returns the stamped Rend bonus to land; the combined mult
	 * settles after. One-shot flags are peeked here and consumed there, at the same {@code now}.
	 */
	public record HitPlan(boolean bump, float charge, boolean rushBonus, int rendStacks, float rendHp, boolean impetusPending,
			double deal, double take, double combined) {
	}

	/** Full hit decision from definitions and transient state. Victim parts are 1.0 without a victim. */
	public static HitPlan planHit(ClassDefinition atkDef, PlayerRuntime atkRuntime, ClassDefinition victimDef,
			Supplier<PlayerRuntime> victimRuntime, float charge, long now) {
		boolean bump = shouldBump(charge);
		boolean rushBonus = bump && atkRuntime != null && atkRuntime.hasRushBonusPending(now);
		int rendStacks = atkRuntime == null ? -1 : atkRuntime.peekRendEmpowerStacks(now);
		float rendHp = rendStacks > 0 ? BonusCurve.bonusFor(RendAbility.empowerFor(rendStacks), charge) : 0;
		boolean impetus = impetusPendingFor(atkDef, atkRuntime, now);
		double deal = dealWithImpetus(atkDef, atkRuntime, impetus, now);
		double take = victimDef == null || victimRuntime == null ? 1.0 : takeFor(victimDef, victimRuntime, now);
		return new HitPlan(bump, charge, rushBonus, rendStacks, rendHp, impetus, deal, take, deal * take);
	}
}
