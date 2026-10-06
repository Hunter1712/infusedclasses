package com.hunter1712.infusedclasses;

/**
 * Server-side cooldown tracking for active abilities. Pure logic, no Minecraft types, so the state machine
 * can be asserted on directly.
 */
public final class Cooldowns {
	public static final long WAR_CRY_MS = 30_000L;
	public static final long SECOND_WIND_MS = 60_000L;

	/** One independent lane: its own wait, its own ready-at. Two fields, never shared. */
	private static final class Lane {
		private final long waitMs;
		private long readyAt;

		private Lane(long waitMs) {
			this.waitMs = waitMs;
		}

		private boolean use(long now) {
			if (now < readyAt) {
				return false;
			}
			readyAt = now + waitMs;
			return true;
		}

		private long remaining(long now) {
			return Math.max(0L, readyAt - now);
		}

		private void reset() {
			readyAt = 0;
		}
	}

	private final Lane warCry = new Lane(WAR_CRY_MS);
	private final Lane secondWind = new Lane(SECOND_WIND_MS);

	/** @return true if the ability fired; false if still on cooldown. */
	public boolean useWarCry(long now) {
		return warCry.use(now);
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long warCryRemaining(long now) {
		return warCry.remaining(now);
	}

	/** @return true if the heal fired; false if still on cooldown. Independent of War Cry. */
	public boolean useSecondWind(long now) {
		return secondWind.use(now);
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long secondWindRemaining(long now) {
		return secondWind.remaining(now);
	}

	/** Ready again immediately: class switch and respawn start empty. */
	public void reset() {
		warCry.reset();
		secondWind.reset();
	}
}
