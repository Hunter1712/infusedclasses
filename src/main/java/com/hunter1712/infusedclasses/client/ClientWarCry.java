package com.hunter1712.infusedclasses.client;

/**
 * Client-side store for the last War Cry clock signal: remaining Strength ticks at send time,
 * ticked down locally between server messages. The server feeds it on fire and every proc and
 * zeroes it on class switch, respawn, and edge-triggered on effect loss; the overlay hides the row
 * while no Strength effect is live as a backstop. Time arrives as a parameter, so a fake clock asserts
 * the interpolation without waiting.
 */
public final class ClientWarCry {
	private record Stored(int expiryTicks, long atMillis) {
	}

	private static Stored last;

	private ClientWarCry() {
	}

	/** Stores the signal, replacing whatever came before. Zero clears. */
	public static void set(int expiryTicks, long now) {
		last = new Stored(expiryTicks, now);
	}

	public static void clear() {
		last = null;
	}

	/** Ticks left, counted down locally at 20 ticks a second and floored at zero. */
	public static int ticksLeft(long now) {
		if (last == null) {
			return 0;
		}
		return (int) Math.max(0, last.expiryTicks - (now - last.atMillis) / 50);
	}
}
