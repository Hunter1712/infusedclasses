package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.client.ClassCardRenderer.CardRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused card math, asserted without a running game. */
class CardLayoutTest {
	private static void assertOnScreen(int screenW, int screenH, CardRect card) {
		assertTrue(card.x() >= 0, "card left on screen");
		assertTrue(card.x() + card.w() <= screenW, "card right on screen");
		assertTrue(ClassCardRenderer.titleY(card) >= 0, "title on screen");
		assertTrue(ClassCardRenderer.footerY(card) <= screenH, "footer on screen");
	}

	private static void assertWidgetsOnScreen(int screenW, int screenH, CardRect card, int dots) {
		var select = ClassCardRenderer.selectRect(card);
		assertTrue(select.x() >= card.x() && select.x() + select.w() <= card.x() + card.w());
		assertTrue(select.y() >= card.y() && select.y() + select.h() <= card.y() + card.h());
		var prev = ClassCardRenderer.prevRect(screenW, card);
		var next = ClassCardRenderer.nextRect(screenW, card);
		assertTrue(prev.x() >= 0 && prev.x() + prev.w() <= screenW);
		assertTrue(next.x() >= 0 && next.x() + next.w() <= screenW);
		for (int i = 0; i < dots; i++) {
			int x = ClassCardRenderer.dotX(i, dots, screenW);
			assertTrue(x >= 0 && x + ClassCardRenderer.DOT_W <= screenW, "dot " + i + " on screen");
		}
	}

	@Test
	void focusedCardIsCentredAndContained() {
		for (int[] size : new int[][]{{854, 480}, {1280, 720}, {426, 240}, {320, 240}}) {
			CardRect card = ClassCardRenderer.focusedCard(size[0], size[1]);

			assertTrue(Math.abs((card.x() + card.w() / 2) - size[0] / 2) <= 1, "card centred");
			assertOnScreen(size[0], size[1], card);
			assertWidgetsOnScreen(size[0], size[1], card, 2);
		}
	}

	@Test
	void wideCardStaysInsideTheMax() {
		CardRect card = ClassCardRenderer.focusedCard(1920, 1080);

		assertEquals(520, card.w());
		assertTrue(card.x() >= 0 && card.x() + card.w() <= 1920);
	}

	@Test
	void scrollClampsAndScrollbarShowsOnlyOnOverflow() {
		CardRect card = ClassCardRenderer.focusedCard(854, 480);

		assertEquals(0, ClassCardRenderer.clampScroll(-5, 100));
		assertEquals(100, ClassCardRenderer.clampScroll(999, 100));
		assertEquals(30, ClassCardRenderer.clampScroll(30, 100));
		assertEquals(0, ClassCardRenderer.clampScroll(10, 0));

		assertEquals(null, ClassCardRenderer.scrollBar(card,
				ClassCardRenderer.layoutFor(card, 5), 0));

		int rows = 60;
		var layout = ClassCardRenderer.layoutFor(card, rows);
		assertTrue(layout.maxScroll() > 0);
		var bar = ClassCardRenderer.scrollBar(card, layout, 0);
		assertTrue(bar != null);
		assertTrue(bar.y() >= ClassCardRenderer.bodyTop(card));
		assertTrue(bar.y() + bar.h() <= ClassCardRenderer.stopY(card));
		assertEquals(3, bar.w());
		var end = ClassCardRenderer.scrollBar(card, layout, layout.maxScroll());
		assertTrue(end.y() > bar.y());
		assertTrue(end.y() + end.h() <= ClassCardRenderer.stopY(card) + 1);
	}

	@Test
	void dimmedKeepsAlphaAndDarkens() {
		assertEquals(0xFF6BB5D8, ClassCardRenderer.dimmed(0xFF7ED6FF));
		assertEquals(0xFFA0A0A0, ClassCardRenderer.dimmed(0xFFBDBDBD));
		assertEquals(0xFFD8D8D8, ClassCardRenderer.dimmed(0xFFFFFFFF));
	}

	@Test
	void pagingWrapsAtBothEnds() {
		assertEquals(0, ClassCardRenderer.nextPage(1, 2));
		assertEquals(1, ClassCardRenderer.nextPage(0, 2));
		assertEquals(1, ClassCardRenderer.prevPage(0, 2));
		assertEquals(0, ClassCardRenderer.prevPage(1, 2));
	}

	@Test
	void hoveringIsTheCardRect() {
		CardRect card = new CardRect(10, 20, 100, 80);

		assertTrue(ClassCardRenderer.isHovering(card, 10, 20));
		assertTrue(ClassCardRenderer.isHovering(card, 109, 99));
		assertFalse(ClassCardRenderer.isHovering(card, 110, 20));
		assertFalse(ClassCardRenderer.isHovering(card, 10, 100));
	}
}
