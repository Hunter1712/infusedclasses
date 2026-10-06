package com.hunter1712.infusedclasses;

/**
 * Grace-expiry math, single-sourced. Server decay answers one question — how long until
 * past-grace stacks drop — so it asks here instead of repeating the
 * {@code grace * 1000} comparison. Time arrives as a parameter, so plain JUnit pins the
 * boundaries: exactly at grace is still live ({@code >}, never {@code >=}), past it every
 * stack is already gone.
 */
public final class PassiveExpiry {
	private PassiveExpiry() {
	}

	/**
	 * Whether stacks last triggered at {@code lastTrigger} are past their grace at {@code now}.
	 * A passive never triggered has nothing to decay, so a null timestamp never expires.
	 */
	public static boolean expired(Long lastTrigger, int graceSeconds, long now) {
		if (lastTrigger == null) {
			return false;
		}
		return (now - lastTrigger) > graceSeconds * 1000L;
	}
}
