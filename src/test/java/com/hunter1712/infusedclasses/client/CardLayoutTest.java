package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.client.CardLayout.CardRect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused card math, asserted without a running game. */
class CardLayoutTest {
	private static void assertOnScreen(int screenW, int screenH, CardRect card) {
		assertTrue(card.x() >= 0, "card left on screen");
		assertTrue(card.x() + card.w() <= screenW, "card right on screen");
		assertTrue(CardLayout.titleY(card) >= 0, "title on screen");
		assertTrue(CardLayout.footerY(card) <= screenH, "footer on screen");
	}

	private static void assertWidgetsOnScreen(int screenW, int screenH, CardRect card, int dots) {
		var select = CardLayout.selectRect(card);
		assertTrue(select.x() >= card.x() && select.x() + select.w() <= card.x() + card.w());
		assertTrue(select.y() >= card.y() && select.y() + select.h() <= card.y() + card.h());
		var prev = CardLayout.prevRect(card);
		var next = CardLayout.nextRect(screenW, card);
		assertTrue(prev.x() >= 0 && prev.x() + prev.w() <= screenW);
		assertTrue(next.x() >= 0 && next.x() + next.w() <= screenW);
		for (int i = 0; i < dots; i++) {
			int x = CardLayout.dotX(i, dots, screenW);
			assertTrue(x >= 0 && x + CardLayout.DOT_W <= screenW, "dot " + i + " on screen");
		}
	}

	@Test
	void focusedCardIsCentredAndContained() {
		for (int[] size : new int[][]{{854, 480}, {1280, 720}, {426, 240}, {320, 240}}) {
			CardRect card = CardLayout.focusedCard(size[0], size[1]);

			assertTrue(Math.abs((card.x() + card.w() / 2) - size[0] / 2) <= 1, "card centred");
			assertOnScreen(size[0], size[1], card);
			assertWidgetsOnScreen(size[0], size[1], card, 2);
		}
	}

	@Test
	void wideCardStaysInsideTheMax() {
		CardRect card = CardLayout.focusedCard(1920, 1080);

		assertEquals(520, card.w());
		assertTrue(card.x() >= 0 && card.x() + card.w() <= 1920);
	}

	@Test
	void scrollClampsAndScrollbarShowsOnlyOnOverflow() {
		CardRect card = CardLayout.focusedCard(854, 480);

		assertEquals(0, CardLayout.clampScroll(-5, 100));
		assertEquals(100, CardLayout.clampScroll(999, 100));
		assertEquals(30, CardLayout.clampScroll(30, 100));
		assertEquals(0, CardLayout.clampScroll(10, 0));

		assertEquals(null, CardLayout.scrollBar(card,
				CardLayout.layoutFor(card, 5), 0));

		int rows = 60;
		var layout = CardLayout.layoutFor(card, rows);
		assertTrue(layout.maxScroll() > 0);
		var bar = CardLayout.scrollBar(card, layout, 0);
		assertTrue(bar != null);
		assertTrue(bar.y() >= CardLayout.bodyTop(card));
		assertTrue(bar.y() + bar.h() <= CardLayout.stopY(card));
		assertEquals(3, bar.w());
		var end = CardLayout.scrollBar(card, layout, layout.maxScroll());
		assertTrue(end.y() > bar.y());
		assertTrue(end.y() + end.h() <= CardLayout.stopY(card) + 1);
	}

	@Test
	void headerAnchorsStackNameTaglineDividerBody() {
		CardRect card = CardLayout.focusedCard(854, 480);

		assertEquals(card.y() + 10, CardLayout.nameY(card));
		assertEquals(CardLayout.nameY(card) + 11, CardLayout.taglineY(card));
		assertEquals(CardLayout.taglineY(card) + 12, CardLayout.dividerY(card));
		assertEquals(CardLayout.dividerY(card) + CardLayout.DIVIDER_H + 6, CardLayout.bodyTop(card));
	}

	@Test
	void dimmedKeepsAlphaAndDarkens() {
		assertEquals(0xFF6BB5D8, ClassCardRenderer.dimmed(0xFF7ED6FF));
		assertEquals(0xFFA0A0A0, ClassCardRenderer.dimmed(0xFFBDBDBD));
		assertEquals(0xFFD8D8D8, ClassCardRenderer.dimmed(0xFFFFFFFF));
	}

	@Test
	void descStartsOnlyOnRowOne() {
		assertEquals(10, ClassCardRenderer.descStartFor(0, 20, 10));
		assertEquals(5, ClassCardRenderer.descStartFor(0, 5, 10));
		assertEquals(0, ClassCardRenderer.descStartFor(1, 20, 10));
		assertEquals(0, ClassCardRenderer.descStartFor(2, 8, 10));
	}

	@Test
	void pagingWrapsAtBothEnds() {
		assertEquals(0, CardLayout.nextPage(1, 2));
		assertEquals(1, CardLayout.nextPage(0, 2));
		assertEquals(1, CardLayout.prevPage(0, 2));
		assertEquals(0, CardLayout.prevPage(1, 2));
	}

	@Test
	void hoveringIsTheCardRect() {
		CardRect card = new CardRect(10, 20, 100, 80);

		assertTrue(CardLayout.isHovering(card, 10, 20));
		assertTrue(CardLayout.isHovering(card, 109, 99));
		assertFalse(CardLayout.isHovering(card, 110, 20));
		assertFalse(CardLayout.isHovering(card, 10, 100));
	}
}
