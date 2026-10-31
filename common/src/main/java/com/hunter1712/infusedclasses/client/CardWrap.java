package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassPresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The card body's text layout as a decision: one wrapped row, and the whole body
 * measured for a frame. The wrap tracks the name/description split by character offset
 * so tone follows meaning, not row number, and it takes its widths as a function of the
 * string, so the layout is assertable with a stub measure and the screen passes
 * {@code font::width}. Where everything sits is {@link CardLayout}'s business; this only
 * decides what each row says and how the body measures.
 */
public final class CardWrap {
	private CardWrap() {
	}

	/** One wrapped row: its text, where the description starts, the style it draws with, and how far it indents. */
	public record TextRow(String text, int descStart, ClassPresentation.LineStyle style, int indent) {
	}

	/** Wrapped body rows plus the scroll layout they imply: measured once per frame, drawn from the result. */
	public record Measured(List<TextRow> rows, CardLayout.BodyLayout layout) {
	}

	/**
	 * Where the description starts in a wrapped row: the name length on row one,
	 * zero after (continuation rows are pure description). Pure ints, pinned by test.
	 */
	public static int descStartFor(int rowIndex, int rowLength, int prefixLength) {
		return rowIndex == 0 ? Math.min(rowLength, prefixLength) : 0;
	}

	/**
	 * Greedy word wrap over the full line, tracking the name/desc split by character
	 * offset so tone follows meaning, not row number. Names always fit row one.
	 */
	public static List<TextRow> wrapRows(ClassPresentation.CardLine line, int wrapW, ToIntFunction<String> width) {
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
	public static Measured measure(CardLayout.CardRect card, ClassPresentation.CardModel model,
			ToIntFunction<String> width) {
		List<TextRow> rows = new ArrayList<>();
		int wrapW = CardLayout.innerWidth(card);
		for (ClassPresentation.CardLine line : model.text().lines()) {
			rows.addAll(wrapRows(line, wrapW, width));
		}
		return new Measured(List.copyOf(rows), CardLayout.layoutFor(card, rows.size()));
	}
}
