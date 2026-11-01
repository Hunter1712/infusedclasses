package com.hunter1712.infusedclasses;

import java.util.EnumMap;

/**
 * One ability window per lane: a press timestamp plus the duration stamped at arm time.
 * A window is timed and peeked, never owed; its spend is decided by whoever reads it
 * (the melee plan prices Onslaught off the live window). Pure logic, no Minecraft types.
 */
final class AbilityWindows {
	private final EnumMap<Lane, Window> windows = freshWindows();

	static EnumMap<Lane, Window> freshWindows() {
		EnumMap<Lane, Window> map = new EnumMap<>(Lane.class);
		for (Lane lane : Lane.values()) {
			map.put(lane, new Window());
		}
		return map;
	}

	/** Stamps the lane's window at {@code now} for {@code durationMs}. */
	void arm(Lane lane, long now, long durationMs) {
		windows.get(lane).arm(now, durationMs);
	}

	/** Milliseconds left on the lane's window, 0 when not live. */
	long remainingMs(Lane lane, long now) {
		return windows.get(lane).remainingMs(now);
	}

	/** Whether the lane's window is live and still holds its one-shot flag. */
	boolean hasPending(Lane lane, long now) {
		return windows.get(lane).hasPending(now);
	}

	/** Consumes the lane's one-shot exactly once: true when one was waiting. */
	boolean consume(Lane lane, long now) {
		return windows.get(lane).consume(now);
	}

	/** Clears the lane's window; its cooldown is untouched. */
	void clear(Lane lane) {
		windows.get(lane).clear();
	}

	/** Clears every window: switch and respawn never inherit a window. */
	void clearAll() {
		for (Window window : windows.values()) {
			window.clear();
		}
	}

	/** Whether the lane's window is live at {@code now}. */
	boolean isLive(Lane lane, long now) {
		return windows.get(lane).isLive(now);
	}

	private static final class Window {
		private long pressedAt = -1L;
		private long durationMs;
		private boolean pending;

		boolean isLive(long now) {
			return pressedAt >= 0 && now - pressedAt < durationMs;
		}

		long remainingMs(long now) {
			if (!isLive(now)) {
				return 0L;
			}
			return durationMs - (now - pressedAt);
		}

		void arm(long now, long durationMs) {
			pressedAt = now;
			this.durationMs = durationMs;
			pending = true;
		}

		boolean hasPending(long now) {
			return pending && isLive(now);
		}

		boolean consume(long now) {
			if (!hasPending(now)) {
				return false;
			}
			pending = false;
			return true;
		}

		void clear() {
			pressedAt = -1L;
			durationMs = 0;
			pending = false;
		}
	}
}
