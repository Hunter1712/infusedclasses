package com.hunter1712.infusedclasses;

/**
 * War Cry extension decision. Pure: given one melee hit, answers the Strength duration it should
 * now carry, or 0 when the swing earns nothing. RNG arrives as a roll in [0,1) so the adapter owns
 * randomness and JUnit asserts without it. One sweep into N victims is N calls, one roll each.
 * No cap, no internal cooldown, no per-swing dedup; the full-charge gate is the only throttle.
 * Source of truth stays the live effect duration, never a timestamp: milk and death clear the
 * effect, and a cleared effect means no proc.
 */
public final class WarCryExtension {
	/** Proc chance per fully-charged victim hit. */
	public static final double PROC_RATE = 0.25;
	/** Duration growth per proc, in ticks (2s). */
	public static final int EXTEND_TICKS = 40;

	/**
	 * One melee hit, as the decision sees it: whether the attacker is a Warrior, whether the
	 * swing is fully charged, whether War Cry's own Strength (amplifier 1, not any foreign
	 * Strength) is live and for how long, whether the victim is a player or hostile mob, and
	 * the RNG roll in [0,1).
	 */
	public record Hit(boolean warrior, boolean charged, boolean warCryLive, boolean victimEligible,
			int remainingTicks, double roll) {
	}

	private WarCryExtension() {
	}

	/**
	 * New Strength duration in ticks, or 0 when this hit extends nothing: non-Warrior attackers,
	 * uncharged swings, a missing/cleared/foreign Strength instance, ineligible victims, and
	 * rolls at or above the rate all yield 0. A proc yields {@code remainingTicks + EXTEND_TICKS}
	 * with no clamp.
	 */
	public static int extendRoll(Hit hit) {
		if (!hit.warrior() || !hit.charged() || !hit.warCryLive() || !hit.victimEligible()
				|| hit.roll() >= PROC_RATE) {
			return 0;
		}
		return hit.remainingTicks() + EXTEND_TICKS;
	}
}
