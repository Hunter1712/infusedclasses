package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Melee plan decisions: what one melee hit earns and settles, without players.
 * The entry maps, adapters apply; this owns every ability-id check the hook needs.
 */
class MeleePlanTest {
	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	private static ClassDefinition gambler() {
		return ClassRegistry.byId(ClassRegistry.GAMBLER);
	}

	private static PlayerRuntime inStance(RotateAbility.Stance stance) {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.rotate(0, stance);
		return runtime;
	}

	/** The plan's deal mult at full charge with a normal roll: the read production actually uses. */
	private static double dealOf(ClassDefinition def, PlayerRuntime runtime, long now) {
		return MeleePlan.planHit(def, runtime, null, null, 1.0F, now, 0.5F).deal();
	}

	@Test
	void dealMultIsStanceForLegionariesOneForEveryoneElse() {
		PlayerRuntime scutum = inStance(RotateAbility.Stance.SCUTUM);
		PlayerRuntime gladius = inStance(RotateAbility.Stance.GLADIUS);

		assertEquals(0.9, dealOf(legionary(), scutum, 0));
		assertEquals(1.0, dealOf(legionary(), gladius, 10_000));
		assertEquals(1.0, dealOf(juggernaut(), scutum, 0));
		assertEquals(1.0, dealOf(null, scutum, 0));
	}

	@Test
	void impetusFlipMultipliesDealAtAnyChargeAndSpendsOnce() {
		PlayerRuntime gladius = inStance(RotateAbility.Stance.GLADIUS);

		assertEquals(1.10, dealOf(legionary(), gladius, 0), 1e-9);

		MeleePlan.HitPlan plan = MeleePlan.planHit(legionary(), gladius, null, null, 0.5F, 0, 0.5F);

		// Any charge: spam still sees the flip buff (but earns no stacks, no roll).
		assertTrue(plan.impetusPending());
		assertFalse(plan.bump());
		MeleePlan.Landed landed = MeleePlan.resolve(legionary(), gladius, null, null, 0.5F, 0, 0.5F);
		assertEquals(0.0F, landed.rendHp());
		assertFalse(gladius.state(0).impetusPending());
		assertEquals(1.0, dealOf(legionary(), gladius, 0), 1e-9);
	}

	@Test
	void impetusBuffExpiresUnspent() {
		PlayerRuntime gladius = inStance(RotateAbility.Stance.GLADIUS);

		assertEquals(1.10,
				dealOf(legionary(), gladius, RotateAbility.SWITCH_DEAL_MS - 1), 1e-9);
		assertEquals(1.0,
				dealOf(legionary(), gladius, RotateAbility.SWITCH_DEAL_MS), 1e-9);
	}

	@Test
	void testudoCatchDiscountsTakeThenExpires() {
		PlayerRuntime scutum = inStance(RotateAbility.Stance.SCUTUM);

		assertEquals(0.85, MeleePlan.takeFor(legionary(), () -> scutum, 0), 1e-9);
		assertEquals(0.85, MeleePlan.takeFor(legionary(), () -> scutum, RotateAbility.SWITCH_TAKE_MS - 1), 1e-9);
		assertEquals(1.0, MeleePlan.takeFor(legionary(), () -> scutum, RotateAbility.SWITCH_TAKE_MS), 1e-9);
	}

	@Test
	void takeMultIsStanceForLegionariesOneForEveryoneElse() {
		PlayerRuntime gladius = inStance(RotateAbility.Stance.GLADIUS);

		assertEquals(1.1, MeleePlan.takeFor(legionary(), () -> gladius, 10_000));
		assertEquals(1.0, MeleePlan.takeFor(juggernaut(), () -> gladius, 0));
		assertEquals(1.0, MeleePlan.takeFor(null, () -> gladius, 0));
	}

	@Test
	void aequilibriumWindowKeepsStanceMults() {
		PlayerRuntime scutum = new PlayerRuntime();
		AequilibriumAbility.fire(legionary(), scutum, 0);

		// The ultimate adds attributes, never mults: the stance tradeoff stays underneath.
		assertEquals(0.9, dealOf(legionary(), scutum, 0), 1e-9);
		assertEquals(1.0, MeleePlan.takeFor(legionary(), () -> scutum, 0), 1e-9);
		assertEquals(1.0, dealOf(juggernaut(), scutum, 0));
	}

	@Test
	void planHitNamesTheCombinedMults() {
		PlayerRuntime attacker = inStance(RotateAbility.Stance.GLADIUS);
		PlayerRuntime victim = inStance(RotateAbility.Stance.SCUTUM);

		MeleePlan.HitPlan peak = MeleePlan.planHit(legionary(), attacker, legionary(), () -> victim, 1.0F, 0, 0.5F);

		assertEquals(1.10, peak.deal(), 1e-9);
		assertEquals(0.85, peak.take(), 1e-9);
		assertEquals(CritRoll.Verdict.NORMAL, peak.dice().verdict());
		assertEquals(1.10 * 0.85, peak.combined(), 1e-9);
	}

	@Test
	void planHitGatesStacksAndPeeksOneShots() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, 0);

		MeleePlan.HitPlan full = MeleePlan.planHit(juggernaut(), runtime, null, null, 1.0F, 0, 0.5F);
		MeleePlan.HitPlan spam = MeleePlan.planHit(juggernaut(), runtime, null, null, 0.5F, 0, 0.5F);

		assertTrue(full.bump());
		assertFalse(spam.bump());
		assertTrue(full.rushBonus());
		assertFalse(spam.rushBonus());
		assertEquals(0, full.rendStacks());
		assertEquals(0, full.rendHp());
	}

	@Test
	void planNamesTheDiceVerdictAndFoldsItIntoTheCombinedMult() {
		PlayerRuntime runtime = new PlayerRuntime();

		MeleePlan.HitPlan crit = MeleePlan.planHit(juggernaut(), runtime, null, null, 1.0F, 0, 0.05F);
		MeleePlan.HitPlan normal = MeleePlan.planHit(juggernaut(), runtime, null, null, 1.0F, 0, 0.5F);

		// The universal rule: a crit at x1.25, folded into the swing's combined mult.
		assertEquals(CritRoll.Verdict.CRIT, crit.dice().verdict());
		assertEquals(1.25, crit.dice().mult(), 1e-9);
		assertEquals(1.25, crit.combined(), 1e-9);
		assertEquals(CritRoll.Verdict.NORMAL, normal.dice().verdict());
		assertEquals(1.0, normal.combined(), 1e-9);
	}

	@Test
	void diceFoldsMultiplicativelyWithStance() {
		PlayerRuntime scutum = inStance(RotateAbility.Stance.SCUTUM);

		MeleePlan.HitPlan plan = MeleePlan.planHit(legionary(), scutum, null, null, 1.0F, 0, 0.05F);

		// Scutum x0.9 combined with the x1.25 crit: the mults multiply, none replaces another.
		assertEquals(0.9, plan.deal(), 1e-9);
		assertEquals(0.9 * 1.25, plan.combined(), 1e-9);
	}

	@Test
	void wheelOutcomeMultRidesThePlanWhileLive() {
		PlayerRuntime runtime = new PlayerRuntime();
		RouletteAbility.fire(gambler(), runtime, 0, 0.0F); // Edge: dealt x1.25

		MeleePlan.HitPlan plan = MeleePlan.planHit(gambler(), runtime, null, null, 1.0F, 0, 0.5F);

		assertEquals(RouletteAbility.EDGE_MULT, plan.deal(), 1e-9);
		assertEquals(RouletteAbility.EDGE_MULT, plan.combined(), 1e-9);
	}

	@Test
	void halfSwingNeverRolls() {
		PlayerRuntime runtime = new PlayerRuntime();

		MeleePlan.HitPlan spam = MeleePlan.planHit(gambler(), runtime, null, null, 0.5F, 0, 0.0F);

		assertEquals(CritRoll.Verdict.NONE, spam.dice().verdict());
		assertEquals(1.0, spam.dice().mult(), 1e-9);
		assertEquals(CritRoll.Movement.NONE, spam.dice().movement());
	}

	@Test
	void classlessFullSwingSettlesTheUniversalCritOnly() {
		PlayerRuntime runtime = new PlayerRuntime();

		MeleePlan.HitPlan crit = MeleePlan.planHit(null, runtime, null, null, 1.0F, 0, 0.05F);

		assertEquals(CritRoll.Verdict.CRIT, crit.dice().verdict());
		assertEquals(1.25, crit.combined(), 1e-9);
		assertEquals(CritRoll.Movement.NONE, crit.dice().movement());
		// Settling a classless hit touches no stacks, no lanes, no Streak.
		MeleePlan.Landed landed = MeleePlan.resolve(null, runtime, null, null, 1.0F, 0, 0.05F);
		assertEquals(0.0F, landed.rendHp());
		assertEquals(CritRoll.Verdict.CRIT, landed.verdict());
		assertEquals(0, runtime.state(0).streak());
	}

	@Test
	void aNullAttackerRuntimeSettlesLikeAClasslessOne() {
		// The C5 shape: classless hits never materialise a runtime, so the plan and the
		// settle both read null as "no abilities to move" and still roll the universal dice.
		MeleePlan.Landed landed = MeleePlan.resolve(null, null, null, null, 1.0F, 0, 0.05F);

		assertEquals(0.0F, landed.rendHp());
		assertEquals(CritRoll.Verdict.CRIT, landed.verdict());
		assertEquals(1.25, landed.combined(), 1e-9);
	}

	@Test
	void settleAppliesTheDiceMovement() {
		PlayerRuntime runtime = new PlayerRuntime();

		MeleePlan.resolve(gambler(), runtime, null, null, 1.0F, 0, 0.5F);
		MeleePlan.resolve(gambler(), runtime, null, null, 1.0F, 0, 0.5F);
		assertEquals(2, runtime.state(0).streak());

		MeleePlan.resolve(gambler(), runtime, null, null, 1.0F, 0, 0.1F);
		assertEquals(0, runtime.state(0).streak());

		// Betrayal counts as a non-crit: the bad roll still loads the pocket.
		MeleePlan.Landed betrayal = MeleePlan.resolve(gambler(), runtime, null, null, 1.0F, 0, 0.01F);
		assertEquals(CritRoll.Verdict.BETRAYAL, betrayal.verdict());
		assertEquals(0.5, betrayal.combined(), 1e-9);
		assertEquals(1, runtime.state(0).streak());
	}

	@Test
	void rendBonusIsUntouchedByTheDice() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive momentum = juggernaut().passives().get(0);
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 1.0F);
		}
		RendAbility.fire(juggernaut(), runtime, 0);

		MeleePlan.HitPlan plan = MeleePlan.planHit(juggernaut(), runtime, null, null, 1.0F, 0, 0.05F);

		// The Rend bonus is its own: the crit folds into the settled mult, never into rendHp.
		assertEquals(CritRoll.Verdict.CRIT, plan.dice().verdict());
		assertEquals(1.25, plan.dice().mult(), 1e-9);
		assertEquals(RendAbility.bonusFor(5), plan.rendHp(), 1e-9);
		MeleePlan.Landed landed = MeleePlan.resolve(juggernaut(), runtime, null, null, 1.0F, 0, 0.05F);
		assertEquals(plan.rendHp(), landed.rendHp(), 1e-9);
		assertEquals(1.25, landed.combined(), 1e-9);
		// The resolve bumps the stack it earned; the entry's consume is what drops them for the fire.
		assertEquals(6, runtime.stacks(momentum));
	}

	@Test
	void settleConsumesWhatThePlanPeeked() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive momentum = juggernaut().passives().get(0);
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 1.0F);
		}
		RushAbility.fire(juggernaut(), runtime, 0);
		RendAbility.fire(juggernaut(), runtime, 0);
		MeleePlan.HitPlan plan = MeleePlan.planHit(juggernaut(), runtime, null, null, 1.0F, 0, 0.5F);

		MeleePlan.Landed landed = MeleePlan.resolve(juggernaut(), runtime, null, null, 1.0F, 0, 0.5F);

		// The resolve returns the stamped bonus to land: the entry lands what it settled.
		assertEquals(plan.rendHp(), landed.rendHp(), 1e-9);
		assertTrue(landed.rendHp() > 0);
		// Settle owns the bump too: full swing earns one (+1), Rush credits +5, Rend snapshot spent, not refunded.
		assertEquals(11, runtime.stacks(momentum));
		assertFalse(runtime.state(0).lane(Lane.RUSH).windowPending());
		assertEquals(0, runtime.state(0).rendBonusStacks());
	}

	@Test
	void settleTouchesNothingOnAnEmptyHit() {
		PlayerRuntime runtime = new PlayerRuntime();

		MeleePlan.Landed landed = MeleePlan.resolve(juggernaut(), runtime, null, null, 0.5F, 0, 0.5F);

		assertEquals(0.0F, landed.rendHp());

		assertEquals(0, runtime.stacks(juggernaut().passives().get(0)));
		assertFalse(runtime.state(0).lane(Lane.RUSH).windowPending());
		assertEquals(0, runtime.state(0).rendBonusStacks());
	}

	@Test
	void flipLandedAfterTheHitStaysPending() {
		PlayerRuntime runtime = new PlayerRuntime();
		MeleePlan.HitPlan plan = MeleePlan.planHit(legionary(), runtime, null, null, 1.0F, 0, 0.5F);
		// The hit resolves before the flip lands: it saw nothing, so it spends nothing.
		MeleePlan.resolve(legionary(), runtime, null, null, 1.0F, 0, 0.5F);
		runtime.rotate(0, RotateAbility.Stance.GLADIUS);

		assertFalse(plan.impetusPending());
		assertTrue(runtime.state(0).impetusPending());
	}

	@Test
	void resolveBumpsOnlyWhatFullSwingsEarned() {
		PlayerRuntime runtime = new PlayerRuntime();

		MeleePlan.resolve(juggernaut(), runtime, null, null, 0.5F, 0, 0.5F);
		assertEquals(0, runtime.stacks(juggernaut().passives().get(0)));

		MeleePlan.resolve(juggernaut(), runtime, null, null, 1.0F, 0, 0.5F);
		assertEquals(1, runtime.stacks(juggernaut().passives().get(0)));
	}
}
