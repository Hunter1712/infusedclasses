package com.hunter1712.infusedclasses;

/**
 * Bind morph decisions: what a reused bind means for a class. Every class reuses the
 * Juggernaut binds so binds survive the class: {@code R} arrives as the rotate intent
 * for Legionaries, the roulette intent for Gamblers, Rush for everyone else; {@code G}
 * arrives as the oath ultimate for Legionaries, All In for Gamblers, Rend for everyone
 * else. The intent receivers wire, this decides; decisions and applies stay pure/adapter.
 */
public final class BindMorph {
	/** What {@code R} means once the class is known. */
	public enum RushTarget {
		RUSH,
		ROTATE,
		ROULETTE
	}

	/** What {@code G} means once the class is known. One {@code G}, one ultimate per class. */
	public enum OathTarget {
		REND,
		AEQUILIBRIUM,
		ALL_IN
	}

	private BindMorph() {
	}

	/** What {@code R} means: the wheel for Gamblers, the rotate intent for Legionaries, Rush for everyone else. */
	public static RushTarget rushFor(ClassDefinition def) {
		if (def == null) {
			return RushTarget.RUSH;
		}
		if (def.hasAbility(ClassRegistry.ROULETTE_ABILITY)) {
			return RushTarget.ROULETTE;
		}
		if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return RushTarget.ROTATE;
		}
		return RushTarget.RUSH;
	}

	/** What {@code G} means: All In for Gamblers, the oath ultimate for Legionaries, Rend for everyone else. */
	public static OathTarget oathFor(ClassDefinition def) {
		if (def == null) {
			return OathTarget.REND;
		}
		if (def.hasAbility(ClassRegistry.ALL_IN_ABILITY)) {
			return OathTarget.ALL_IN;
		}
		if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return OathTarget.AEQUILIBRIUM;
		}
		return OathTarget.REND;
	}
}
