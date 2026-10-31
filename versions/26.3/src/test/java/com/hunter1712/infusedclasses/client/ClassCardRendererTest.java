package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassPresentation;
import com.hunter1712.infusedclasses.ClassRegistry;
import com.hunter1712.infusedclasses.client.CardLayout.CardRect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The card body's wrap and measure, driven by a stub measure (one unit per character) so
 * which words land on which row, where the tone boundary falls and the row count the scroll
 * layout is built from are all assertable without a Font.
 */
class ClassCardRendererTest {
	/** One width unit per character: the stub the screen's {@code font::width} stands in for. */
	private static final ToIntFunction<String> PIXELS = String::length;

	private static ClassPresentation.CardLine line(String name, String desc) {
		return new ClassPresentation.CardLine(name, desc, ClassPresentation.LineKind.ABILITY, 0xFFE74C3C);
	}

	@Test
	void wrapsOnlyAtWordBoundariesAndKeepsEveryWord() {
		ClassPresentation.CardLine rush = line("Rush", "Cleanses harmful effects and quickens you for 5s.");

		List<ClassCardRenderer.TextRow> rows = ClassCardRenderer.wrapRows(rush, 20, PIXELS);

		StringBuilder rejoined = new StringBuilder();
		for (ClassCardRenderer.TextRow row : rows) {
			// A row fits its width, unless it is one word that cannot.
			assertTrue(row.text().length() <= 20 || !row.text().contains(" "), row.text());
			rejoined.append(rejoined.isEmpty() ? row.text() : " " + row.text());
		}
		assertEquals(rush.text(), rejoined.toString(), "no word is split or lost");
		assertTrue(rows.size() > 1, "the line wraps at 20 units");
	}

	@Test
	void toneBoundaryLandsOnRowOneAndZeroAfter() {
		ClassPresentation.CardLine rush = line("Rush", "Cleanses harmful effects and quickens you for 5s.");

		List<ClassCardRenderer.TextRow> rows = ClassCardRenderer.wrapRows(rush, 20, PIXELS);

		assertEquals("Rush \u2014 ".length(), rows.get(0).descStart());
		for (int i = 1; i < rows.size(); i++) {
			assertEquals(0, rows.get(i).descStart(), "row " + i + " is pure description");
		}
	}

	@Test
	void aNameWiderThanTheCardStillStartsRowOne() {
		ClassPresentation.CardLine longName = line("Verylongpassivename", "text after");

		List<ClassCardRenderer.TextRow> rows = ClassCardRenderer.wrapRows(longName, 8, PIXELS);

		// The name overflows alone on row one and draws whole as the name: the tone
		// boundary clamps to the row's end, never past it.
		assertEquals("Verylongpassivename", rows.get(0).text());
		assertEquals(rows.get(0).text().length(), rows.get(0).descStart());
		assertEquals(0, rows.get(1).descStart());
	}

	@Test
	void anEmptyDescriptionStaysOneRow() {
		ClassPresentation.CardLine header = new ClassPresentation.CardLine(
				"Passives", null, ClassPresentation.LineKind.PASSIVE_HEADER, 0xFFE74C3C);

		// Null and blank descriptions are the same line: the name alone, boundary at the end.
		assertEquals(List.of(new ClassCardRenderer.TextRow("Passives", "Passives".length(), header.style(), 0)),
				ClassCardRenderer.wrapRows(header, 4, PIXELS));
		assertEquals(1, ClassCardRenderer.wrapRows(line("Vigor", ""), 4, PIXELS).size());
	}

	@Test
	void checkpointRowsCarryTheirIndent() {
		ClassPresentation.CardLine tier = new ClassPresentation.CardLine(
				"Steady at 15", "text after", ClassPresentation.LineKind.TIER, 0xFFE74C3C);

		List<ClassCardRenderer.TextRow> rows = ClassCardRenderer.wrapRows(tier, 40, PIXELS);

		for (ClassCardRenderer.TextRow row : rows) {
			assertEquals(CardLayout.TIER_INDENT, row.indent(), row.text());
		}
		// Every other kind starts at the body's left edge.
		for (ClassCardRenderer.TextRow row : ClassCardRenderer.wrapRows(line("Rush", "Engage."), 40, PIXELS)) {
			assertEquals(0, row.indent());
		}
	}

	@Test
	void measureCountsEveryWrappedRowForTheScrollLayout() {
		CardRect card = CardLayout.focusedCard(854, 480);
		ClassPresentation.CardModel model =
				ClassPresentation.cardFor(ClassRegistry.byId(ClassRegistry.JUGGERNAUT), false);

		ClassCardRenderer.Measured measured = ClassCardRenderer.measure(card, model, PIXELS);

		int expected = 0;
		for (ClassPresentation.CardLine line : model.text().lines()) {
			expected += ClassCardRenderer.wrapRows(line, CardLayout.innerWidth(card), PIXELS).size();
		}
		assertEquals(expected, measured.rows().size());
		// The scroll layout is built from exactly the rows the draw pass will iterate.
		assertEquals(measured.rows().size() * CardLayout.ROW_H, measured.layout().textH());
	}
}
