package com.hunter1712.infusedclasses;

/**
 * Bind morph decisions: what a reused slot bind means for a class. Every class reuses
 * the two slot binds so binds survive the class: the signature slot arrives as the
 * rotate intent for Legionaries, the roulette intent for Gamblers, Rush for everyone
 * else; the ultimate slot arrives as the oath ultimate for Legionaries, All In for
 * Gamblers, Rend for everyone else. The intent receivers wire, this decides; decisions
 * and applies stay pure/adapter.
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

	/** What the signature slot means: the wheel for Gamblers, the rotate intent for Legionaries, Rush for everyone else. */
	public static SignatureTarget signatureFor(ClassDefinition def) {
		if (def == null) {
			return SignatureTarget.RUSH;
		}
		if (def.hasAbility(ClassRegistry.ROULETTE_ABILITY)) {
			return SignatureTarget.ROULETTE;
		}
		if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return SignatureTarget.ROTATE;
		}
		return SignatureTarget.RUSH;
	}

	/** What the ultimate slot means: All In for Gamblers, the oath ultimate for Legionaries, Rend for everyone else. */
	public static UltimateTarget ultimateFor(ClassDefinition def) {
		if (def == null) {
			return UltimateTarget.REND;
		}
		if (def.hasAbility(ClassRegistry.ALL_IN_ABILITY)) {
			return UltimateTarget.ALL_IN;
		}
		if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return UltimateTarget.AEQUILIBRIUM;
		}
		return UltimateTarget.REND;
	}
}
