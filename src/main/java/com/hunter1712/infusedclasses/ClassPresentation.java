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

	/** Tooltip for the select button: locked teaser or joined ability lines. */
	public static String tooltipFor(ClassDefinition def) {
		if (def.locked()) {
			return "Coming soon";
		}
		return String.join(" — ", def.abilityLines()) + ". Click to choose " + def.displayName() + ".";
	}

	/** Lines shown in the card body: passives (or placeholder) then ability lines. */
	public static List<String> cardLinesFor(ClassDefinition def) {
		List<String> lines = new ArrayList<>();
		if (def.locked()) {
			lines.add("Passives: ???");
			lines.add("Ability: ???");
			lines.add("COMING SOON");
			return lines;
		}
		for (Passive p : def.passives()) {
			lines.add(ClassAttributes.display(p));
		}
		if (lines.isEmpty()) {
			lines.add("No passives yet");
		}
		if (def.activeAbility() == null) {
			lines.add("Ability: —");
		} else {
			lines.addAll(def.abilityLines());
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