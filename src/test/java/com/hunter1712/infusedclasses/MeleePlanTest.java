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
}
