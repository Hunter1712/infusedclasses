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
		PASSIVE, ABILITY, NOTICE
	}

	public record CardLine(String text, LineKind kind) {
	}

	/** Picker subtitle under the title. */
	public static String subtitle() {
		return "Your choice shapes your ability";
	}

	/** Tooltip for the select button: locked teaser or passive texts joined with ability lines. */
	public static String tooltipFor(ClassDefinition def) {
		if (def.locked()) {
			return "Coming soon";
		}
		List<String> parts = new ArrayList<>();
		for (ConditionalPassive p : def.passives()) {
			parts.add(p.name() + ": " + p.fullDesc() + " (max " + p.maxStacks() + " stacks)");
		}
		parts.addAll(def.abilityLines());
		return String.join(" — ", parts) + ". Click to choose " + def.displayName() + ".";
	}

	/** Lines shown in the card body: passive name/short-desc pairs then ability lines. */
	public static List<CardLine> cardLinesFor(ClassDefinition def) {
		List<CardLine> lines = new ArrayList<>();
		if (def.locked()) {
			lines.add(new CardLine("Passives: ???", LineKind.PASSIVE));
			lines.add(new CardLine("Ability: ???", LineKind.ABILITY));
			lines.add(new CardLine("COMING SOON", LineKind.NOTICE));
			return lines;
		}
		for (ConditionalPassive p : def.passives()) {
			lines.add(new CardLine(p.name(), LineKind.PASSIVE));
			lines.add(new CardLine(p.shortDesc(), LineKind.PASSIVE));
		}
		if (def.passives().isEmpty()) {
			lines.add(new CardLine("No passives yet", LineKind.PASSIVE));
		}
		if (def.activeAbility() == null) {
			lines.add(new CardLine("Ability: —", LineKind.ABILITY));
		} else {
			for (String ability : def.abilityLines()) {
				lines.add(new CardLine(ability, LineKind.ABILITY));
			}
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
