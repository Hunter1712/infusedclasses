package com.hunter1712.infusedclasses;

/**
 * Server-side cooldown tracking for active abilities. Pure logic, no Minecraft types, so the state machine
 * can be asserted on directly.
 */
public final class Cooldowns {
	public static final long RUSH_MS = 25_000L;
	public static final long REND_MS = 60_000L;

	private long rushReadyAt;
	private long rendReadyAt;

	/** @return true if Rush fired; false if still on cooldown. Independent of Rend. */
	public boolean useRush(long now) {
		if (now < rushReadyAt) {
			return false;
		}
		rushReadyAt = now + RUSH_MS;
		return true;
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long rushRemaining(long now) {
		return remaining(rushReadyAt, now);
	}

	/** @return true if Rend fired; false if still on cooldown. */
	public boolean useRend(long now) {
		if (now < rendReadyAt) {
			return false;
		}
		rendReadyAt = now + REND_MS;
		return true;
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long rendRemaining(long now) {
		return remaining(rendReadyAt, now);
	}

	/** Ready again immediately: class switch and respawn start empty. */
	public void reset() {
		rushReadyAt = 0;
		rendReadyAt = 0;
	}

	private static long remaining(long readyAt, long now) {
		return Math.max(0L, readyAt - now);
	}
}
