package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The universal full-swing gate: exact at 1.0, overcharge counts, anything under it
 * earns and rolls nothing.
 */
class BonusCurveTest {
	@Test
	void fullSwingsAreExactAndOverchargeCounts() {
		assertTrue(BonusCurve.isFullSwing(1.0F));
		assertTrue(BonusCurve.isFullSwing(1.5F));
	}

	@Test
	void halfSwingsAndSpamAreNotFull() {
		assertFalse(BonusCurve.isFullSwing(0.999F));
		assertFalse(BonusCurve.isFullSwing(0.5F));
		assertFalse(BonusCurve.isFullSwing(0.0F));
		assertFalse(BonusCurve.isFullSwing(-1.0F));
	}
}
