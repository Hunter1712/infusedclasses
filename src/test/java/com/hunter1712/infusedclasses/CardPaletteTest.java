package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The accent-derived card tones, pinned as rules — the legibility floor, the tint
 * direction, the dimming order — rather than as hexes, so tuning a knob does not
 * rewrite the tests.
 */
class CardPaletteTest {
	private static final int JUGGERNAUT = 0xFFE74C3C;
	private static final int LEGIONARY = 0xFFB87333;
	private static final int GAMBLER = 0xFFD4AF37;
	private static final int[] ACCENTS = {JUGGERNAUT, LEGIONARY, GAMBLER};

	@Test
	void accentTextKeepsLegibleAccentsAndLightensDarkOnes() {
		// Gold already reads on the dark body, so it stays itself.
		assertEquals(GAMBLER, CardPalette.accentText(GAMBLER));
		for (int accent : new int[] {JUGGERNAUT, LEGIONARY}) {
			int text = CardPalette.accentText(accent);
			assertNotEquals(accent, text, "a dark accent is lightened, not kept");
			assertTrue(CardPalette.luminance(text) >= 0.55, "the legibility floor holds");
		}
	}

	@Test
	void theBodyTakesAWhisperOfTheAccent() {
		for (int accent : ACCENTS) {
			assertEquals(0xE0, CardPalette.body(accent) >>> 24);
			assertNotEquals(0x141414, CardPalette.body(accent) & 0xFFFFFF, "the tint is not a no-op");
		}
		assertNotEquals(CardPalette.body(JUGGERNAUT), CardPalette.body(GAMBLER));
	}

	@Test
	void checkpointRowsSitBelowTheLegibleAccent() {
		for (int accent : ACCENTS) {
			assertTrue(CardPalette.luminance(CardPalette.tierText(accent))
					< CardPalette.luminance(CardPalette.accentText(accent)));
		}
	}

	@Test
	void descriptionsShareOneSubtleNeutral() {
		assertEquals(0xFF8C8C8C, CardPalette.descText());
		// Every item name takes the accent, so the shared desc tone must sit below
		// even the dimmest legible accent name.
		for (int accent : ACCENTS) {
			assertTrue(CardPalette.luminance(CardPalette.descText())
					< CardPalette.luminance(CardPalette.accentText(accent)));
		}
	}

	@Test
	void bordersFollowTheAccent() {
		for (int accent : ACCENTS) {
			assertTrue(CardPalette.luminance(CardPalette.borderPlain(accent))
					< CardPalette.luminance(CardPalette.borderHover(accent)));
		}
		assertNotEquals(CardPalette.borderPlain(JUGGERNAUT), CardPalette.borderPlain(GAMBLER));
	}

	@Test
	void theTaglineKeepsItsWarmToneWithAClassNudge() {
		for (int accent : ACCENTS) {
			assertEquals(0xFF, CardPalette.tagline(accent) >>> 24);
		}
		assertNotEquals(CardPalette.tagline(JUGGERNAUT), CardPalette.tagline(GAMBLER));
	}
}
