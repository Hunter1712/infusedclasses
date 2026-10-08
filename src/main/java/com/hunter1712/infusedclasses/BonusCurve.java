package com.hunter1712.infusedclasses;

/**
 * Charged bonus curve shared by every melee class: mod hook bonus damage scales
 * quadratically with swing charge. Full swings pay full bonus, half charge pays
 * a quarter, spam pays chips. Base weapon damage is never routed here — vanilla
 * already halves spam, and the mod adds no second cut to it.
 */
public final class BonusCurve {
	private BonusCurve() {
	}

	/**
	 * Bonus damage for a base amount at a charge 0-1. Clamped, so overcharge
	 * reads full; a non-positive base reads 0, never negative.
	 */
	public static float bonusFor(float base, float charge) {
		if (base <= 0) {
			return 0;
		}
		float clamped = Math.max(0, Math.min(1, charge));
		return base * clamped * clamped;
	}

	/**
	 * One definition of a full swing, shared by the stack gate and the bonus
	 * curve. Exact {@code >= 1.0F} for now: loosen only with a measured charge
	 * histogram, never by guessing at float noise.
	 */
	public static boolean isFullSwing(float charge) {
		return charge >= 1.0F;
	}
}
