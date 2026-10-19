package com.hunter1712.infusedclasses;

/**
 * The one definition of a full swing, shared by the stack feed, the crit roll and the
 * Onslaught price: full swings earn and roll, half swings and spam do neither. The
 * charge curve itself retired with Rush and Rend; the gate stayed.
 */
public final class BonusCurve {
	private BonusCurve() {
	}

	/**
	 * Exact {@code >= 1.0F} for now: loosen only with a measured charge histogram,
	 * never by guessing at float noise.
	 */
	public static boolean isFullSwing(float charge) {
		return charge >= 1.0F;
	}
}
