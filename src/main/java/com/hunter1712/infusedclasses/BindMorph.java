package com.hunter1712.infusedclasses;

/**
 * Bind morph decisions: what a reused bind means for a class and stance. Legionary
 * reuses the Juggernaut binds so binds survive the class: {@code R} arrives as the
 * rotate intent, {@code G} arrives as the oath intent the stance morphs. The intent
 * receivers wire, this decides; decisions and applies stay pure/adapter.
 */
public final class BindMorph {
	/** What {@code G} means once the class and stance are known. */
	public enum OathTarget {
		REND,
		IMPETUS,
		TESTUDO
	}

	private BindMorph() {
	}

	/** Whether {@code R} arrives as the rotate intent: true for Legionaries, Rush for everyone else. */
	public static boolean usesRotate(ClassDefinition def) {
		return def != null && def.activeAbilities().contains(ClassRegistry.ROTATE_ABILITY);
	}

	/** What {@code G} means: the stance-morphed oath for Legionaries, Rend for everyone else. */
	public static OathTarget oathFor(ClassDefinition def, RotateAbility.Stance stance) {
		if (def != null && def.activeAbilities().contains(ClassRegistry.IMPETUS_ABILITY)) {
			return stance == RotateAbility.Stance.GLADIUS ? OathTarget.IMPETUS : OathTarget.TESTUDO;
		}
		return OathTarget.REND;
	}
}
