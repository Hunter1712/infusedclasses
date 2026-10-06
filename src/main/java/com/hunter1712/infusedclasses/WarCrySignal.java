package com.hunter1712.infusedclasses;

/**
 * Edge-triggered War Cry clock flag: whether the client may hold banked War Cry ticks. Marked on
 * every clock signal (fire, proc), cleared when the zero goes out, so the server tick clears the
 * row once on effect loss (milk, death, timeout) and banked ticks can never be resurrected by a
 * later beacon. Plain booleans, so JUnit asserts it with no player.
 */
public final class WarCrySignal {
	private boolean sent;

	/** Marks the clock as signalled: call on every fire and proc. */
	public void markSent() {
		sent = true;
	}

	/** Clears without reporting: call when the zero goes out (switch, respawn). */
	public void clear() {
		sent = false;
	}

	/**
	 * Edge-triggered signal clear. A live effect is adopted (false, and the client holds ticks
	 * worth keeping); a dead effect with a banked signal reports true once — the entry's cue to
	 * send the zero — and disarms. Anything else is silent, so buffless players cost no packet.
	 */
	public boolean pollLost(boolean effectLive) {
		if (effectLive) {
			sent = true;
			return false;
		}
		if (sent) {
			sent = false;
			return true;
		}
		return false;
	}
}
