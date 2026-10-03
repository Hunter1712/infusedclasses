package dev.infusedclasses;

/**
 * Server-side cooldown tracking for active abilities. Pure logic, no Minecraft types, so the state machine
 * can be asserted on directly.
 */
public final class Cooldowns {
	public static final long WAR_CRY_MS = 30_000L;
	public static final int WAR_CRY_EFFECT_TICKS = 160; // 8s

	private long warCryReadyAt;

	/** @return true if the ability fired; false if still on cooldown. */
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
}