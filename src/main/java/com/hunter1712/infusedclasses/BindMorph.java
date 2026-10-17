package com.hunter1712.infusedclasses;

/**
 * Bind morph decisions: what a reused slot bind means for a class. Every class reuses
 * the two slot binds so binds survive the class: the signature slot arrives as the
 * rotate intent for Legionaries, the roulette intent for Gamblers, Rush for everyone
 * else; the ultimate slot arrives as the oath ultimate for Legionaries, All In for
 * Gamblers, Rend for everyone else. The roster rows carry the binds: this walks the
 * class's actives and takes the first row that answers, so a further class adds rows,
 * not branches. The Legionary's ultimate still keys on the ROTATE row (ADR-0009). The
 * intent receivers wire, this decides; decisions and applies stay pure/adapter.
 */
public final class BindMorph {
	/** What the signature slot means once the class is known. */
	public enum SignatureTarget {
		RUSH,
		ROTATE,
		ROULETTE
	}

	/** What the ultimate slot means once the class is known. One ultimate per class. */
	public enum UltimateTarget {
		REND,
		AEQUILIBRIUM,
		ALL_IN
	}

	private BindMorph() {
	}

	/** What the signature slot means: the class's first active whose roster row answers, Rush for everyone else. */
	public static SignatureTarget signatureFor(ClassDefinition def) {
		if (def == null) {
			return SignatureTarget.RUSH;
		}
		for (ClassDefinition.ActiveAbility active : def.actives()) {
			AbilityRoster.Row row = AbilityRoster.byAbility(active.id());
			if (row != null && row.signature() != null) {
				return row.signature();
			}
		}
		return SignatureTarget.RUSH;
	}

	/** What the ultimate slot means: the class's first active whose roster row answers, Rend for everyone else. */
	public static UltimateTarget ultimateFor(ClassDefinition def) {
		if (def == null) {
			return UltimateTarget.REND;
		}
		for (ClassDefinition.ActiveAbility active : def.actives()) {
			AbilityRoster.Row row = AbilityRoster.byAbility(active.id());
			if (row != null && row.ultimate() != null) {
				return row.ultimate();
			}
		}
		return UltimateTarget.REND;
	}
}
