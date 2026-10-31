package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The full-swing gate: anything under a full swing earns nothing, rolls nothing and
 * prices nothing; exact {@code >= 1.0F}, overcharge counts.
 */
class FullSwingTest {
	@Test
	void fullSwingsAreExactAndOverchargeCounts() {
		assertTrue(FullSwing.isFull(1.0F));
		assertTrue(FullSwing.isFull(1.5F));
	}

	@Test
	void halfSwingsAndSpamAreNotFull() {
		assertFalse(FullSwing.isFull(0.999F));
		assertFalse(FullSwing.isFull(0.5F));
		assertFalse(FullSwing.isFull(0.0F));
		assertFalse(FullSwing.isFull(-1.0F));
	}
}
