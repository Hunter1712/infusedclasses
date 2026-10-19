package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Universal charged rule and full-swing gate: full swings pay full bonus, half pays a
 * quarter, spam pays nothing; anything under a full swing earns and rolls nothing.
 */
class BonusCurveTest {
	@Test
	void curvePaysFullHalfAndSpam() {
		assertEquals(6.0F, BonusCurve.bonusFor(6.0F, 1.0F));
		assertEquals(1.5F, BonusCurve.bonusFor(6.0F, 0.5F));
		assertEquals(0.0F, BonusCurve.bonusFor(6.0F, 0.0F));
	}

	@Test
	void overchargeClampsAndDeadBaseReadsZero() {
		assertEquals(6.0F, BonusCurve.bonusFor(6.0F, 1.5F));
		assertEquals(0.0F, BonusCurve.bonusFor(6.0F, -1.0F));
		assertEquals(0.0F, BonusCurve.bonusFor(0.0F, 1.0F));
		assertEquals(0.0F, BonusCurve.bonusFor(-3.0F, 1.0F));
	}

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
