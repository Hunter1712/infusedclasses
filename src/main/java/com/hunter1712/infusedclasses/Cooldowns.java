package com.hunter1712.infusedclasses;

import java.util.EnumMap;

/**
 * Server-side cooldown tracking for active abilities. Pure logic, no Minecraft types, so the state machine
 * can be asserted on directly.
 */
public final class Cooldowns {
	public static final long RUSH_MS = 25_000L;
	public static final long REND_MS = 60_000L;
	/** Rotate lock in millis: 8s between stance flips. Owned here with the lane that enforces it. */
	public static final long ROTATE_MS = 8_000L;
	/** The Legionary shared oath lane: 50s, owned solely by Aequilibrium. */
	public static final long OATH_MS = 50_000L;
	/** Gambler Roulette lane: one free spin per 25s. */
	public static final long ROULETTE_MS = 25_000L;
	/** Gambler All In lane: the ultimate's 75s wait. */
	public static final long ALL_IN_MS = 75_000L;

	/**
	 * One lane per ability family behind a single map: a fifth lane adds one value
	 * plus two one-line verbs, never a field plus a reset line. Private: callers and
	 * tests cross the per-lane verbs, never this.
	 */
	private enum Lane {
		RUSH,
		REND,
		ROTATE,
		OATH,
		ROULETTE,
		ALL_IN
	}

	private final EnumMap<Lane, Long> readyAt = new EnumMap<>(Lane.class);

	private static long duration(Lane lane) {
		return switch (lane) {
			case RUSH -> RUSH_MS;
			case REND -> REND_MS;
			case ROTATE -> ROTATE_MS;
			case OATH -> OATH_MS;
			case ROULETTE -> ROULETTE_MS;
			case ALL_IN -> ALL_IN_MS;
		};
	}

	private boolean use(Lane lane, long now) {
		if (now < readyAt.getOrDefault(lane, 0L)) {
			return false;
		}
		readyAt.put(lane, now + duration(lane));
		return true;
	}

	private long remaining(Lane lane, long now) {
		return Math.max(0L, readyAt.getOrDefault(lane, 0L) - now);
	}

	/** @return true if Rush fired; false if still on cooldown. Independent of Rend. */
	public boolean useRush(long now) {
		return use(Lane.RUSH, now);
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long rushRemaining(long now) {
		return remaining(Lane.RUSH, now);
	}

	/** @return true if Rend fired; false if still on cooldown. */
	public boolean useRend(long now) {
		return use(Lane.REND, now);
	}

	/** Milliseconds left, 0 when ready. Zero means ready. */
	public long rendRemaining(long now) {
		return remaining(Lane.REND, now);
	}

	/** Ready again immediately: class switch and respawn start empty. */
	public void reset() {
		readyAt.clear();
	}

	/** @return true if Rotate fired; false if still on its lock. */
	public boolean useRotate(long now) {
		return use(Lane.ROTATE, now);
	}

	/** Milliseconds left on the rotate lock, 0 when ready. */
	public long rotateRemaining(long now) {
		return remaining(Lane.ROTATE, now);
	}

	/** @return true if an oath ability fired; false if the shared lane is held. */
	public boolean useOath(long now) {
		return use(Lane.OATH, now);
	}

	/** Milliseconds left on the shared oath lane, 0 when ready. */
	public long oathRemaining(long now) {
		return remaining(Lane.OATH, now);
	}

	/** @return true if a spin fired; false if the Roulette lane is still on cooldown. Independent of every other lane. */
	public boolean useRoulette(long now) {
		return use(Lane.ROULETTE, now);
	}

	/** Milliseconds left on the Roulette lane, 0 when ready. */
	public long rouletteRemaining(long now) {
		return remaining(Lane.ROULETTE, now);
	}

	/** @return true if All In fired; false if its lane is still on cooldown. Independent of every other lane. */
	public boolean useAllIn(long now) {
		return use(Lane.ALL_IN, now);
	}

	/** Milliseconds left on the All In lane, 0 when ready. */
	public long allInRemaining(long now) {
		return remaining(Lane.ALL_IN, now);
	}
}
