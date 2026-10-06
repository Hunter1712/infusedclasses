package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/** A character class: id, display name, playable yet, conditional passives, HP-scaled passives, bonus max health, active ability ids, picker presentation. */
public record ClassDefinition(
		Identifier id,
		String displayName,
		boolean locked,
		List<ConditionalPassive> passives,
		List<HpScaledPassive> hpPassives,
		double maxHealthBonus,
		List<Identifier> activeAbilities,
		int accentColor,
		String tagline,
		List<ActiveAbility> actives
) {
	/** One active on a card: its list line plus its tooltip description. Names never join with "+". */
	public record ActiveAbility(String name, String desc) {
	}
}

