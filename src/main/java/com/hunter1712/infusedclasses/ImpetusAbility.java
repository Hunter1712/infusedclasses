package com.hunter1712.infusedclasses;

/**
 * Impetus decisions: the Legionary's Gladius-flip buff. No lane, no window to arm:
 * flipping to Gladius stamps the flip record on the transient runtime, and the next
 * melee hit landed within the switch window deals the mult. One-shot: the melee plan
 * peeks the pending buff and the hit settle spends it, so it lands at most once.
 */
public final class ImpetusAbility {
	public static final String NAME = "Impetus";
	/** Flip buff: the next melee hit after flipping to Gladius deals x1.10, at any charge. */
	public static final double SWITCH_DEAL_MULT = 1.10;
	/** Flip buff window in millis: 5s after the flip, then the buff expires unspent. */
	public static final long SWITCH_DEAL_MS = 5_000L;

	private ImpetusAbility() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "Flip buff: after flipping to Gladius, your next melee hit deals x" + SWITCH_DEAL_MULT
				+ " at any charge (" + (SWITCH_DEAL_MS / 1000) + "s, one-shot).";
	}
}
