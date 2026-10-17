package com.hunter1712.infusedclasses.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * The client config's contents: the two player-editable knobs, parsed from the config file's
 * text. Pure: {@link #parse} never throws and falls back per field, so a hand-edited or broken
 * file cannot stop the client. {@link HudConfigFile} owns the disk; the status HUD reads the
 * loaded result. Only fields the player can set live here — no tuning numbers.
 */
public record HudConfig(boolean enabled, Corner corner) {
	/** Which screen corner the status strip anchors to; these are the file's spellings. */
	public enum Corner {
		BOTTOM_RIGHT("bottom_right"),
		BOTTOM_LEFT("bottom_left");

		private final String fileValue;

		Corner(String fileValue) {
			this.fileValue = fileValue;
		}

		/** The value this corner is spelled as in the config file. */
		public String fileValue() {
			return fileValue;
		}

		private static Corner fromFileValue(String value) {
			for (Corner corner : values()) {
				if (corner.fileValue.equals(value)) {
					return corner;
				}
			}
			return null;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** The file's defaults: HUD on, anchored bottom right. */
	public static HudConfig defaults() {
		return new HudConfig(true, Corner.BOTTOM_RIGHT);
	}

	/** One parse's details: the config it lands on, and whether any file content defaulted. */
	record Parsed(HudConfig config, boolean fellBack) {
	}

	/**
	 * Parses the config file's text. Empty, garbage and non-object roots yield the defaults;
	 * inside {@code hud} each field falls back alone when missing or wrongly typed; unknown
	 * keys and unknown corner values are ignored. Never throws.
	 */
	public static HudConfig parse(String json) {
		return parseDetail(json).config();
	}

	/** {@link #parse} plus whether anything defaulted: the file adapter's one log line reads it. */
	static Parsed parseDetail(String json) {
		HudConfig defaults = defaults();
		if (json == null || json.isBlank()) {
			return new Parsed(defaults, true);
		}
		JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (RuntimeException e) {
			return new Parsed(defaults, true);
		}
		if (!root.isJsonObject()) {
			return new Parsed(defaults, true);
		}
		JsonElement hud = root.getAsJsonObject().get("hud");
		if (hud == null || !hud.isJsonObject()) {
			return new Parsed(defaults, true);
		}
		JsonObject hudObject = hud.getAsJsonObject();
		boolean enabled = booleanOr(hudObject, "enabled", defaults.enabled());
		Corner corner = cornerOr(hudObject, "corner", defaults.corner());
		boolean fellBack = !booleanApplied(hudObject, "enabled") || !cornerApplied(hudObject, "corner");
		return new Parsed(new HudConfig(enabled, corner), fellBack);
	}

	private static boolean booleanApplied(JsonObject object, String key) {
		return object.get(key) instanceof JsonPrimitive primitive && primitive.isBoolean();
	}

	private static boolean cornerApplied(JsonObject object, String key) {
		return object.get(key) instanceof JsonPrimitive primitive && primitive.isString()
				&& Corner.fromFileValue(primitive.getAsString()) != null;
	}

	private static boolean booleanOr(JsonObject object, String key, boolean fallback) {
		JsonElement value = object.get(key);
		if (value instanceof JsonPrimitive primitive && primitive.isBoolean()) {
			return primitive.getAsBoolean();
		}
		return fallback;
	}

	private static Corner cornerOr(JsonObject object, String key, Corner fallback) {
		JsonElement value = object.get(key);
		if (value instanceof JsonPrimitive primitive && primitive.isString()) {
			Corner corner = Corner.fromFileValue(primitive.getAsString());
			if (corner != null) {
				return corner;
			}
		}
		return fallback;
	}

	/** The canonical file text, written on first run; {@code parse(toJson())} is this config back. */
	public String toJson() {
		JsonObject hud = new JsonObject();
		hud.addProperty("enabled", enabled);
		hud.addProperty("corner", corner.fileValue());
		JsonObject root = new JsonObject();
		root.add("hud", hud);
		return GSON.toJson(root) + System.lineSeparator();
	}
}
