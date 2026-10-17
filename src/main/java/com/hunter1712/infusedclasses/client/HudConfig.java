package com.hunter1712.infusedclasses.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * The client config's contents: the three player-editable knobs, parsed from the config
 * file's text. Pure: {@link #parse} never throws and falls back per field, so a hand-edited
 * or broken file cannot stop the client. {@link HudConfigFile} owns the disk; the status
 * HUD reads the loaded result. Only fields the player can set live here — no tuning numbers.
 * The corner names screen zones now: the old hotbar spellings carry their side in parse, so
 * pre-zone files keep parsing quietly.
 */
public record HudConfig(boolean enabled, Corner corner, float scale) {
	/**
	 * Which screen zone the status strip occupies; these are the file's spellings. The
	 * default zone is the screen's left edge, vertically centered — the right zone mirrors
	 * it. Older files' {@code bottom_left}/{@code bottom_right} (the retired hotbar anchor)
	 * still parse and carry their side, so an upgrade never logs or flips a file.
	 */
	public enum Corner {
		LEFT("left"),
		RIGHT("right");

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
			// The old hotbar-side spellings of pre-zone files: their sides carry over.
			return switch (value) {
				case "bottom_left", "center_left" -> LEFT;
				case "bottom_right", "center_right" -> RIGHT;
				default -> null;
			};
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	/** The strip's own extra scale, outside the gui scale: clipped here, so an absurd file cannot draw a wall of letters. */
	private static final float SCALE_MIN = 0.5F;
	private static final float SCALE_MAX = 6.0F;
	private static final float SCALE_DEFAULT = 1.0F;

	/** The file's defaults: HUD on, in the screen's left-center zone, native size. */
	public static HudConfig defaults() {
		return new HudConfig(true, Corner.LEFT, SCALE_DEFAULT);
	}

	/** One parse's details: the config it lands on, and whether any file content defaulted. */
	record Parsed(HudConfig config, boolean fellBack) {
	}

	/**
	 * Parses the config file's text. Empty, garbage and non-object roots yield the defaults;
	 * inside {@code hud} each field falls back alone when missing or wrongly typed; unknown
	 * keys and unknown corner values are ignored; an off-bounds scale clamps instead of
	 * defaulting (the player's intent was a number). Never throws.
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
		float scale = scaleOr(hudObject, "scale", defaults.scale());
		// A knob the file never mentions is not a fallback — old files carry only old knobs,
		// and the adapter logs once per startup, so absence must never turn into noise.
		boolean fellBack = !booleanApplied(hudObject, "enabled") || !cornerApplied(hudObject, "corner")
				|| wronglyTyped(hudObject, "scale");
		return new Parsed(new HudConfig(enabled, corner, scale), fellBack);
	}

	private static boolean booleanApplied(JsonObject object, String key) {
		return object.get(key) instanceof JsonPrimitive primitive && primitive.isBoolean();
	}

	private static boolean cornerApplied(JsonObject object, String key) {
		return object.get(key) instanceof JsonPrimitive primitive && primitive.isString()
				&& Corner.fromFileValue(primitive.getAsString()) != null;
	}

	private static boolean wronglyTyped(JsonObject object, String key) {
		JsonElement value = object.get(key);
		return value != null && !(value instanceof JsonPrimitive primitive && primitive.isNumber());
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

	private static float scaleOr(JsonObject object, String key, float fallback) {
		JsonElement value = object.get(key);
		if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
			return Math.clamp(primitive.getAsFloat(), SCALE_MIN, SCALE_MAX);
		}
		return fallback;
	}

	/** The canonical file text, written on first run; {@code parse(toJson())} is this config back. */
	public String toJson() {
		JsonObject hud = new JsonObject();
		hud.addProperty("enabled", enabled);
		hud.addProperty("corner", corner.fileValue());
		hud.addProperty("scale", scale);
		JsonObject root = new JsonObject();
		root.add("hud", hud);
		return GSON.toJson(root) + System.lineSeparator();
	}
}
