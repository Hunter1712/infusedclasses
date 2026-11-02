package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The settlement's own mitigation arithmetic: resistance and protection layer after
 * vanilla armor, and each bypass tag skips only its layer. No player is constructed.
 */
class HitMitigationTest {
	private static final float EPSILON = 1e-6F;

	@Test
	void resistanceAndProtectionApplyInOrder() {
		assertEquals(10.0F, HitMitigation.landedAfterArmor(10.0F, -1, 0.0F, false, false, false), EPSILON);
		// Resistance I is 20%: (25 - 5) / 25.
		assertEquals(8.0F, HitMitigation.landedAfterArmor(10.0F, 0, 0.0F, false, false, false), EPSILON);
		// Resistance II is 40%: (25 - 10) / 25.
		assertEquals(6.0F, HitMitigation.landedAfterArmor(10.0F, 1, 0.0F, false, false, false), EPSILON);
		// Protection 5 points absorbs one fifth of the already-resisted amount.
		assertEquals(4.8F, HitMitigation.landedAfterArmor(10.0F, 1, 5.0F, false, false, false), EPSILON);
	}

	@Test
	void bypassTagsSkipTheExpectedLayers() {
		assertEquals(10.0F, HitMitigation.landedAfterArmor(10.0F, 1, 5.0F, true, false, false), EPSILON);
		assertEquals(8.0F, HitMitigation.landedAfterArmor(10.0F, 1, 5.0F, false, true, false), EPSILON);
		assertEquals(6.0F, HitMitigation.landedAfterArmor(10.0F, 1, 5.0F, false, false, true), EPSILON);
	}
}
