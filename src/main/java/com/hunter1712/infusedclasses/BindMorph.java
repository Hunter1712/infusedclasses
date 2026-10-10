package com.hunter1712.infusedclasses;

/**
 * Bind morph decisions: what a reused bind means for a class. Legionary
 * reuses the Juggernaut binds so binds survive the class: {@code R} arrives as the
 * rotate intent, {@code G} arrives as the oath intent. The oath is one ultimate now,
 * so no stance morph remains: every Legionary's {@code G} is Aequilibrium. The intent
 * receivers wire, this decides; decisions and applies stay pure/adapter.
 */
public final class BindMorph {
	/** What {@code G} means once the class is known. */
	public enum OathTarget {
		REND,
		AEQUILIBRIUM
	}

	private BindMorph() {
	}

	/** Whether {@code R} arrives as the rotate intent: true for Legionaries, Rush for everyone else. */
	public static boolean usesRotate(ClassDefinition def) {
		return def != null && def.hasAbility(ClassRegistry.ROTATE_ABILITY);
	}

	/** What {@code G} means: the oath ultimate for Legionaries, Rend for everyone else. */
	public static OathTarget oathFor(ClassDefinition def) {
		if (def != null && def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return OathTarget.AEQUILIBRIUM;
		}
		return OathTarget.REND;
	}
}
