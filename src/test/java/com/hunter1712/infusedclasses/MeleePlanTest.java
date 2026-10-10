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

	private static PlayerRuntime inStance(RotateAbility.Stance stance) {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.flipStance(0, stance);
		return runtime;
	}

	@Test
	void fullSwingsBumpSpamDoesNot() {
		assertTrue(MeleePlan.shouldBump(1.0F));
		assertFalse(MeleePlan.shouldBump(0.5F));
		assertFalse(MeleePlan.shouldBump(0.0F));
	}

	@Test
	void dealMultIsStanceForLegionariesOneForEveryoneElse() {
		PlayerRuntime scutum = inStance(RotateAbility.Stance.SCUTUM);
		PlayerRuntime gladius = inStance(RotateAbility.Stance.GLADIUS);

		assertEquals(0.9, MeleePlan.dealFor(legionary(), scutum, 0));
		assertEquals(1.0, MeleePlan.dealFor(legionary(), gladius, 10_000));
		assertEquals(1.0, MeleePlan.dealFor(juggernaut(), scutum, 0));
		assertEquals(1.0, MeleePlan.dealFor(null, scutum, 0));
	}

	@Test
	void impetusFlipMultipliesDealAtAnyChargeAndSpendsOnce() {
		PlayerRuntime gladius = inStance(RotateAbility.Stance.GLADIUS);

		assertEquals(1.10, MeleePlan.dealFor(legionary(), gladius, 0), 1e-9);

		MeleePlan.HitPlan plan = MeleePlan.planHit(legionary(), gladius, null, null, 0.5F, 0);

		// Any charge: spam still sees the flip buff (but earns no stacks).
		assertTrue(plan.impetusPending());
		assertFalse(plan.bump());
		assertEquals(0.0F, PassiveApplier.settleHit(gladius, legionary(), plan, 0));
		assertFalse(gladius.hasImpetusPending(0));
		assertEquals(1.0, MeleePlan.dealFor(legionary(), gladius, 0), 1e-9);
	}

	@Test
	void impetusBuffExpiresUnspent() {
		PlayerRuntime gladius = inStance(RotateAbility.Stance.GLADIUS);

		assertEquals(1.10,
				MeleePlan.dealFor(legionary(), gladius, ImpetusAbility.SWITCH_DEAL_MS - 1), 1e-9);
		assertEquals(1.0,
				MeleePlan.dealFor(legionary(), gladius, ImpetusAbility.SWITCH_DEAL_MS), 1e-9);
	}

	@Test
	void testudoCatchDiscountsTakeThenExpires() {
		PlayerRuntime scutum = inStance(RotateAbility.Stance.SCUTUM);

		assertEquals(0.85, MeleePlan.takeFor(legionary(), () -> scutum, 0), 1e-9);
		assertEquals(0.85, MeleePlan.takeFor(legionary(), () -> scutum, TestudoAbility.SWITCH_TAKE_MS - 1), 1e-9);
		assertEquals(1.0, MeleePlan.takeFor(legionary(), () -> scutum, TestudoAbility.SWITCH_TAKE_MS), 1e-9);
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
		assertEquals(0.9, MeleePlan.dealFor(legionary(), scutum, 0), 1e-9);
		assertEquals(1.0, MeleePlan.takeFor(legionary(), () -> scutum, 0), 1e-9);
		assertEquals(1.0, MeleePlan.dealFor(juggernaut(), scutum, 0));
	}

	@Test
	void planHitNamesTheCombinedMults() {
		PlayerRuntime attacker = inStance(RotateAbility.Stance.GLADIUS);
		PlayerRuntime victim = inStance(RotateAbility.Stance.SCUTUM);

		MeleePlan.HitPlan peak = MeleePlan.planHit(legionary(), attacker, legionary(), () -> victim, 1.0F, 0);

		assertEquals(1.10, peak.deal(), 1e-9);
		assertEquals(0.85, peak.take(), 1e-9);
		assertEquals(1.10 * 0.85, peak.combined(), 1e-9);
	}

	@Test
	void planHitGatesStacksAndPeeksOneShots() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, 0);

		MeleePlan.HitPlan full = MeleePlan.planHit(juggernaut(), runtime, null, null, 1.0F, 0);
		MeleePlan.HitPlan spam = MeleePlan.planHit(juggernaut(), runtime, null, null, 0.5F, 0);

		assertTrue(full.bump());
		assertFalse(spam.bump());
		assertTrue(full.rushBonus());
		assertFalse(spam.rushBonus());
		assertEquals(-1, full.rendStacks());
		assertEquals(0, full.rendHp());
	}

	@Test
	void settleConsumesWhatThePlanPeeked() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive momentum = juggernaut().passives().get(0);
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, 0, 1.0F);
		}
		RushAbility.fire(juggernaut(), runtime, 0);
		RendAbility.fire(juggernaut(), runtime, 0);
		MeleePlan.HitPlan plan = MeleePlan.planHit(juggernaut(), runtime, null, null, 1.0F, 0);

		float landed = PassiveApplier.settleHit(runtime, juggernaut(), plan, 0);

		// The settle returns the stamped bonus to land: the entry lands what it spent.
		assertEquals(plan.rendHp(), landed);
		assertTrue(landed > 0);
		// Settle owns the bump too: full swing earns one (+1), Rush credits +3, Rend snapshot spent, not refunded.
		assertEquals(9, runtime.stacks(momentum));
		assertFalse(runtime.hasRushBonusPending(0));
		assertEquals(-1, runtime.peekRendEmpowerStacks(0));
	}

	@Test
	void settleTouchesNothingOnAnEmptyHit() {
		PlayerRuntime runtime = new PlayerRuntime();
		MeleePlan.HitPlan plan = MeleePlan.planHit(juggernaut(), runtime, null, null, 0.5F, 0);

		assertEquals(0, PassiveApplier.settleHit(runtime, juggernaut(), plan, 0));

		assertEquals(0, runtime.stacks(juggernaut().passives().get(0)));
		assertFalse(runtime.hasRushBonusPending(0));
		assertEquals(-1, runtime.peekRendEmpowerStacks(0));
	}

	@Test
	void settleLeavesUnseenImpetusPending() {
		PlayerRuntime runtime = new PlayerRuntime();
		MeleePlan.HitPlan plan = MeleePlan.planHit(legionary(), runtime, null, null, 1.0F, 0);
		// The flip lands after the plan peeked: the plan saw nothing, so the settle spends nothing.
		runtime.flipStance(0, RotateAbility.Stance.GLADIUS);

		assertFalse(plan.impetusPending());
		PassiveApplier.settleHit(runtime, legionary(), plan, 0);

		assertTrue(runtime.hasImpetusPending(0));
	}

	@Test
	void settleCannotBumpWhatThePlanDidNotEarn() {
		PlayerRuntime runtime = new PlayerRuntime();
		// A lying plan (bump claimed at spam charge): the settle replays the gated
		// charge through the shared gate, so nothing bumps.
		MeleePlan.HitPlan lying = new MeleePlan.HitPlan(true, 0.5F, false, -1, 0, false, 1.0, 1.0, 1.0);

		assertEquals(0, PassiveApplier.settleHit(runtime, juggernaut(), lying, 0));
		assertEquals(0, runtime.stacks(juggernaut().passives().get(0)));
	}
}
