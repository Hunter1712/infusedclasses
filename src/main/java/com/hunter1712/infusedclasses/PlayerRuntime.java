package com.hunter1712.infusedclasses;

import java.util.HashMap;
import java.util.Map;

/**
 * Transient per-player state: conditional-passive stacks and ability cooldowns. Nothing here is
 * persisted or copied, so all of it dies with the session. The server entry holds one per player
 * and owns that lifetime: created on first use, empty again after a respawn, dropped on logout.
 *
 * Time arrives as a parameter and the class whose passives these are is passed in too, so this
 * module holds no clock and no reference to a player's choice. Stacks, the cap, the grace period
 * and the decay rate are the passive's own facts, read off the definition it is handed.
 */
public final class PlayerRuntime {
	// ponytail: cooldowns reset on relog, persist to NBT if the reset becomes an exploit.
	private final Cooldowns cooldowns = new Cooldowns();
	// Conditional-passive stacks: keyed by passive name.
	private final Map<String, Integer> passiveStacks = new HashMap<>();
	// Last-trigger timestamps (millis) for decay: keyed by passive name.
	private final Map<String, Long> lastTriggerTime = new HashMap<>();

	public Cooldowns cooldowns() {
		return cooldowns;
	}

	/** Current stacks for a passive, 0 when never triggered. */
	public int stacks(String passiveName) {
		return passiveStacks.getOrDefault(passiveName, 0);
	}

	/** Bumps every passive on the given class whose trigger matches.
	 *  Unknown triggers, a missing class and trigger-less classes are inert. */
	public void onTrigger(ClassDefinition def, String trigger, long now) {
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			if (passive.trigger().equals(trigger)) {
				passiveStacks.put(passive.name(), Math.min(passive.maxStacks(), stacks(passive.name()) + 1));
				lastTriggerTime.put(passive.name(), now);
			}
		}
	}

	/**
	 * Ticks decay for all passives on the given class.
	 * Returns true if any stacks changed (so callers can re-apply modifiers).
	 * Decay rule: each passive's own grace period, then its own decay rate.
	 */
	public boolean tickDecay(ClassDefinition def, long now) {
		if (def == null) {
			return false;
		}
		boolean anyChanged = false;
		for (ConditionalPassive passive : def.passives()) {
			String name = passive.name();
			int current = stacks(name);
			if (current <= 0) {
				continue;
			}
			Long last = lastTriggerTime.get(name);
			if (last == null) {
				continue;
			}
			long elapsedSec = (now - last) / 1000;
			if (elapsedSec > passive.graceSeconds()) {
				int decayed = (int) ((elapsedSec - passive.graceSeconds()) * passive.decayPerSecond());
				int next = Math.max(0, current - decayed);
				if (next != current) {
					passiveStacks.put(name, next);
					anyChanged = true;
				}
			}
		}
		return anyChanged;
	}
}
