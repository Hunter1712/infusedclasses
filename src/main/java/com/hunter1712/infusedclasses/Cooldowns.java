package com.hunter1712.infusedclasses;

/**
 * Server-side cooldown tracking for active abilities. Pure logic, no Minecraft types, so the state machine
 * can be asserted on directly.
 */
public final class Cooldowns {
	public static final long SECOND_WIND_MS = 60_000L;

	private long readyAt;

	/** @return true if the heal fired; false if still on cooldown. */
	public boolean useSecondWind(long now) {
		if (now < readyAt) {
			return false;
		}
		readyAt = now + SECOND_WIND_MS;
		return true;
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long secondWindRemaining(long now) {
		return Math.max(0L, readyAt - now);
	}

	/** Ready again immediately: class switch and respawn start empty. */
	public void reset() {
		readyAt = 0;
	}
}
