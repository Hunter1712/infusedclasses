package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure presentation for class cards and tooltips. One seam for all renderers.
 */
public final class ClassPresentation {
	private ClassPresentation() {
	}

	/** One rendered card line plus which section it belongs to. */
	public enum LineKind {
		PASSIVE_HEADER, PASSIVE, ABILITY_HEADER, ABILITY, NOTICE
	}

	public record CardLine(String text, LineKind kind) {
	}

	/** Picker subtitle under the title. */
	public static String subtitle() {
		return "Your choice shapes your ability";
	}

	/** Tooltip for the select button: concise passive/active summary. */
	public static String tooltipFor(ClassDefinition def) {
		if (def.locked()) {
			return "Coming soon";
		}
		StringBuilder sb = new StringBuilder();
		if (!def.passives().isEmpty()) {
			for (ConditionalPassive p : def.passives()) {
				sb.append(p.name()).append(": ").append(p.fullDesc()).append(" (max ").append(p.maxStacks()).append(" stacks). ");
			}
		}
		if (def.activeAbility() != null) {
			// First ability line is the name (may include keybind); strip "(X)" suffix
			String raw = def.abilityLines().getFirst();
			String clean = raw.replaceAll("\\s*\\([^)]+\\)\\s*$", "").trim();
			sb.append("Active: ").append(clean);
			// Dedicated ability description field
			if (def.abilityDesc() != null && !def.abilityDesc().isBlank()) {
				sb.append(" — ").append(def.abilityDesc());
			}
			sb.append(". ");
		}
		sb.append("Click to choose ").append(def.displayName()).append(".");
		return sb.toString();
	}

	/** Lines shown in the card body: Passives header + names, then Active header + ability names. */
	public static List<CardLine> cardLinesFor(ClassDefinition def) {
		List<CardLine> lines = new ArrayList<>();
		if (def.locked()) {
			lines.add(new CardLine("Passives: ???", LineKind.PASSIVE));
			lines.add(new CardLine("Active: ???", LineKind.ABILITY));
			lines.add(new CardLine("COMING SOON", LineKind.NOTICE));
			return lines;
		}
		if (!def.passives().isEmpty()) {
			lines.add(new CardLine("Passives", LineKind.PASSIVE_HEADER));
			for (ConditionalPassive p : def.passives()) {
				lines.add(new CardLine("  " + p.name(), LineKind.PASSIVE));
			}
		}
		if (def.activeAbility() != null) {
			lines.add(new CardLine("Active", LineKind.ABILITY_HEADER));
			// First ability line is the name (may include keybind); strip "(X)" suffix
			String raw = def.abilityLines().getFirst();
			String clean = raw.replaceAll("\\s*\\([^)]+\\)\\s*$", "").trim();
			lines.add(new CardLine("  " + clean, LineKind.ABILITY));
		}
		return lines;
	}

	/** Accent color for card top strip (grey for locked). */
	public static int accentFor(ClassDefinition def) {
		return def.locked() ? 0xFF555555 : def.accentColor();
	}

	/** Button label and active state. */
	public static ButtonData buttonFor(ClassDefinition def) {
		return new ButtonData(def.locked() ? "Locked" : "Select", !def.locked());
	}

	public record ButtonData(String label, boolean active) {
	}
}
