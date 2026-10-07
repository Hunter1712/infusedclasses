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

	/**
	 * The first passive on the given trigger, or null. The single scan for trigger-found
	 * passives: decisions and adapters resolve through here, never by list position, so
	 * reordering passives cannot silently spend or credit the wrong one.
	 */
	public ConditionalPassive passiveForTrigger(String trigger) {
		for (ConditionalPassive passive : passives()) {
			if (passive.trigger().equals(trigger)) {
				return passive;
			}
		}
		return null;
	}
}

