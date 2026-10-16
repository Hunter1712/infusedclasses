package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/** A character class: id, display name, playable yet, passives, ability ids, picker presentation, crit rule. */
public record ClassDefinition(
		Identifier id,
		String displayName,
		boolean locked,
		List<ConditionalPassive> passives,
		List<HpScaledPassive> hpPassives,
		List<Identifier> activeAbilities,
		int accentColor,
		String tagline,
		List<ActiveAbility> actives,
		List<ActiveAbility> flipBuffs,
		CritRule critRule
) {
	/** One active on a card: its detail line plus its full description. Names never join with "+". */
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

	/**
	 * Whether this class carries the ability: the single seam for ability-id checks, so
	 * decisions never read the raw list. Null-safe on the id; a missing class never carries.
	 */
	public boolean hasAbility(Identifier ability) {
		return ability != null && activeAbilities().contains(ability);
	}
}

