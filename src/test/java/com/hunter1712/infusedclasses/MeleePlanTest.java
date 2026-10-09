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

	@Test
	void fullSwingsBumpSpamDoesNot() {
		assertTrue(MeleePlan.shouldBump(1.0F));
		assertFalse(MeleePlan.shouldBump(0.5F));
		assertFalse(MeleePlan.shouldBump(0.0F));
	}

	@Test
	void dealMultIsStanceForLegionariesOneForEveryoneElse() {
		PlayerRuntime scutum = new PlayerRuntime();
		PlayerRuntime gladius = new PlayerRuntime();
		gladius.flipStance();

		assertEquals(0.8, MeleePlan.dealFor(legionary(), scutum, 0));
		assertEquals(1.0, MeleePlan.dealFor(legionary(), gladius, 0));
		assertEquals(1.0, MeleePlan.dealFor(juggernaut(), scutum, 0));
		assertEquals(1.0, MeleePlan.dealFor(null, scutum, 0));
	}

	@Test
	void testudoStacksOnScutumOnly() {
		PlayerRuntime scutum = new PlayerRuntime();
		TestudoAbility.fire(legionary(), scutum, 0);

		assertEquals(0.56, MeleePlan.dealFor(legionary(), scutum, 0), 1e-9);
	}

	@Test
	void takeMultIsStanceForLegionariesOneForEveryoneElse() {
		PlayerRuntime gladius = new PlayerRuntime();
		gladius.flipStance();

		assertEquals(1.2, MeleePlan.takeFor(legionary(), () -> gladius, 0));
		assertEquals(1.0, MeleePlan.takeFor(juggernaut(), () -> gladius, 0));
		assertEquals(1.0, MeleePlan.takeFor(null, () -> gladius, 0));
	}

	@Test
	void impetusStacksOnGladiusOnly() {
		PlayerRuntime gladius = new PlayerRuntime();
		gladius.flipStance();
		ImpetusAbility.fire(legionary(), gladius, 0);

		assertEquals(1.5, MeleePlan.takeFor(legionary(), () -> gladius, 0));
	}

	@Test
	void impetusBonusNeedsWindowAndLegionary() {
		PlayerRuntime live = new PlayerRuntime();
		live.flipStance();
		ImpetusAbility.fire(legionary(), live, 0);
		PlayerRuntime cold = new PlayerRuntime();
		cold.flipStance();

		assertEquals(3.0F, MeleePlan.impetusBonusFor(legionary(), live, 0, 1.0F));
		assertEquals(0.75F, MeleePlan.impetusBonusFor(legionary(), live, 0, 0.5F));
		assertEquals(0.0F, MeleePlan.impetusBonusFor(legionary(), cold, 0, 1.0F));
		assertEquals(0.0F, MeleePlan.impetusBonusFor(juggernaut(), live, 0, 1.0F));
		assertEquals(0.0F, MeleePlan.impetusBonusFor(null, live, 0, 1.0F));
	}

	@Test
	void planHitNamesTheCombinedMults() {
		PlayerRuntime scutum = new PlayerRuntime();
		TestudoAbility.fire(legionary(), scutum, 0);
		PlayerRuntime gladius = new PlayerRuntime();
		gladius.flipStance();
		ImpetusAbility.fire(legionary(), gladius, 0);

		MeleePlan.HitPlan plate = MeleePlan.planHit(legionary(), scutum, null, null, 1.0F, 0);
		MeleePlan.HitPlan glass = MeleePlan.planHit(legionary(), gladius, legionary(), () -> gladius, 1.0F, 0);

		assertEquals(0.56, plate.combined(), 1e-9);
		assertEquals(1.5, glass.take(), 1e-9);
		assertEquals(1.5, glass.combined(), 1e-9);
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

		float landed = PassiveApplier.settleHitOneShots(runtime, juggernaut(), plan, 0);

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

		assertEquals(0, PassiveApplier.settleHitOneShots(runtime, juggernaut(), plan, 0));

		assertEquals(0, runtime.stacks(juggernaut().passives().get(0)));
		assertFalse(runtime.hasRushBonusPending(0));
		assertEquals(-1, runtime.peekRendEmpowerStacks(0));
	}
}
