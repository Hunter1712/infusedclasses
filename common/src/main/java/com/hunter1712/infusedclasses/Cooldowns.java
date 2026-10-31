package com.hunter1712.infusedclasses;

import java.util.EnumMap;

/**
 * Server-side cooldown tracking: one ready-at stamp per lane, in one map. Pure logic, no
 * Minecraft types, so the state machine can be asserted on directly. Lanes are addressed by
 * the {@link Lane} value that owns its own length, so a seventh lane adds a value and no verb;
 * callers and tests cross the lane-keyed pair, never the map.
 */
public final class Cooldowns {
	private final EnumMap<Lane, Long> readyAt = new EnumMap<>(Lane.class);

	/** @return true if the lane fired; false while it is still cooling. Lanes are independent. */
	public boolean use(Lane lane, long now) {
		if (now < readyAt.getOrDefault(lane, 0L)) {
			return false;
		}
		readyAt.put(lane, now + lane.cooldownMs());
		return true;
	}

	/** Milliseconds left on the lane, 0 when ready. Zero means ready. */
	public long remaining(Lane lane, long now) {
		return Math.max(0L, readyAt.getOrDefault(lane, 0L) - now);
	}

	/** Ready again immediately: class switch and respawn start empty. */
	public void reset() {
		readyAt.clear();
	}
}
