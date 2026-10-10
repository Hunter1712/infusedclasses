package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassPresentation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * What the focused picker card looks like: frame palette, accent strip, hover
 * highlight and the wrapped card body text. Each line is drawn with the colour and weight the
 * presentation module put on it, so nothing here has to know what a line says. Where
 * everything sits is CardLayout's business; this only draws.
 */
public final class ClassCardRenderer {
	private static final int ACCENT_H = 3;
	private static final int BODY_COLOR = 0xE0141414;
	private static final int DIVIDER_COLOR = 0xFF3A3A3A;
	private static final int TAGLINE_COLOR = 0xFFB0A67E;

	private ClassCardRenderer() {
	}

	/** One wrapped row: text plus where the description starts (name draws bright, desc dimmed). */
	static record TextRow(String text, int descStart) {
	}

	/**
	 * Where the description starts in a wrapped row: the name length on row one,
	 * zero after (continuation rows are pure description). Pure ints, pinned by test.
	 */
	static int descStartFor(int rowIndex, int rowLength, int prefixLength) {
		return rowIndex == 0 ? Math.min(rowLength, prefixLength) : 0;
	}

	/**
	 * Greedy word wrap over the full line, tracking the name/desc split by character
	 * offset so tone follows meaning, not row number. Names always fit row one.
	 */
	static List<TextRow> wrapRows(Font font, ClassPresentation.CardLine line, int wrapW) {
		// The join lives in CardLine.text: the split is where the description starts, never rebuilt.
		String full = line.text();
		String desc = line.desc();
		int at = desc == null || desc.isEmpty() ? full.length() : full.indexOf(desc);
		int prefixLength = at < 0 ? full.length() : at;
		List<TextRow> rows = new ArrayList<>();
		StringBuilder row = new StringBuilder();
		for (String word : full.split(" ")) {
			String add = row.isEmpty() ? word : " " + word;
			if (!row.isEmpty() && font.width(row.toString() + add) > wrapW) {
				rows.add(new TextRow(row.toString(), descStartFor(rows.size(), row.length(), prefixLength)));
				row = new StringBuilder(word);
			} else {
				row.append(add);
			}
		}
		rows.add(new TextRow(row.toString(), descStartFor(rows.size(), row.length(), prefixLength)));
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

	/** Body, accent strip, border and hover highlight. Drawn behind the widgets. Takes the whole model, like the body pass, so the screen resolves once per page. */
	public static void drawFrame(GuiGraphics graphics, CardLayout.CardRect card, ClassPresentation.CardModel model) {
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
	public static CardLayout.BodyLayout drawBody(GuiGraphics graphics, Font font, CardLayout.CardRect card,
			ClassPresentation.CardModel model, int scrollPx) {
		ClassPresentation.CardStyle style = model.style();
		List<ClassPresentation.CardLine> lines = model.text().lines();
		int cx = card.x() + card.w() / 2;
		int ty = card.y() + 10;
		graphics.drawCenteredString(font, model.text().displayName(), cx, ty, style.nameColor());
		ty += 11;
		graphics.drawCenteredString(font, model.text().tagline(), cx, ty, TAGLINE_COLOR);
		ty += 12;
		graphics.fill(card.x() + CardLayout.PAD, ty, card.x() + card.w() - CardLayout.PAD, ty + 1, DIVIDER_COLOR);

		int top = CardLayout.bodyTop(card);
		int stop = CardLayout.stopY(card);
		int rows = 0;
		for (ClassPresentation.CardLine line : lines) {
			net.minecraft.network.chat.Style rowStyle = line.style().bold()
					? net.minecraft.network.chat.Style.EMPTY.withBold(true)
					: net.minecraft.network.chat.Style.EMPTY;
			for (TextRow row : wrapRows(font, line, card.w() - CardLayout.PAD * 2)) {
				int y = top + rows * CardLayout.ROW_H - scrollPx;
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
		return CardLayout.layoutFor(card, rows);
	}
}
