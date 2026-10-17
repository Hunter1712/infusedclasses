package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.client.HudConfig.Corner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client config's pure parse half: every input shape, good or bad, resolves to a config.
 * The adapter owns the disk; this pins what the file's text means.
 */
class HudConfigTest {
	@Test
	void emptyAndGarbageFallBackToDefaults() {
		assertEquals(HudConfig.defaults(), HudConfig.parse(null));
		assertEquals(HudConfig.defaults(), HudConfig.parse(""));
		assertEquals(HudConfig.defaults(), HudConfig.parse("   "));
		assertEquals(HudConfig.defaults(), HudConfig.parse("not json"));
		assertEquals(HudConfig.defaults(), HudConfig.parse("[]"));
		assertEquals(HudConfig.defaults(), HudConfig.parse("42"));
	}

	@Test
	void cornersParseByFileValue() {
		assertEquals(Corner.BOTTOM_LEFT,
				HudConfig.parse("{\"hud\":{\"corner\":\"bottom_left\"}}").corner());
		assertEquals(Corner.BOTTOM_RIGHT,
				HudConfig.parse("{\"hud\":{\"corner\":\"bottom_right\"}}").corner());
	}

	@Test
	void unknownCornerFallsBackAlone() {
		HudConfig config = HudConfig.parse("{\"hud\":{\"enabled\":false,\"corner\":\"top\"}}");

		assertFalse(config.enabled());
		assertEquals(Corner.BOTTOM_RIGHT, config.corner());
	}

	@Test
	void wrongTypedFieldsFallBackAlone() {
		HudConfig badEnabled = HudConfig.parse("{\"hud\":{\"enabled\":\"yes\",\"corner\":\"bottom_left\"}}");
		assertTrue(badEnabled.enabled());
		assertEquals(Corner.BOTTOM_LEFT, badEnabled.corner());

		HudConfig badCorner = HudConfig.parse("{\"hud\":{\"enabled\":false,\"corner\":5}}");
		assertFalse(badCorner.enabled());
		assertEquals(Corner.BOTTOM_RIGHT, badCorner.corner());

		HudConfig badHud = HudConfig.parse("{\"hud\":\"bottom_left\"}");
		assertEquals(HudConfig.defaults(), badHud);
	}

	@Test
	void unknownKeysAreIgnored() {
		HudConfig config = HudConfig.parse("{\"other\":1,\"hud\":{\"enabled\":false,\"wat\":true}}");

		assertFalse(config.enabled());
		assertEquals(Corner.BOTTOM_RIGHT, config.corner());
	}

	@Test
	void partialHudKeepsTheOtherDefault() {
		HudConfig config = HudConfig.parse("{\"hud\":{\"corner\":\"bottom_left\"}}");

		assertTrue(config.enabled());
		assertEquals(Corner.BOTTOM_LEFT, config.corner());
	}

	@Test
	void defaultsOmitNothingWhenWritten() {
		HudConfig file = HudConfig.parse("{\"hud\":{}}");

		assertEquals(HudConfig.defaults(), file);
	}

	@Test
	void writtenFileRoundTripsThroughParse() {
		assertEquals(HudConfig.defaults(), HudConfig.parse(HudConfig.defaults().toJson()));
		assertEquals(new HudConfig(false, Corner.BOTTOM_LEFT),
				HudConfig.parse(new HudConfig(false, Corner.BOTTOM_LEFT).toJson()));
	}

	@Test
	void everyKindOfDefaultingIsAFallbackForTheFileAdapter() {
		// Wholly-defaulted shapes: empty, garbage, unknown corner.
		assertTrue(HudConfig.parseDetail("").fellBack());
		assertTrue(HudConfig.parseDetail("garbage").fellBack());
		assertTrue(HudConfig.parseDetail("{\"hud\":{\"enabled\":true,\"corner\":\"above\"}}").fellBack());
		// A single wrong-typed field defaults alone; a clean file never defaults.
		assertTrue(HudConfig.parseDetail("{\"hud\":{\"enabled\":\"on\",\"corner\":\"bottom_left\"}}").fellBack());
		assertFalse(HudConfig.parseDetail("{\"hud\":{\"enabled\":false,\"corner\":\"bottom_left\"}}").fellBack());
	}
}
