package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Locale;
import java.util.Map;

/**
 * One seam for attribute knowledge: gameplay resolution plus display naming
 * from a single table. Pure queries stay off the vanilla registry so tests
 * assert without game bootstrap; only resolve touches live holders.
 */
public final class ClassAttributes {
	private static final Map<String, String> DISPLAY = Map.of(
			"max_health", "Max Health",
			"armor", "Armor",
			"armor_toughness", "Toughness",
			"movement_speed", "Move Speed",
			"attack_damage", "Attack Damage",
			"attack_speed", "Attack Speed",
			"knockback_resistance", "Knockback Resist");

	private ClassAttributes() {
	}

	/** Pure: true when this mod knows how to resolve and display the attribute. */
	public static boolean isKnown(Identifier id) {
		return id != null && "minecraft".equals(id.getNamespace()) && DISPLAY.containsKey(id.getPath());
	}

	/** Pure display name, prettified fallback for future attributes. */
	public static String displayName(Identifier attribute) {
		if (attribute == null) {
			return "";
		}
		String known = DISPLAY.get(attribute.getPath());
		return known != null ? known : prettify(attribute.getPath());
	}

	/** Pure passive line, e.g. "+8 Max Health" or "-10% Move Speed". */
	public static String display(Passive passive) {
		String label = displayName(passive.attribute());
		return switch (passive.operation()) {
			case ADD_MULTIPLIED_TOTAL, ADD_MULTIPLIED_BASE ->
					formatPercent(passive.value()) + " " + label;
			default -> formatFlat(passive.value()) + " " + label;
		};
	}

	/** Live holder for effect application. Null for unknown. Touches vanilla holders. */
	public static Holder<Attribute> resolve(Identifier id) {
		if (!isKnown(id)) {
			return null;
		}
		return switch (id.getPath()) {
			case "max_health" -> Attributes.MAX_HEALTH;
			case "armor" -> Attributes.ARMOR;
			case "armor_toughness" -> Attributes.ARMOR_TOUGHNESS;
			case "movement_speed" -> Attributes.MOVEMENT_SPEED;
			case "attack_damage" -> Attributes.ATTACK_DAMAGE;
			case "attack_speed" -> Attributes.ATTACK_SPEED;
			case "knockback_resistance" -> Attributes.KNOCKBACK_RESISTANCE;
			default -> null;
		};
	}

	public static String prettify(String path) {
		String[] parts = path.split("_");
		StringBuilder out = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			if (!out.isEmpty()) {
				out.append(' ');
			}
			out.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				out.append(part.substring(1));
			}
		}
		return out.toString();
	}

	private static String formatFlat(double value) {
		String num = value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
		return value > 0 ? "+" + num : num;
	}

	private static String formatPercent(double value) {
		double pct = value * 100.0;
		String num = pct == Math.rint(pct) ? String.valueOf((long) pct) : String.format(Locale.ROOT, "%.1f", pct);
		return (value > 0 ? "+" : "") + num + "%";
	}
}
