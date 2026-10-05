package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ConditionalPassive;
import com.hunter1712.infusedclasses.PassiveStacksPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side store for the last synced stacks entries per passive. The server stays
 * authoritative and pushes on change; this only ticks the displayed countdown down
 * locally between messages. No drawing here. Time arrives as a parameter, so a fake
 * clock asserts the interpolation without waiting.
 */
public final class ClientPassiveStacks {
	private record Stored(int stacks, long atMillis, long millisUntilDecayAtSend) {
	}

	private static final Map<String, Stored> BY_NAME = new LinkedHashMap<>();

	private ClientPassiveStacks() {
	}

	/**
	 * Stores the signal's entries by passive name, replacing whatever came before: every
	 * signal carries the full per-passive state, so an empty signal and a missing name
	 * both clear. The class confirm never calls this, so mid-fight rows survive it.
	 */
	public static void setAll(List<PassiveStacksPayload.Entry> entries, long now) {
		BY_NAME.clear();
		if (entries == null) {
			return;
		}
		for (PassiveStacksPayload.Entry entry : entries) {
			BY_NAME.put(entry.passiveName(), new Stored(entry.stacks(), now, entry.millisUntilDecay()));
		}
	}

	public static void clear() {
		BY_NAME.clear();
	}

	public static boolean isEmpty() {
		return BY_NAME.isEmpty();
	}

	/**
	 * Stacks with the local drop applied, so a stale entry past grace reads as zero
	 * instead of a stuck row. Mirrors the server's all-at-once clear from the send-time
	 * snapshot.
	 */
	public static int stacksFor(ConditionalPassive passive, long now) {
		Stored stored = BY_NAME.get(passive.name());
		if (stored == null) {
			return 0;
		}
		if (now - stored.atMillis - stored.millisUntilDecayAtSend > 0) {
			return 0;
		}
		return stored.stacks;
	}

	/** Millis until decay starts, ticked down locally and floored at zero. */
	public static long millisUntilDecay(String passiveName, long now) {
		Stored stored = BY_NAME.get(passiveName);
		if (stored == null) {
			return 0;
		}
		return Math.max(0, stored.millisUntilDecayAtSend - (now - stored.atMillis));
	}
}
