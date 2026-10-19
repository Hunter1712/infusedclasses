package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.CardPalette;
import com.hunter1712.infusedclasses.ClassPresentation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * What the focused picker card looks like: frame palette, accent strip, hover
 * highlight and the wrapped card body text. Each line's name draws with the colour and weight
 * the presentation module put on it, while every description draws the palette's one shared
 * subtle tone, so nothing here has to know what a line says; the frame tones also come from
 * the card palette, so nothing here picks a class colour either. The body
 * left-aligns at the card's padding and checkpoint rows indent past it, so a bar's tiers
 * hang off their rules line. Where everything sits is CardLayout's business; this only draws.
 */
public final class ClassCardRenderer {
	private static final int ACCENT_H = 3;

	private ClassCardRenderer() {
	}

	/** One wrapped row: its text, where the description starts, the style it draws with, and how far it indents. */
	static record TextRow(String text, int descStart, ClassPresentation.LineStyle style, int indent) {
	}

	/** Wrapped body rows plus the scroll layout they imply: measured once per frame, drawn from the result. */
	static record Measured(List<TextRow> rows, CardLayout.BodyLayout layout) {
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
	 * offset so tone follows meaning, not row number. Names always fit row one. Widths
	 * arrive as a function of the string, so the wrap is assertable with a stub measure;
	 * the screen passes {@code font::width}.
	 */
	static List<TextRow> wrapRows(ClassPresentation.CardLine line, int wrapW, ToIntFunction<String> width) {
		// The join lives in CardLine.text; the boundary is derived there, never searched.
		String full = line.text();
		int prefixLength = line.descStart();
		int indent = CardLayout.rowIndent(line.kind());
		List<TextRow> rows = new ArrayList<>();
		StringBuilder row = new StringBuilder();
		for (String word : full.split(" ")) {
			String add = row.isEmpty() ? word : " " + word;
			if (!row.isEmpty() && width.applyAsInt(row.toString() + add) > wrapW) {
				rows.add(new TextRow(row.toString(),
						descStartFor(rows.size(), row.length(), prefixLength), line.style(), indent));
				row = new StringBuilder(word);
			} else {
				row.append(add);
			}
		}
		rows.add(new TextRow(row.toString(),
				descStartFor(rows.size(), row.length(), prefixLength), line.style(), indent));
		return rows;
	}

	/**
	 * Wraps the whole body for one frame: every row with its style, plus the scroll
	 * layout those rows imply. The caller clamps its scroll from the result before
	 * drawing, so no frame draws past a range that frame already knows.
	 */
	static Measured measure(CardLayout.CardRect card, ClassPresentation.CardModel model, ToIntFunction<String> width) {
		List<TextRow> rows = new ArrayList<>();
		int wrapW = CardLayout.innerWidth(card);
		for (ClassPresentation.CardLine line : model.text().lines()) {
			rows.addAll(wrapRows(line, wrapW, width));
		}
		return new Measured(List.copyOf(rows), CardLayout.layoutFor(card, rows.size()));
	}

	/** Body, accent strip, border and hover highlight. Drawn behind the widgets. Takes the whole model, like the body pass, so the screen resolves once per page. */
	public static void drawFrame(GuiGraphics graphics, CardLayout.CardRect card, ClassPresentation.CardModel model) {
		ClassPresentation.CardText text = model.text();
		ClassPresentation.CardStyle style = model.style();
		int x1 = card.x() + card.w();
		int y1 = card.y() + card.h();
		graphics.fill(card.x(), card.y(), x1, y1, CardPalette.body(text.accent()));
		graphics.fill(card.x(), card.y(), x1, card.y() + ACCENT_H, text.accent());
		graphics.fill(card.x(), card.y(), card.x() + 1, y1, style.borderColor());
		graphics.fill(x1 - 1, card.y(), x1, y1, style.borderColor());
		graphics.fill(card.x(), card.y(), x1, card.y() + 1, style.borderColor());
		graphics.fill(card.x(), y1 - 1, x1, y1, style.borderColor());
	}

	/**
	 * Name, tagline, divider and the measured body rows of one card: the screen resolves
	 * the model once and measures once, never the parts. Every line starts at the body's
	 * left edge and checkpoint rows indent past it, so a bar's tiers hang off their rules
	 * line; the divider and tagline take the class accent's own tones. Long lines wrap
	 * inside the card; rows past the visible slice scroll under the wheel instead of
	 * clipping away, and the pinned select control never moves. Names draw in their
	 * line's own colour, descriptions and their continuations in the palette's one
	 * shared subtle tone; where everything sits is CardLayout's anchors.
	 */
	public static void drawBody(GuiGraphics graphics, Font font, CardLayout.CardRect card,
			ClassPresentation.CardModel model, Measured measured, int scrollPx) {
		ClassPresentation.CardStyle style = model.style();
		int accent = model.text().accent();
		int left = CardLayout.bodyLeft(card);
		graphics.drawString(font, model.text().displayName(), left, CardLayout.nameY(card), style.nameColor());
		graphics.drawString(font, model.text().tagline(), left, CardLayout.taglineY(card),
				CardPalette.tagline(accent));
		graphics.fill(left, CardLayout.dividerY(card),
				left + CardLayout.innerWidth(card),
				CardLayout.dividerY(card) + CardLayout.DIVIDER_H, CardPalette.divider(accent));

		int top = CardLayout.bodyTop(card);
		int stop = CardLayout.stopY(card);
		List<TextRow> rows = measured.rows();
		for (int i = 0; i < rows.size(); i++) {
			TextRow row = rows.get(i);
			int y = top + i * CardLayout.ROW_H - scrollPx;
			if (y >= top && y + font.lineHeight <= stop) {
				net.minecraft.network.chat.Style rowStyle = row.style().bold()
						? net.minecraft.network.chat.Style.EMPTY.withBold(true)
						: net.minecraft.network.chat.Style.EMPTY;
				String head = row.text().substring(0, row.descStart());
				String tail = row.text().substring(row.descStart());
				int x = left + row.indent();
				graphics.drawString(font, FormattedCharSequence.forward(head, rowStyle), x, y,
						row.style().color());
				graphics.drawString(font, FormattedCharSequence.forward(tail, rowStyle),
						x + font.width(head), y, CardPalette.descText());
			}
		}
	}
}
