package com.hunter1712.infusedclasses;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Shared gate-and-arm rule for every ability: rejects pass through untouched, a fired press arms
 * its lane, and an arm that somehow fails reports on-cooldown instead of a fire it did not earn.
 * Abilities keep their own numbers, chat lines and windows; this owns only the arming fallback.
 */
public final class AbilityGate {
	private AbilityGate() {
	}

	/**
	 * @param triggered the trigger decision (a reject or a fire)
	 * @param fired whether the trigger fired
	 * @param arm arms the lane; called only when fired
	 * @param remaining lane remaining, read only when the arm fails
	 * @param onCooldown builds the on-cooldown outcome from the remaining
	 * @return the trigger decision when rejected or armed, the on-cooldown outcome when the arm fails
	 */
	public static <R> R gate(R triggered, boolean fired, BooleanSupplier arm, LongSupplier remaining,
			Function<Long, R> onCooldown) {
		if (!fired) {
			return triggered;
		}
		if (arm.getAsBoolean()) {
			return triggered;
		}
		return onCooldown.apply(remaining.getAsLong());
	}
}
