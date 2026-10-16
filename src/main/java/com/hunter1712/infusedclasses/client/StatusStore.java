package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.PlayerStatusPayload;

/**
 * The client's one status snapshot plus the time it arrived. Every payload overwrites
 * both fields whole, and a disconnect clears them: the snapshot is session data, so
 * nothing survives it. The HUD element reads only this store and interpolates its
 * countdowns from the receipt time; no second copy of status state exists client-side.
 */
public final class StatusStore {
	private static PlayerStatusPayload status;
	private static long receivedAt;

	private StatusStore() {
	}

	/** A fresh snapshot: it replaces the old one whole, receipt time included. */
	public static void accept(PlayerStatusPayload payload, long now) {
		status = payload;
		receivedAt = now;
	}

	/** The last snapshot, null until one arrives. */
	public static PlayerStatusPayload status() {
		return status;
	}

	/** Millis since the snapshot arrived: the strip's interpolation anchor. */
	public static long elapsed(long now) {
		return now - receivedAt;
	}

	/** Disconnect: nothing about the old session stays behind. */
	public static void clear() {
		status = null;
		receivedAt = 0L;
	}
}
