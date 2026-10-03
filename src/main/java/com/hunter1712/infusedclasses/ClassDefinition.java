package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/** A character class: id, display name, playable yet, passive tweaks, active ability id. */
public record ClassDefinition(
		Identifier id,
		String displayName,
		boolean locked,
		List<Passive> passives,
		Identifier activeAbility
) {
}

