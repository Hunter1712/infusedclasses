package com.hunter1712.infusedclasses;

import java.util.function.Supplier;

/**
 * Melee plan decisions: what one melee hit earns and settles, decided from definitions,
 * transient state and what the hit landed. The decision half peeks; {@link #resolve} then
 * settles the runtime movements and hands the entry exactly what to land. The server
 * entry maps, adapters apply; this owns every ability-id check the hook needs, so the
 * entry never compares ability ids to gate rewards. The victim runtime arrives lazily: a
 * victim without a runtime to move costs no transient state.
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

	/** The attacker's always-on chassis deal mult: 1.0 without a class or a row. */
	private static double chassisDeal(ClassDefinition atkDef) {
		return atkDef == null ? 1.0 : atkDef.chassisDealMult();
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

	/** The attacker's Momentum bar, 0 without an attacker runtime or a bar to read. */
	private static int momentumStacks(ClassDefinition atkDef, PlayerRuntime atkRuntime) {
		ConditionalPassive momentum = atkDef == null ? null : atkDef.passiveForTrigger(Trigger.ON_HIT_DEALT);
		return momentum == null || atkRuntime == null ? 0 : atkRuntime.stacks(momentum);
	}

	/**
	 * What one melee hit earns and settles, decided in field order: the stack bump (bars
	 * feed only outside an Onslaught window), the Onslaught price (full swings only, 2
	 * stacks), the Impetus spend, then the chassis, stance, wheel, oath and window mults.
	 * The Onslaught mult pays on any landed hit while its window is live; its price waits
	 * for a full swing. Resolving applies the movements in the same order and returns the
	 * stamped verdict and combined mult to land. One-shot flags are peeked here and
	 * consumed there, at the same {@code now}. A hit that took no damage (a raised shield
	 * ate it) earns nothing: it reads no state, materialises no victim runtime and draws
	 * no verdict, so nothing of it can settle.
	 */
	public record HitPlan(boolean bump, float charge, boolean priceOnslaught, boolean impetusPending,
			double deal, double take, double combined, CritRoll.Result dice) {
		/** The plan of a hit that earned nothing: no peek, no verdict, neutral mults against the zero landed. */
		static HitPlan none(float charge) {
			return new HitPlan(false, charge, false, false, 1.0, 1.0, 1.0, CritRoll.Result.NONE);
		}
	}

	/** What one settled hit lands: the verdict to cue and the combined mult to settle. */
	public record Landed(CritRoll.Verdict verdict, double combined) {
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
		PlayerRuntime.State state = atkRuntime == null ? null : atkRuntime.state(now);
		int stacks = momentumStacks(atkDef, atkRuntime);
		boolean onslaughtLive = OnslaughtAbility.windowLive(state);
		// No bar feeds while the ultimate is live: the hit pays instead of earning.
		boolean bump = FullSwing.isFull(charge) && !onslaughtLive;
		// The mult pays any charge; the price waits for a full swing.
		boolean priceOnslaught = FullSwing.isFull(charge) && OnslaughtAbility.liveFor(state, stacks);
		boolean impetus = atkDef != null && atkDef.hasAbility(ClassRegistry.ROTATE_ABILITY)
				&& state != null && state.impetusPending();
		double deal = chassisDeal(atkDef) * dealWithImpetus(atkDef, state, impetus)
				* RouletteAbility.dealMult(state) * AequilibriumAbility.dealMult(state)
				* OnslaughtAbility.dealMult(state, stacks);
		double take = victimDef == null || victimRuntime == null ? 1.0 : takeFor(victimDef, victimRuntime, now);
		CritRule rule = atkDef == null ? CritRule.universal() : atkDef.critRule();
		boolean allIn = state != null && state.lane(Lane.ALL_IN).windowRemainingMs() > 0;
		CritRoll.Result dice = CritRoll.roll(rule, state == null ? 0 : state.streak(), charge, roll, allIn);
		return new HitPlan(bump, charge, priceOnslaught, impetus, deal, take,
				deal * take * dice.mult(), dice);
	}

	/**
	 * Decision and settle in one: plans the hit through {@link #planHit} and settles the
	 * runtime movements in the plan's field order — the stack bump, the dice movement,
	 * the Impetus spend, then the Onslaught price — all at the same {@code now} the plan
	 * peeked them. Returns what the entry lands: the crit verdict to cue and the combined
	 * mult to settle. The one seam between the plan and the entry, so nothing lands a hit
	 * that was not resolved. A hit that took no damage resolves the empty plan and moves
	 * nothing.
	 */
	public static Landed resolve(ClassDefinition atkDef, PlayerRuntime atkRuntime, ClassDefinition victimDef,
			Supplier<PlayerRuntime> victimRuntime, float charge, float landedDamage, long now, float roll) {
		HitPlan plan = planHit(atkDef, atkRuntime, victimDef, victimRuntime, charge, landedDamage, now, roll);
		settle(atkRuntime, atkDef, plan, now);
		return new Landed(plan.dice().verdict(), plan.combined());
	}

	/** Settles the plan's movements in field order; the entry lands nothing the plan did not stamp. */
	private static void settle(PlayerRuntime runtime, ClassDefinition def, HitPlan plan, long now) {
		if (runtime == null || plan == null) {
			return;
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
		if (plan.impetusPending()) {
			runtime.consumeBonus(Lane.ROTATE, now);
		}
		if (plan.priceOnslaught()) {
			spendOnslaught(runtime, def);
		}
	}

	/**
	 * The Onslaught price: {@link OnslaughtAbility#COST_PER_HIT} stacks per full-swing hit, and a
	 * bar reaching empty ends the window there — the ultimate lasts the bar and the window,
	 * whichever empties first.
	 */
	private static void spendOnslaught(PlayerRuntime runtime, ClassDefinition def) {
		ConditionalPassive momentum = def == null ? null : def.passiveForTrigger(Trigger.ON_HIT_DEALT);
		if (momentum == null) {
			return;
		}
		runtime.spendStacks(momentum, OnslaughtAbility.COST_PER_HIT);
		OnslaughtAbility.endIfDrained(runtime, momentum);
	}

	/**
	 * The victim's feed: a melee hit that dealt damage feeds {@code ON_HIT_TAKEN} +1, the
	 * same as one dealt. Suppressed while the victim's Onslaught window is live — no stack
	 * gain from any source while it lasts — and classless or unaided victims cost nothing:
	 * the class check runs before the runtime is materialised, so a victim with no
	 * ON_HIT_TAKEN bar never pays for one.
	 */
	public static void resolveTaken(ClassDefinition victimDef, Supplier<PlayerRuntime> victimRuntime,
			float landedDamage, long now) {
		if (landedDamage <= 0 || victimDef == null
				|| victimDef.passiveForTrigger(Trigger.ON_HIT_TAKEN) == null) {
			return;
		}
		PlayerRuntime runtime = victimRuntime == null ? null : victimRuntime.get();
		if (runtime == null || OnslaughtAbility.windowLive(runtime.state(now))) {
			return;
		}
		runtime.onTrigger(victimDef, Trigger.ON_HIT_TAKEN, now, 1.0F);
	}
}
