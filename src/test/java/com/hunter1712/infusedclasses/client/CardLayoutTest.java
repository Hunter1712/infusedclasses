package com.hunter1712.infusedclasses.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Row math next to the card metrics, asserted without a running game. */
class CardLayoutTest {
	@Test
	void singleCardIsCentred() {
		assertEquals(140, ClassCardRenderer.cardX(0, 1, 400, 12));
	}

	@Test
	void rowOfThreeIsCentredWithGaps() {
		int left = ClassCardRenderer.cardX(0, 3, 400, 12);
		assertEquals(left + 120 + 12, ClassCardRenderer.cardX(1, 3, 400, 12));
		assertEquals(left + 2 * (120 + 12), ClassCardRenderer.cardX(2, 3, 400, 12));
		assertEquals(400 / 2, left + ClassCardRenderer.rowWidth(3, 12) / 2);
	}

	@Test
	void cardsTopNeverCrowdsTheTitle() {
		assertEquals(64, ClassCardRenderer.cardsTop(100));
		assertTrue(ClassCardRenderer.cardsTop(800) > 64);
	}

	@Test
	void hoveringIsTheCardRect() {
		assertTrue(ClassCardRenderer.isHovering(10, 20, 10, 20));
		assertTrue(ClassCardRenderer.isHovering(10, 20, 129, 175));
		assertFalse(ClassCardRenderer.isHovering(10, 20, 130, 20));
		assertFalse(ClassCardRenderer.isHovering(10, 20, 10, 176));
	}
}
