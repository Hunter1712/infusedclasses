package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/** A character class: id, display name, passives, actives with their copy, crit rule. */
public record ClassDefinition(
		Identifier id,
		String displayName,
		List<ConditionalPassive> passives,
		List<HpScaledPassive> hpPassives,
		int accentColor,
		String tagline,
		List<ActiveAbility> actives,
		List<FlipBuff> flipBuffs,
		CritRule critRule
) {
	/** One playable active on a card: its ability id plus its card copy. Names never join with "+". */
	public record ActiveAbility(Identifier id, String name, String desc) {
	}

	/** One flip buff on a card: display copy only, never gated. */
	public record FlipBuff(String name, String desc) {
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
	 * Whether this class carries the ability: the single seam for ability-id checks,
	 * reading the active rows so a gated ability and its card copy cannot drift apart.
	 * Null-safe on the id; a missing class never carries.
	 */
	public boolean hasAbility(Identifier ability) {
		if (ability == null) {
			return false;
		}
		for (ActiveAbility active : actives()) {
			if (active.id().equals(ability)) {
				return true;
			}
		}
		return false;
	}
}

