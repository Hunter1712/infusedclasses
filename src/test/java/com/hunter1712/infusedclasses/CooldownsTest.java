package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownsTest {
	@Test
	void firstUseFires() {
		assertTrue(new Cooldowns().useRend(0));
	}

	@Test
	void secondUseInsideCooldownRejected() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.useRend(0);

		assertFalse(cooldowns.useRend(Cooldowns.REND_MS - 1));
		assertTrue(cooldowns.useRend(Cooldowns.REND_MS));
	}

	@Test
	void remainingCountsDownAndClampsAtZero() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.useRend(1_000);

		assertEquals(60_000, cooldowns.rendRemaining(1_000));
		assertEquals(45_000, cooldowns.rendRemaining(16_000));
		assertEquals(0, cooldowns.rendRemaining(99_000));
	}

	@Test
	void lanesAdvanceIndependentlyAndResetTogether() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.useRush(0);

		assertEquals(Cooldowns.RUSH_MS, cooldowns.rushRemaining(0));
		assertEquals(0, cooldowns.rendRemaining(0));
		assertTrue(cooldowns.useRend(0));

		cooldowns.reset();

		assertEquals(0, cooldowns.rushRemaining(0));
		assertEquals(0, cooldowns.rendRemaining(0));
	}
}
