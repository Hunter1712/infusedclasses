package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.client.HudConfig.Corner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client config's pure parse half: every input shape, good or bad, resolves to a config.
 * The adapter owns the disk; this pins what the file's text means — including that the
 * retired hotbar spellings still carry their side without a fallback log.
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
	void zoneSpellingsParseByFileValue() {
		assertEquals(Corner.LEFT, HudConfig.parse("{\"hud\":{\"corner\":\"left\"}}").corner());
		assertEquals(Corner.RIGHT, HudConfig.parse("{\"hud\":{\"corner\":\"right\"}}").corner());
	}

	@Test
	void legacyHotbarSpellingsCarryTheirSide() {
		assertEquals(Corner.LEFT, HudConfig.parse("{\"hud\":{\"corner\":\"bottom_left\"}}").corner());
		assertEquals(Corner.RIGHT, HudConfig.parse("{\"hud\":{\"corner\":\"bottom_right\"}}").corner());
		assertEquals(Corner.LEFT, HudConfig.parse("{\"hud\":{\"corner\":\"center_left\"}}").corner());
		assertEquals(Corner.RIGHT, HudConfig.parse("{\"hud\":{\"corner\":\"center_right\"}}").corner());
		// a carried side is an applied corner, not a fallback: a pre-zone file with
		// both knobs spelled the old way parses quietly
		assertFalse(HudConfig.parseDetail("{\"hud\":{\"enabled\":true,\"corner\":\"bottom_right\"}}").fellBack());
	}

	@Test
	void unknownCornerFallsBackAlone() {
		HudConfig config = HudConfig.parse("{\"hud\":{\"enabled\":false,\"corner\":\"top\"}}");

		assertFalse(config.enabled());
		assertEquals(Corner.LEFT, config.corner());
	}

	@Test
	void wrongTypedFieldsFallBackAlone() {
		HudConfig badEnabled = HudConfig.parse("{\"hud\":{\"enabled\":\"yes\",\"corner\":\"left\"}}");
		assertTrue(badEnabled.enabled());
		assertEquals(Corner.LEFT, badEnabled.corner());

		HudConfig badCorner = HudConfig.parse("{\"hud\":{\"enabled\":false,\"corner\":5}}");
		assertFalse(badCorner.enabled());
		assertEquals(Corner.LEFT, badCorner.corner());

		HudConfig badHud = HudConfig.parse("{\"hud\":\"left\"}");
		assertEquals(HudConfig.defaults(), badHud);
	}

	@Test
	void unknownKeysAreIgnored() {
		HudConfig config = HudConfig.parse("{\"other\":1,\"hud\":{\"enabled\":false,\"wat\":true}}");

		assertFalse(config.enabled());
		assertEquals(Corner.LEFT, config.corner());
	}

	@Test
	void partialHudKeepsTheOtherDefault() {
		HudConfig config = HudConfig.parse("{\"hud\":{\"corner\":\"right\"}}");

		assertTrue(config.enabled());
		assertEquals(Corner.RIGHT, config.corner());
	}

	@Test
	void defaultsOmitNothingWhenWritten() {
		HudConfig file = HudConfig.parse("{\"hud\":{}}");

		assertEquals(HudConfig.defaults(), file);
	}

	@Test
	void writtenFileRoundTripsThroughParse() {
		assertEquals(HudConfig.defaults(), HudConfig.parse(HudConfig.defaults().toJson()));
		assertEquals(new HudConfig(false, Corner.LEFT, 1.0F),
				HudConfig.parse(new HudConfig(false, Corner.LEFT, 1.0F).toJson()));
		assertEquals(new HudConfig(true, Corner.RIGHT, 2.0F),
				HudConfig.parse(new HudConfig(true, Corner.RIGHT, 2.0F).toJson()));
	}

	@Test
	void scalesParseByNumberAndClampOffBounds() {
		assertEquals(2.0F, HudConfig.parse("{\"hud\":{\"scale\":2}}").scale());
		assertEquals(1.5F, HudConfig.parse("{\"hud\":{\"scale\":1.5}}").scale());
		// off-bounds clamps instead of defaulting: the player's intent was a number
		assertEquals(6.0F, HudConfig.parse("{\"hud\":{\"scale\":99}}").scale());
		assertEquals(0.5F, HudConfig.parse("{\"hud\":{\"scale\":0}}").scale());
	}

	@Test
	void wrongTypedScalesFallBackAlone() {
		HudConfig config = HudConfig.parse("{\"hud\":{\"scale\":\"big\",\"corner\":\"left\"}}");

		assertEquals(1.0F, config.scale());
		assertEquals(Corner.LEFT, config.corner());
		assertTrue(config.enabled());
	}

	@Test
	void everyKindOfDefaultingIsAFallbackForTheFileAdapter() {
		// Wholly-defaulted shapes: empty, garbage, unknown corner.
		assertTrue(HudConfig.parseDetail("").fellBack());
		assertTrue(HudConfig.parseDetail("garbage").fellBack());
		assertTrue(HudConfig.parseDetail("{\"hud\":{\"enabled\":true,\"corner\":\"above\"}}").fellBack());
		// A single wrong-typed field defaults alone; a clean file never defaults.
		assertTrue(HudConfig.parseDetail("{\"hud\":{\"enabled\":\"on\",\"corner\":\"left\"}}").fellBack());
		assertFalse(HudConfig.parseDetail("{\"hud\":{\"enabled\":false,\"corner\":\"left\"}}").fellBack());
		// A scale counts as clean however it clamps, beside an otherwise clean file;
		// a wrong-typed scale defaults its field alone.
		assertFalse(HudConfig.parseDetail("{\"hud\":{\"enabled\":true,\"corner\":\"left\",\"scale\":3}}").fellBack());
		assertTrue(HudConfig.parseDetail("{\"hud\":{\"enabled\":true,\"corner\":\"left\",\"scale\":\"3\"}}").fellBack());
	}
}
