package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassiveExpiryTest {
	@Test
	void exactlyAtGraceIsStillLive() {
		assertFalse(PassiveExpiry.expired(1_000L, 5, 6_000L));
		assertTrue(PassiveExpiry.expired(1_000L, 5, 6_001L));
	}

	@Test
	void nullTimestampNeverExpires() {
		assertFalse(PassiveExpiry.expired(null, 5, Long.MAX_VALUE));
	}

	@Test
	void millisUntilFloorsAtZero() {
		assertEquals(5_000L, PassiveExpiry.millisUntil(1_000L, 5, 3, 1_000L));
		assertEquals(0L, PassiveExpiry.millisUntil(1_000L, 5, 3, 6_000L));
		assertEquals(0L, PassiveExpiry.millisUntil(1_000L, 5, 3, 9_999L));
		assertEquals(0L, PassiveExpiry.millisUntil(1_000L, 5, 0, 1_000L));
		assertEquals(0L, PassiveExpiry.millisUntil(null, 5, 3, 1_000L));
	}

	@Test
	void localTickCountsDownThenExpires() {
		assertEquals(5_000L, PassiveExpiry.tickedMillisUntil(1_000L, 5_000L, 1_000L));
		assertEquals(2_000L, PassiveExpiry.tickedMillisUntil(1_000L, 5_000L, 4_000L));
		assertEquals(0L, PassiveExpiry.tickedMillisUntil(1_000L, 5_000L, 9_999L));
		assertFalse(PassiveExpiry.locallyExpired(1_000L, 5_000L, 6_000L));
		assertTrue(PassiveExpiry.locallyExpired(1_000L, 5_000L, 6_001L));
	}
}
