package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownsTest {
	@Test
	void firstUseFires() {
		assertTrue(new Cooldowns().use(Lane.ONSLAUGHT, 0));
	}

	@Test
	void secondUseInsideCooldownRejected() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.use(Lane.ONSLAUGHT, 0);

		assertFalse(cooldowns.use(Lane.ONSLAUGHT, Lane.ONSLAUGHT.cooldownMs() - 1));
		assertTrue(cooldowns.use(Lane.ONSLAUGHT, Lane.ONSLAUGHT.cooldownMs()));
	}

	@Test
	void remainingCountsDownAndClampsAtZero() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.use(Lane.ONSLAUGHT, 1_000);

		assertEquals(60_000, cooldowns.remaining(Lane.ONSLAUGHT, 1_000));
		assertEquals(45_000, cooldowns.remaining(Lane.ONSLAUGHT, 16_000));
		assertEquals(0, cooldowns.remaining(Lane.ONSLAUGHT, 99_000));
	}

	@Test
	void lanesAdvanceIndependentlyAndResetTogether() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.use(Lane.OFFLOAD, 0);

		assertEquals(Lane.OFFLOAD.cooldownMs(), cooldowns.remaining(Lane.OFFLOAD, 0));
		assertEquals(0, cooldowns.remaining(Lane.ONSLAUGHT, 0));
		assertTrue(cooldowns.use(Lane.ONSLAUGHT, 0));

		cooldowns.reset();

		assertEquals(0, cooldowns.remaining(Lane.OFFLOAD, 0));
		assertEquals(0, cooldowns.remaining(Lane.ONSLAUGHT, 0));
	}

	@Test
	void gamblerLanesAdvanceIndependently() {
		Cooldowns cooldowns = new Cooldowns();
		cooldowns.use(Lane.ROULETTE, 0);

		assertEquals(Lane.ROULETTE.cooldownMs(), cooldowns.remaining(Lane.ROULETTE, 0));
		assertEquals(0, cooldowns.remaining(Lane.ALL_IN, 0));
		assertTrue(cooldowns.use(Lane.ALL_IN, 0));
		assertEquals(Lane.ALL_IN.cooldownMs(), cooldowns.remaining(Lane.ALL_IN, 0));
		assertFalse(cooldowns.use(Lane.ROULETTE, 1));
		assertTrue(cooldowns.use(Lane.ROULETTE, Lane.ROULETTE.cooldownMs()));
	}

	/** The lane owns its own length: a seventh window is its own value here, never a copy elsewhere. */
	@Test
	void everyLaneOwnsItsOwnLength() {
		assertEquals(10_000, Lane.OFFLOAD.cooldownMs());
		assertEquals(60_000, Lane.ONSLAUGHT.cooldownMs());
		assertEquals(8_000, Lane.ROTATE.cooldownMs());
		assertEquals(50_000, Lane.OATH.cooldownMs());
		assertEquals(25_000, Lane.ROULETTE.cooldownMs());
		assertEquals(75_000, Lane.ALL_IN.cooldownMs());
	}
}
