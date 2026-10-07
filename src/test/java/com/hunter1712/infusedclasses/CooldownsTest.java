package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownsTest {
	@Test
	void firstUseFires() {
		assertTrue(new Cooldowns().useSecondWind(0));
	}

	@Test
	void secondUseInsideCooldownRejected() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.useSecondWind(0);

		assertFalse(cooldowns.useSecondWind(Cooldowns.SECOND_WIND_MS - 1));
		assertTrue(cooldowns.useSecondWind(Cooldowns.SECOND_WIND_MS));
	}

	@Test
	void remainingCountsDownAndClampsAtZero() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.useSecondWind(1_000);

		assertEquals(60_000, cooldowns.secondWindRemaining(1_000));
		assertEquals(45_000, cooldowns.secondWindRemaining(16_000));
		assertEquals(0, cooldowns.secondWindRemaining(99_000));
	}

	@Test
	void lanesAdvanceIndependentlyAndResetTogether() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.useBloodlust(0);

		assertEquals(Cooldowns.BLOODLUST_MS, cooldowns.bloodlustRemaining(0));
		assertEquals(0, cooldowns.secondWindRemaining(0));
		assertTrue(cooldowns.useSecondWind(0));

		cooldowns.reset();

		assertEquals(0, cooldowns.bloodlustRemaining(0));
		assertEquals(0, cooldowns.secondWindRemaining(0));
	}
}
