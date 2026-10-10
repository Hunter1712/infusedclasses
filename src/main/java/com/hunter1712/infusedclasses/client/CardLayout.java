package com.hunter1712.infusedclasses.client;

/**
 * Pure layout for the focused picker card: every rectangle, anchor and scroll
 * range in ints, no graphics object in sight. The screen positions widgets from
 * here; the card renderer only draws. Paging lives here too, not on the screen:
 * wraparound math is pure and stays pinned without a running game.
 */
public final class CardLayout {
	/** Card chrome: inner padding, pinned select height, arrow and dot metrics. */
	public static final int PAD = 10;
	public static final int BUTTON_H = 20;
	public static final int ARROW_W = 24;
	public static final int ARROW_H = 24;
	public static final int DOT_W = 20;
	public static final int DOT_H = 14;
	public static final int DOT_GAP = 4;

	/** Focused card rectangle, in screen pixels. */
	public record CardRect(int x, int y, int w, int h) {
	}

	/** Widget rectangle inside screen pixels: the screen positions widgets, never card internals. */
	public record ButtonRect(int x, int y, int w, int h) {
	}

	private CardLayout() {
	}

	/**
	 * Focused card math: one card per page, centered, two-thirds of the width and
	 * three-fifths of the height, clamped so title, subtitle and footer stay visible.
	 * Pure ints: the picker never overflows like the old fixed row.
	 */
	public static CardRect focusedCard(int screenWidth, int screenHeight) {
		int w = Math.min(Math.max(screenWidth * 2 / 3, 160), 520);
		if (w > screenWidth) {
			w = screenWidth;
		}
		int h = Math.min(screenHeight * 3 / 5, 520);
		if (h > screenHeight) {
			h = screenHeight;
		}
		int x = (screenWidth - w) / 2;
		int y = Math.max(52, (screenHeight - h) / 2 + 8);
		return new CardRect(x, y, w, h);
	}

	/** Pinned select control: full inner width along the card bottom. */
	public static ButtonRect selectRect(CardRect card) {
		return new ButtonRect(
				card.x() + PAD,
				card.y() + card.h() - PAD - BUTTON_H,
				card.w() - PAD * 2,
				BUTTON_H);
	}

	/** Previous-page arrow: left of the card, vertically centered, clamped on screen. */
	public static ButtonRect prevRect(int screenWidth, CardRect card) {
		return new ButtonRect(
				Math.max(8, card.x() - ARROW_W - 12),
				card.y() + card.h() / 2 - ARROW_H / 2,
				ARROW_W,
				ARROW_H);
	}

	/** Next-page arrow: right of the card, vertically centered, clamped on screen. */
	public static ButtonRect nextRect(int screenWidth, CardRect card) {
		return new ButtonRect(
				Math.min(screenWidth - ARROW_W - 8, card.x() + card.w() + 12),
				card.y() + card.h() / 2 - ARROW_H / 2,
				ARROW_W,
				ARROW_H);
	}

	/** Page dots live in one centered row between the card and the footer hint. */
	public static int dotsY(CardRect card) {
		return card.y() + card.h() + 6;
	}

	public static int dotX(int index, int count, int screenWidth) {
		int rowWidth = count * DOT_W + (count - 1) * DOT_GAP;
		return screenWidth / 2 - rowWidth / 2 + index * (DOT_W + DOT_GAP);
	}

	/** Title, subtitle and footer anchor off the card, so they never overlap it. */
	public static int titleY(CardRect card) {
		return card.y() - 44;
	}

	public static int subtitleY(CardRect card) {
		return card.y() - 28;
	}

	public static int footerY(CardRect card) {
		return card.y() + card.h() + 24;
	}

	/** Page math with wraparound at both ends, so navigation never dead-ends. */
	public static int nextPage(int page, int count) {
		return (page + 1) % count;
	}

	public static int prevPage(int page, int count) {
		return (page + count - 1) % count;
	}

	public static boolean isHovering(CardRect card, int mouseX, int mouseY) {
		return mouseX >= card.x() && mouseX < card.x() + card.w()
				&& mouseY >= card.y() && mouseY < card.y() + card.h();
	}

	/** First text row of the body: name, tagline and divider sit above it. */
	public static int bodyTop(CardRect card) {
		return card.y() + 10 + 11 + 12 + 1 + 6;
	}

	/** Last text row: the pinned select control sits below it, never scrolled. */
	public static int stopY(CardRect card) {
		return selectRect(card).y() - 4;
	}

	/** Clamps a scroll offset into its content: 0 at top, never past the end. */
	public static int clampScroll(int scroll, int maxScroll) {
		return Math.max(0, Math.min(scroll, Math.max(0, maxScroll)));
	}

	/** Body text geometry: wrapped rows height plus how far they scroll. Pure ints. */
	public record BodyLayout(int textH, int maxScroll) {
	}

	static final int ROW_H = 11;

	/** Layout for a wrapped row count: text height plus the scroll range that shows it all. */
	public static BodyLayout layoutFor(CardRect card, int rows) {
		int textH = rows * ROW_H;
		return new BodyLayout(textH, Math.max(0, bodyTop(card) + textH - stopY(card)));
	}

	/**
	 * Scrollbar thumb for scrolled content, null when everything fits. Pure ints:
	 * track runs the visible rows, thumb size follows the visible share.
	 */
	public static ButtonRect scrollBar(CardRect card, BodyLayout layout, int scrollPx) {
		int visibleH = stopY(card) - bodyTop(card);
		if (layout.maxScroll() <= 0) {
			return null;
		}
		int thumbH = Math.max(12, visibleH * visibleH / layout.textH());
		int thumbY = bodyTop(card) + scrollPx * (visibleH - thumbH) / layout.maxScroll();
		return new ButtonRect(card.x() + card.w() - PAD + 2, thumbY, 3, thumbH);
	}
}
