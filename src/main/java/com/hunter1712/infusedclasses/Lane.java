package com.hunter1712.infusedclasses;

/**
 * One ability lane: the shared key every cooldown, window and lane read is addressed by,
 * and the home of the lane's own cooldown length. A lane is one row, so a seventh window
 * adds a value here — its cooldown beside it, its window constant at the arm site it fires
 * from — never a field, a verb, a snapshot component or a reset line.
 *
 * <p>Plain data, no Minecraft types: tests address lanes directly, and the runtime's map
 * stays behind the lane-keyed verbs.
 */
public enum Lane {
	/** Juggernaut Offload lane: one Momentum spend per 10s. */
	OFFLOAD(10_000L),
	/** Juggernaut Onslaught lane: the ultimate's 60s wait. */
	ONSLAUGHT(60_000L),
	/** Rotate lock in millis: 8s between stance flips. Owned by the lane that enforces it. */
	ROTATE(8_000L),
	/** The Legionary shared oath lane: 50s, owned solely by Aequilibrium. */
	OATH(50_000L),
	/** Gambler Roulette lane: one free spin per 25s. */
	ROULETTE(25_000L),
	/** Gambler All In lane: the ultimate's 75s wait. */
	ALL_IN(75_000L);

	private final long cooldownMs;

	Lane(long cooldownMs) {
		this.cooldownMs = cooldownMs;
	}

	/** The wait this lane enforces after a fire: the lane's own fact, read by the cooldowns and the copy. */
	public long cooldownMs() {
		return cooldownMs;
	}
}
