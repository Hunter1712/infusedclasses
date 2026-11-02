package com.hunter1712.infusedclasses;

/**
 * The vanilla {@code lastHurt} memory after a guarded settle bonus, as a decision: the old
 * memory plus what the bonus actually took off the target. A rejected or fully eaten bonus
 * takes nothing and leaves the memory alone, so later hits still compare against the
 * combined swing. Pure: the adapter reads and writes the field around the hurt.
 */
public final class LastHurtMemory {
	private LastHurtMemory() {
	}

	public static float restore(float lastHurt, float healthBefore, float healthAfter) {
		return lastHurt + Math.max(0.0F, healthBefore - healthAfter);
	}
}
