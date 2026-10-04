package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One seam for attribute knowledge: gameplay resolution plus display naming
 * from a single table. Pure queries stay off the vanilla registry so tests
 * assert without game bootstrap; only resolve touches live holders.
 */
public final class ClassAttributes {
	private static final Map<String, AttributeInfo> TABLE = Map.of(
			"max_health", new AttributeInfo("Max Health", () -> Attributes.MAX_HEALTH),
			"armor", new AttributeInfo("Armor", () -> Attributes.ARMOR),
			"armor_toughness", new AttributeInfo("Toughness", () -> Attributes.ARMOR_TOUGHNESS),
			"movement_speed", new AttributeInfo("Move Speed", () -> Attributes.MOVEMENT_SPEED),
			"attack_damage", new AttributeInfo("Attack Damage", () -> Attributes.ATTACK_DAMAGE),
			"attack_speed", new AttributeInfo("Attack Speed", () -> Attributes.ATTACK_SPEED),
			"knockback_resistance", new AttributeInfo("Knockback Resist", () -> Attributes.KNOCKBACK_RESISTANCE));

	private record AttributeInfo(String displayName, Supplier<Holder<Attribute>> holderSupplier) {
	}

	private ClassAttributes() {
	}

	/** Pure: true when this mod knows how to resolve and display the attribute. */
	public static boolean isKnown(Identifier id) {
		return id != null && "minecraft".equals(id.getNamespace()) && TABLE.containsKey(id.getPath());
	}

	/** Pure display name, prettified fallback for future attributes. */
	public static String displayName(Identifier attribute) {
		if (attribute == null) {
			return "";
		}
		AttributeInfo info = TABLE.get(attribute.getPath());
		return info != null ? info.displayName() : prettify(attribute.getPath());
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
		AttributeInfo info = TABLE.get(id.getPath());
		return info != null ? info.holderSupplier().get() : null;
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
