package com.hunter1712.infusedclasses;

/**
 * Bind morph decisions: what a reused slot bind means for a class. Every class reuses the two
 * slot binds so binds survive the class: the signature slot arrives as the rotate intent for
 * Legionaries, the roulette intent for Gamblers, Offload for everyone else; the ultimate slot
 * arrives as the oath ultimate for Legionaries, All In for Gamblers, Onslaught for everyone
 * else. One walk of the class's actives picks the roster row whose {@link AbilityRoster.Slot}
 * matches, so the same row answers what the slot fires and what the stack draws: a further
 * ability adds a row, not a branch, and the stack can never draw one ultimate while the key
 * fires another.
 *
 * <p>The printed fallbacks (Offload, Onslaught) keep both keys answering for a classless
 * player and for a class carrying neither slot's ability; the drawn rows stay null there,
 * because there is no lane to draw. The intent receivers wire, this decides; decisions and
 * applies stay pure/adapter (ADR-0001).
 */
public final class BindMorph {
	/**
	 * The class's shape: the row each slot draws, null when the class does not carry that
	 * slot's ability. The two halves cannot disagree, and a class carrying neither slot
	 * ability draws no slot lines.
	 */
	public record Shape(AbilityRoster.Row signatureRow, AbilityRoster.Row ultimateRow) {
	}

	private BindMorph() {
	}

	/**
	 * The row a slot fires: the class's first active in that slot, the printed fallback
	 * (Offload, Onslaught) for a classless player or a class carrying neither.
	 */
	public static AbilityRoster.Row slotFor(ClassDefinition def, AbilityRoster.Slot slot) {
		if (def != null) {
			for (ClassDefinition.ActiveAbility active : def.actives()) {
				AbilityRoster.Row row = AbilityRoster.byAbility(active.id());
				if (row != null && row.slot() == slot) {
					return row;
				}
			}
		}
		return slot == AbilityRoster.Slot.SIGNATURE ? AbilityRoster.OFFLOAD : AbilityRoster.ONSLAUGHT;
	}

	/**
	 * The class's shape: the row each slot fires, drawn only when the class carries that
	 * ability. The HUD's slot lines and the keys' fires are this one value.
	 */
	public static Shape shape(ClassDefinition def) {
		return new Shape(drawn(def, slotFor(def, AbilityRoster.Slot.SIGNATURE)),
				drawn(def, slotFor(def, AbilityRoster.Slot.ULTIMATE)));
	}

	/** The row to draw for a slot: its fired row, null when the class does not carry that ability. */
	private static AbilityRoster.Row drawn(ClassDefinition def, AbilityRoster.Row fired) {
		return def != null && def.hasAbility(fired.ability()) ? fired : null;
	}
}
