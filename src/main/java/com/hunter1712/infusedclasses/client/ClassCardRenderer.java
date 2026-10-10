package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassPresentation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the focused picker card looks like: frame palette, accent strip, hover
 * highlight and the wrapped card body text. Each line is drawn with the colour and weight the
 * presentation module put on it, so nothing here has to know what a line says. The screen
 * decides which page shows; the rectangles below decide where it sits, all in pure ints.
 */
public final class ClassCardRenderer {
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

	private static final int ACCENT_H = 3;
	private static final int BODY_COLOR = 0xE0141414;
	private static final int DIVIDER_COLOR = 0xFF3A3A3A;
	private static final int TAGLINE_COLOR = 0xFFB0A67E;

	private ClassCardRenderer() {
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

	/** One wrapped row: text plus where the description starts (name draws bright, desc dimmed). */
	static record TextRow(String text, int descStart) {
	}

	/**
	 * Greedy word wrap over the full line, tracking the name/desc split by character
	 * offset so tone follows meaning, not row number. Names always fit row one.
	 */
	static List<TextRow> wrapRows(Font font, String name, String desc, int wrapW) {
		String prefix = desc == null || desc.isEmpty() ? name : name + " \u2014 ";
		String full = desc == null || desc.isEmpty() ? name : prefix + desc;
		List<TextRow> rows = new ArrayList<>();
		StringBuilder row = new StringBuilder();
		for (String word : full.split(" ")) {
			String add = row.isEmpty() ? word : " " + word;
			if (!row.isEmpty() && font.width(row.toString() + add) > wrapW) {
				rows.add(new TextRow(row.toString(), Math.min(row.length(), prefix.length())));
				row = new StringBuilder(word);
			} else {
				row.append(add);
			}
		}
		rows.add(new TextRow(row.toString(), Math.min(row.length(), prefix.length())));
		return rows;
	}

	/** Dimmed copy of a line color for description continuations: names scan, descs recede. */
	static int dimmed(int argb) {
		int a = argb >>> 24;
		int r = (argb >> 16 & 0xFF) * 85 / 100;
		int g = (argb >> 8 & 0xFF) * 85 / 100;
		int b = (argb & 0xFF) * 85 / 100;
		return a << 24 | r << 16 | g << 8 | b;
	}

	/** Body text geometry: wrapped rows height plus how far they scroll. Pure ints. */
	public record BodyLayout(int textH, int maxScroll) {
	}

	private static final int ROW_H = 11;

	/** Layout for a wrapped row count: text height plus the scroll range that shows it all. */
	public static BodyLayout layoutFor(CardRect card, int rows) {
		int visibleH = stopY(card) - bodyTop(card);
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

	/** Body, accent strip, border and hover highlight. Drawn behind the widgets. Takes the whole model, like the body pass, so the screen resolves once per page. */
	public static void drawFrame(GuiGraphics graphics, CardRect card, ClassPresentation.CardModel model) {
		ClassPresentation.CardText text = model.text();
		ClassPresentation.CardStyle style = model.style();
		int x1 = card.x() + card.w();
		int y1 = card.y() + card.h();
		graphics.fill(card.x(), card.y(), x1, y1, BODY_COLOR);
		graphics.fill(card.x(), card.y(), x1, card.y() + ACCENT_H, text.accent());
		graphics.fill(card.x(), card.y(), card.x() + 1, y1, style.borderColor());
		graphics.fill(x1 - 1, card.y(), x1, y1, style.borderColor());
		graphics.fill(card.x(), card.y(), x1, card.y() + 1, style.borderColor());
		graphics.fill(card.x(), y1 - 1, x1, y1, style.borderColor());
		if (style.washColor() != null) {
			graphics.fill(card.x() + 1, card.y() + ACCENT_H, x1 - 1, y1 - 1, style.washColor());
		}
	}

	/**
	 * Name, tagline, divider and the card detail lines from one card model: the screen resolves
	 * the model once, never the parts. Long lines wrap inside the card; rows past the
	 * visible slice scroll under the wheel instead of clipping away, and the pinned
	 * select control never moves. The first wrapped row of a line draws in its own
	 * color, continuations draw dimmed. Returns the body layout for scroll clamping.
	 */
	public static BodyLayout drawBody(GuiGraphics graphics, Font font, CardRect card,
			ClassPresentation.CardModel model, int scrollPx) {
		ClassPresentation.CardStyle style = model.style();
		List<ClassPresentation.CardLine> lines = model.text().lines();
		int cx = card.x() + card.w() / 2;
		int ty = card.y() + 10;
		graphics.drawCenteredString(font, model.text().displayName(), cx, ty, style.nameColor());
		ty += 11;
		graphics.drawCenteredString(font, model.text().tagline(), cx, ty, TAGLINE_COLOR);
		ty += 12;
		graphics.fill(card.x() + PAD, ty, card.x() + card.w() - PAD, ty + 1, DIVIDER_COLOR);

		int top = bodyTop(card);
		int stop = stopY(card);
		int rows = 0;
		for (ClassPresentation.CardLine line : lines) {
			net.minecraft.network.chat.Style rowStyle = line.style().bold()
					? net.minecraft.network.chat.Style.EMPTY.withBold(true)
					: net.minecraft.network.chat.Style.EMPTY;
			for (TextRow row : wrapRows(font, line.name(), line.desc(), card.w() - PAD * 2)) {
				int y = top + rows * ROW_H - scrollPx;
				if (y >= top && y + 9 <= stop) {
					String head = row.text().substring(0, row.descStart());
					String tail = row.text().substring(row.descStart());
					int x = cx - (font.width(head) + font.width(tail)) / 2;
					graphics.drawString(font, FormattedCharSequence.forward(head, rowStyle), x, y,
							line.style().color());
					graphics.drawString(font, FormattedCharSequence.forward(tail, rowStyle),
							x + font.width(head), y, dimmed(line.style().color()));
				}
				rows++;
			}
		}
		return layoutFor(card, rows);
	}
}
