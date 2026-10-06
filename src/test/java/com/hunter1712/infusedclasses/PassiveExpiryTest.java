package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

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
}
