package com.hunter1712.infusedclasses;

/**
 * The one full-swing definition, shared by the stack feed, the crit roll and the
 * Onslaught price. Exact {@code >= 1.0F} for now: loosen only with a measured charge
 * histogram, never by guessing at float noise.
 */
public final class FullSwing {
	private FullSwing() {
	}

	/** Whether a swing charge counts as a full swing. */
	public static boolean isFull(float charge) {
		return charge >= 1.0F;
	}
}
