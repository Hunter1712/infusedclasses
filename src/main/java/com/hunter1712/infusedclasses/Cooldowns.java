package com.hunter1712.infusedclasses;

/**
 * Server-side cooldown tracking for active abilities. Pure logic, no Minecraft types, so the state machine
 * can be asserted on directly.
 */
public final class Cooldowns {
	public static final long RUSH_MS = 25_000L;
	public static final long REND_MS = 60_000L;
	/** Rotate lock in millis: 8s between stance flips. Owned here with the lane that enforces it. */
	public static final long ROTATE_MS = 8_000L;
	/** Shared Legionary oath lane: firing Impetus locks Testudo and vice versa. */
	public static final long OATH_MS = 50_000L;

	private long rushReadyAt;
	private long rendReadyAt;
	private long rotateReadyAt;
	private long oathReadyAt;

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
		rotateReadyAt = 0;
		oathReadyAt = 0;
	}

	/** @return true if Rotate fired; false if still on its lock. */
	public boolean useRotate(long now) {
		if (now < rotateReadyAt) {
			return false;
		}
		rotateReadyAt = now + ROTATE_MS;
		return true;
	}

	/** Milliseconds left on the rotate lock, 0 when ready. */
	public long rotateRemaining(long now) {
		return remaining(rotateReadyAt, now);
	}

	/** @return true if an oath ability fired; false if the shared lane is held. */
	public boolean useOath(long now) {
		if (now < oathReadyAt) {
			return false;
		}
		oathReadyAt = now + OATH_MS;
		return true;
	}

	/** Milliseconds left on the shared oath lane, 0 when ready. */
	public long oathRemaining(long now) {
		return remaining(oathReadyAt, now);
	}

	private static long remaining(long readyAt, long now) {
		return Math.max(0L, readyAt - now);
	}
}
