package com.hunter1712.infusedclasses;

/**
 * Grace-expiry math, single-sourced. Server decay, the stacks signal, and the client's local
 * countdown all answer the same question — how long until past-grace stacks drop — so they all
 * ask here instead of each repeating the {@code grace * 1000} comparison. Time arrives as a
 * parameter, so plain JUnit pins the boundaries: exactly at grace is still live ({@code >},
 * never {@code >=}), past it every stack is already gone.
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

	/**
	 * Millis until decay starts, floored at zero. Zero stacks and a missing timestamp both read
	 * as zero: there is no countdown when there is nothing to lose.
	 */
	public static long millisUntil(Long lastTrigger, int graceSeconds, int stacks, long now) {
		if (lastTrigger == null || stacks <= 0) {
			return 0;
		}
		return Math.max(0, lastTrigger + graceSeconds * 1000L - now);
	}

	/**
	 * The client's local tick of a sent snapshot: the signal's millis-until-decay counted down by
	 * the millis since it arrived, floored at zero. The server stays authoritative on change; this
	 * only interpolates between messages.
	 */
	public static long tickedMillisUntil(long sentAtMillis, long sentMillisUntil, long now) {
		return Math.max(0, sentMillisUntil - (now - sentAtMillis));
	}

	/** Whether a sent snapshot has locally run out: strictly past its countdown, like the server. */
	public static boolean locallyExpired(long sentAtMillis, long sentMillisUntil, long now) {
		return (now - sentAtMillis - sentMillisUntil) > 0;
	}
}
