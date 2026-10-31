package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The HP-scaled modifier plan, asserted as a decision: bucket to amount, and write versus
 * remove versus skip. Plain JUnit: no player, level, attribute instance or graphics object
 * is constructed to make these assertions.
 */
class PassiveEffectTest {
	private static final HpScaledPassive VIGOR = new HpScaledPassive(ClassRegistry.ATTACK_DAMAGE,
			Identifier.fromNamespaceAndPath("infusedclasses", "test_damage"), 0.3, "Test", -1.0);

	@Test
	void bucketZeroYieldsNoModifier() {
		assertNull(PassiveEffect.forBucket(VIGOR, 0));
		assertNull(PassiveEffect.forBucket(VIGOR, -1));
		assertNull(PassiveEffect.forBucket(null, 5));
	}

	@Test
	void bonusIsBasePlusBucketTimesPerStep() {
		assertEquals(0.5, PassiveEffect.forBucket(VIGOR, 5).amount());
		assertEquals(2.0, PassiveEffect.forBucket(VIGOR, 10).amount());
	}

	@Test
	void bonusNeverExceedsTheCap() {
		assertEquals(2.0, PassiveEffect.forBucket(VIGOR, 11).amount());
		assertEquals(2.0, PassiveEffect.forBucket(VIGOR, 99).amount());
	}

	@Test
	void theModifierCarriesItsOwnAttributeAndId() {
		PassiveEffect.Modifier wanted = PassiveEffect.forBucket(VIGOR, 3);

		assertEquals(ClassRegistry.ATTACK_DAMAGE, wanted.attribute());
		assertEquals(Identifier.fromNamespaceAndPath("infusedclasses", "test_damage"), wanted.modifierId());
	}

	@Test
	void bucketsWriteLiveAndRemoveDead() {
		assertTrue(PassiveEffect.planForBucket(VIGOR, 10) instanceof PassiveEffect.Plan.Write write
				&& write.wanted().amount() == 2.0);
		assertTrue(PassiveEffect.planForBucket(VIGOR, 0) instanceof PassiveEffect.Plan.Remove remove
				&& remove.attribute().equals(VIGOR.attribute()));
		assertTrue(PassiveEffect.planForBucket(null, 0) instanceof PassiveEffect.Plan.Skip);
	}
}
