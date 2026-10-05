package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/** A character class: id, display name, playable yet, conditional passives, active ability id, picker presentation. */
public record ClassDefinition(
		Identifier id,
		String displayName,
		boolean locked,
		List<ConditionalPassive> passives,
		Identifier activeAbility,
		int accentColor,
		String tagline,
		String abilityName,
		String abilityDesc
) {
}

