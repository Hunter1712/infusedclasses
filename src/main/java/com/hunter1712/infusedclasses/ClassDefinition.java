package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/** A character class: id, display name, playable yet, passive tweaks, active ability id, picker presentation. */
public record ClassDefinition(
		Identifier id,
		String displayName,
		boolean locked,
		List<Passive> passives,
		Identifier activeAbility,
		int accentColor,
		String tagline,
		List<String> abilityLines
) {
	/** Picker tooltip: locked teasers stay generic, playable joins ability lines. */
	public String abilityBlurb() {
		if (locked) {
			return "Coming soon";
		}
		return String.join(" — ", abilityLines) + ". Click to choose " + displayName + ".";
	}
}

