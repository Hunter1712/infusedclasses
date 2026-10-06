package com.hunter1712.infusedclasses;

/**
 * Server-side cooldown tracking for active abilities. Pure logic, no Minecraft types, so the state machine
 * can be asserted on directly.
 */
public final class Cooldowns {
	public static final long WAR_CRY_MS = 20_000L;
	public static final long SECOND_WIND_MS = 60_000L;

	private long warCryReadyAt;
	private long secondWindReadyAt;

	/** @return true if the shout fired; false if still on cooldown. Independent of Second Wind. */
	public boolean useWarCry(long now) {
		if (now < warCryReadyAt) {
			return false;
		}
		warCryReadyAt = now + WAR_CRY_MS;
		return true;
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long warCryRemaining(long now) {
		return Math.max(0L, warCryReadyAt - now);
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
		warCryReadyAt = 0;
		secondWindReadyAt = 0;
	}
}
