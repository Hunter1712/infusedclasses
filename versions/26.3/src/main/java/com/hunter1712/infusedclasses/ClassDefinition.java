package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/** A character class: id, display name, passives (earned and always-on), actives with their copy, crit rule. */
public record ClassDefinition(
		Identifier id,
		String displayName,
		List<ConditionalPassive> passives,
		List<HpScaledPassive> hpPassives,
		List<AlwaysOn> alwaysOn,
		int accentColor,
		String tagline,
		List<ActiveAbility> actives,
		List<FlipBuff> flipBuffs,
		CritRule critRule
) {
	/** One playable active on a card: its ability id plus its card copy. Names never join with "+". */
	public record ActiveAbility(Identifier id, String name, String desc) {
	}

	/**
	 * One always-on passive: a chassis row that is written while the class is held and
	 * never earned, with the modifier list the applier writes and the melee deal mult
	 * the plan reads. Plain data like the others, so the card copy, the tick, the switch
	 * cleanup and the melee plan read one source.
	 */
	public record AlwaysOn(String name, String desc, List<PassiveEffect.Modifier> modifiers, double dealMult) {
		/** A chassis with no melee mult: the common row. */
		public AlwaysOn(String name, String desc, List<PassiveEffect.Modifier> modifiers) {
			this(name, desc, modifiers, 1.0);
		}
	}

	/** One flip buff on a card: display copy only, never gated. */
	public record FlipBuff(String name, String desc) {
	}

	/** A kit with no always-on rows: nothing to write while classed. */
	public ClassDefinition(Identifier id, String displayName, List<ConditionalPassive> passives,
			List<HpScaledPassive> hpPassives, int accentColor, String tagline,
			List<ActiveAbility> actives, List<FlipBuff> flipBuffs, CritRule critRule) {
		this(id, displayName, passives, hpPassives, List.of(), accentColor, tagline, actives, flipBuffs, critRule);
	}

	/**
	 * The first passive whose earns name the given trigger, or null. The single scan for
	 * trigger-fed passives: decisions and adapters resolve through here, never by list
	 * position, so reordering passives cannot silently spend or credit the wrong one.
	 */
	public ConditionalPassive passiveForTrigger(Trigger trigger) {
		for (ConditionalPassive passive : passives()) {
			if (passive.earnsOn(trigger)) {
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

	/** The product of every always-on row's melee deal mult: 1.0 with none. */
	public double chassisDealMult() {
		double mult = 1.0;
		for (AlwaysOn row : alwaysOn()) {
			mult *= row.dealMult();
		}
		return mult;
	}
}
