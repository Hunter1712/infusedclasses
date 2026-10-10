package com.hunter1712.infusedclasses;

/**
 * Testudo decisions: the Legionary's Scutum-flip buff. No lane, no window to arm:
 * flipping to Scutum stamps the flip record on the transient runtime, and melee taken
 * within the switch window pays the mult. Timed, never consumed: every hit inside the
 * catch window is discounted.
 */
public final class TestudoAbility {
	public static final String NAME = "Testudo";
	/** Flip buff: melee taken within the catch window pays x0.85. */
	public static final double SWITCH_TAKE_MULT = 0.85;
	/** Flip buff window in millis: 1.5s after the flip, then the catch closes. */
	public static final long SWITCH_TAKE_MS = 1_500L;

	private TestudoAbility() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "Flip buff: after flipping to Scutum, melee taken x" + SWITCH_TAKE_MULT
				+ " for " + ConditionalPassive.amount(SWITCH_TAKE_MS / 1000.0) + "s.";
	}
}
