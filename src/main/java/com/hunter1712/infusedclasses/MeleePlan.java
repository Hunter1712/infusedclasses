package com.hunter1712.infusedclasses;

import java.util.function.Supplier;

/**
 * Melee plan decisions: what one melee hit earns and settles, decided from definitions,
 * transient state and what the hit landed. The decision half peeks; {@link #resolve} then
 * settles the runtime movements and hands the entry exactly what to land. The server
 * entry maps, adapters apply; this owns every ability-id check the hook needs, so the
 * entry never compares ability ids to gate rewards. The victim runtime arrives lazily: a
 * non-Legionary victim costs no transient state, as before.
 */
public final class MeleePlan {
	private MeleePlan() {
	}

	/** Outgoing mult sharing one Impetus peek: the plan peeks once, the spend decision and the mult read the same answer. */
	private static double dealWithImpetus(ClassDefinition atkDef, PlayerRuntime.State state, boolean impetus) {
		if (atkDef == null || state == null || !atkDef.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return 1.0;
		}
		double base = RotateAbility.dealMult(state.stance());
		double flip = impetus ? RotateAbility.SWITCH_DEAL_MULT : 1.0;
		return base * flip;
	}

	/** Incoming melee multiplier for a victim: the stance take mult times the live Testudo flip catch, 1.0 for anyone else. */
	public static double takeFor(ClassDefinition victimDef, Supplier<PlayerRuntime> runtime, long now) {
		if (victimDef == null || !victimDef.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return 1.0;
		}
		PlayerRuntime.State state = runtime.get().state(now);
		double base = RotateAbility.takeMult(state.stance());
		double flip = state.flipTarget() == RotateAbility.Stance.SCUTUM ? RotateAbility.SWITCH_TAKE_MULT : 1.0;
		return base * flip;
	}

	/**
	 * What one melee hit earns and settles, decided in field order: stack bump, Rush credit,
	 * Rend spend, Impetus spend, then the stance, wheel and dice mults. Resolving applies
	 * the first four and the dice movement in the same order and returns the stamped Rend bonus
	 * to land; the combined mult (deal x take x dice) settles after. One-shot flags are peeked
	 * here and consumed there, at the same {@code now}. A hit that took no damage (a raised
	 * shield ate it) earns nothing: it reads no state, materialises no victim runtime and draws
	 * no verdict, so nothing of it can settle.
	 */
	public record HitPlan(boolean bump, float charge, boolean rushBonus, int rendStacks, float rendHp, boolean impetusPending,
			double deal, double take, double combined, CritRoll.Result dice) {
		/** The plan of a hit that earned nothing: no peek, no verdict, neutral mults against the zero landed. */
		static HitPlan none(float charge) {
			return new HitPlan(false, charge, false, 0, 0, false, 1.0, 1.0, 1.0, CritRoll.Result.NONE);
		}
	}

	/** What one settled hit lands: the stamped Rend bonus, the verdict to cue, the combined mult to settle. */
	public record Landed(float rendHp, CritRoll.Verdict verdict, double combined) {
	}

	/**
	 * Full hit decision from definitions, transient state and what the hit landed, the
	 * caller's roll included. The pure half: peeks only, settles nothing. A landed amount
	 * of zero — a fully blocked hit — returns the empty plan before any read that costs
	 * state, so a shield neither earns nor spends. Victim parts are 1.0 without a victim;
	 * a missing class reads the universal crit rule. Full swings roll for every class,
	 * classless included.
	 */
	static HitPlan planHit(ClassDefinition atkDef, PlayerRuntime atkRuntime, ClassDefinition victimDef,
			Supplier<PlayerRuntime> victimRuntime, float charge, float landedDamage, long now, float roll) {
		if (landedDamage <= 0) {
			return HitPlan.none(charge);
		}
		boolean bump = BonusCurve.isFullSwing(charge);
		PlayerRuntime.State state = atkRuntime == null ? null : atkRuntime.state(now);
		boolean rushBonus = bump && state != null && state.lane(Lane.RUSH).windowPending();
		int rendStacks = state == null ? 0 : state.rendBonusStacks();
		float rendHp = rendStacks > 0 ? BonusCurve.bonusFor(RendAbility.bonusFor(rendStacks), charge) : 0;
		boolean impetus = atkDef != null && atkDef.hasAbility(ClassRegistry.ROTATE_ABILITY)
				&& state != null && state.impetusPending();
		double deal = dealWithImpetus(atkDef, state, impetus) * RouletteAbility.dealMult(state);
		double take = victimDef == null || victimRuntime == null ? 1.0 : takeFor(victimDef, victimRuntime, now);
		CritRule rule = atkDef == null ? CritRule.universal() : atkDef.critRule();
		boolean allIn = state != null && state.lane(Lane.ALL_IN).windowRemainingMs() > 0;
		CritRoll.Result dice = CritRoll.roll(rule, state == null ? 0 : state.streak(), charge, roll, allIn);
		return new HitPlan(bump, charge, rushBonus, rendStacks, rendHp, impetus, deal, take,
				deal * take * dice.mult(), dice);
	}

	/**
	 * Decision and settle in one: plans the hit through {@link #planHit} and settles the
	 * runtime movements in the plan's field order — the stack bump, the dice movement,
	 * the Rush credit, the Impetus spend, then the Rend snapshot spend — all at the same
	 * {@code now} the plan peeked them. Returns what the entry lands: the stamped Rend
	 * bonus, the crit verdict to cue, and the combined mult. The one seam between the
	 * plan and the entry, so nothing lands a hit that was not resolved. A hit that took
	 * no damage resolves the empty plan and moves nothing.
	 */
	public static Landed resolve(ClassDefinition atkDef, PlayerRuntime atkRuntime, ClassDefinition victimDef,
			Supplier<PlayerRuntime> victimRuntime, float charge, float landedDamage, long now, float roll) {
		HitPlan plan = planHit(atkDef, atkRuntime, victimDef, victimRuntime, charge, landedDamage, now, roll);
		return new Landed(settle(atkRuntime, atkDef, plan, now), plan.dice().verdict(), plan.combined());
	}

	/** Settles the plan's movements; returns the stamped Rend bonus to land, 0 when the plan saw nothing live. */
	private static float settle(PlayerRuntime runtime, ClassDefinition def, HitPlan plan, long now) {
		if (runtime == null || plan == null) {
			return 0;
		}
		if (plan.bump()) {
			// Replays the plan's gated charge through the shared gate, so the settle
			// cannot bump what the plan did not earn.
			runtime.onTrigger(def, Trigger.ON_HIT_DEALT, now, plan.charge());
		}
		switch (plan.dice().movement()) {
			case BUMP -> runtime.bumpStreak();
			case RESET -> runtime.resetStreak();
			case NONE -> {
			}
		}
		if (plan.rushBonus()) {
			grantRushBonus(runtime, def, now);
		}
		if (plan.impetusPending()) {
			runtime.consumeBonus(Lane.ROTATE, now);
		}
		if (plan.rendStacks() > 0 && runtime.consumePayload(Lane.REND, now) > 0) {
			return plan.rendHp();
		}
		return 0;
	}

	/**
	 * Rush's next qualifying melee hit credits the priced passive with the ability's
	 * bonus, capped and grace re-stamped in one call. Resolves through the definition:
	 * a missing priced passive or an unowed bonus does nothing.
	 */
	private static void grantRushBonus(PlayerRuntime runtime, ClassDefinition def, long now) {
		ConditionalPassive momentum = def == null ? null : def.passiveForTrigger(Trigger.ON_HIT_DEALT);
		if (momentum == null || !runtime.consumeBonus(Lane.RUSH, now)) {
			return;
		}
		runtime.bumpPassive(momentum, RushAbility.BONUS_STACKS, now);
	}
}
