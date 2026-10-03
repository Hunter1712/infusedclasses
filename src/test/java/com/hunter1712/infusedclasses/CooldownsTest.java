package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownsTest {
	@Test
	void firstUseFires() {
		assertTrue(new Cooldowns().useWarCry(0));
	}

	@Test
	void secondUseInsideCooldownRejected() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.useWarCry(0);

		assertFalse(cooldowns.useWarCry(Cooldowns.WAR_CRY_MS - 1));
		assertTrue(cooldowns.useWarCry(Cooldowns.WAR_CRY_MS));
	}

	@Test
	void remainingCountsDownAndClampsAtZero() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.useWarCry(1_000);

		assertEquals(30_000, cooldowns.warCryRemaining(1_000));
		assertEquals(15_000, cooldowns.warCryRemaining(16_000));
		assertEquals(0, cooldowns.warCryRemaining(99_000));
	}
}
