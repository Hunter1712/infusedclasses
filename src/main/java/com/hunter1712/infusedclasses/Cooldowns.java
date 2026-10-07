package com.hunter1712.infusedclasses;

/**
 * Server-side cooldown tracking for active abilities. Pure logic, no Minecraft types, so the state machine
 * can be asserted on directly.
 */
public final class Cooldowns {
	public static final long BLOODLUST_MS = 30_000L;
	public static final long SECOND_WIND_MS = 60_000L;

	private long bloodlustReadyAt;
	private long secondWindReadyAt;

	/** @return true if Bloodlust fired; false if still on cooldown. Independent of Second Wind. */
	public boolean useBloodlust(long now) {
		if (now < bloodlustReadyAt) {
			return false;
		}
		bloodlustReadyAt = now + BLOODLUST_MS;
		return true;
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long bloodlustRemaining(long now) {
		return Math.max(0L, bloodlustReadyAt - now);
	}

	/** @return true if Second Wind fired; false if still on cooldown. */
	public boolean useSecondWind(long now) {
		if (now < secondWindReadyAt) {
			return false;
		}
		secondWindReadyAt = now + SECOND_WIND_MS;
		return true;
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long secondWindRemaining(long now) {
		return Math.max(0L, secondWindReadyAt - now);
	}

	/** Ready again immediately: class switch and respawn start empty. */
	public void reset() {
		bloodlustReadyAt = 0;
		secondWindReadyAt = 0;
	}
}
