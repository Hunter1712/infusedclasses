package com.hunter1712.infusedclasses;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transient per-player state: conditional-passive stacks and ability cooldowns. Nothing here is
 * persisted or copied, so all of it dies with the session. The server entry holds one per player
 * and owns that lifetime: created on first use, empty again after a respawn, dropped on logout.
 *
 * Time arrives as a parameter and the class whose passives these are is passed in too, so this
 * module holds no clock and no reference to a player's choice. Stacks, the cap, the grace period
 * and the decay rate are the passive's own facts, read off the definition it is handed, which is
 * also what a stack is keyed by: a passive is addressed by what it is, not by its name.
 */
public final class PlayerRuntime {
	// ponytail: cooldowns reset on relog, persist to NBT if the reset becomes an exploit.
	private final Cooldowns cooldowns = new Cooldowns();
	// Conditional-passive stacks: keyed by the passive definition, never by its name, so a
	// passive is addressed by what it is rather than by what it is called.
	private final Map<ConditionalPassive, Integer> passiveStacks = new HashMap<>();
	// Last-trigger timestamps (millis) for decay: keyed by the passive definition.
	private final Map<ConditionalPassive, Long> lastTriggerTime = new HashMap<>();

	public Cooldowns cooldowns() {
		return cooldowns;
	}

	/** Cooldown verbs, so callers stop reaching through the mutable object. */
	public long warCryRemaining(long now) {
		return cooldowns.warCryRemaining(now);
	}

	public boolean tryUseWarCry(long now) {
		return cooldowns.useWarCry(now);
	}

	public void resetCooldowns() {
		cooldowns.reset();
	}

	/** Current stacks for a passive, 0 when never triggered. */
	public int stacks(ConditionalPassive passive) {
		return passiveStacks.getOrDefault(passive, 0);
	}

	/** Bumps every passive on the given class whose trigger matches.
	 *  Unknown triggers, a missing class and trigger-less classes are inert. */
	public void onTrigger(ClassDefinition def, String trigger, long now) {
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			if (passive.trigger().equals(trigger)) {
				passiveStacks.put(passive, Math.min(passive.maxStacks(), stacks(passive) + 1));
				lastTriggerTime.put(passive, now);
			}
		}
	}

	/**
	 * Ticks decay for all passives on the given class, and says whether any stacks moved. A
	 * caller that puts the effect on the player needs that: a tick which moved no stacks cannot
	 * have changed what should be on the player, so it needs no attribute write.
	 * Decay rule: each passive's own grace period, then its own decay rate.
	 */
	public boolean tickDecay(ClassDefinition def, long now) {
		return !tickDecayMoved(def, now).isEmpty();
	}

	/**
	 * Per-passive movement: which passives lost stacks this tick. The applier needs the set,
	 * not the global flag: a tick that decayed passive A must not remove passive B sitting at
	 * zero. Unmodifiable, possibly empty, never null.
	 */
	public Set<ConditionalPassive> tickDecayMoved(ClassDefinition def, long now) {
		if (def == null) {
			return Set.of();
		}
		Set<ConditionalPassive> moved = new HashSet<>();
		for (ConditionalPassive passive : def.passives()) {
			int current = stacks(passive);
			if (current <= 0) {
				continue;
			}
			Long last = lastTriggerTime.get(passive);
			if (last == null) {
				continue;
			}
			long elapsedSec = (now - last) / 1000;
			if (elapsedSec > passive.graceSeconds()) {
				int decayed = (int) ((elapsedSec - passive.graceSeconds()) * passive.decayPerSecond());
				int next = Math.max(0, current - decayed);
				if (next != current) {
					int removed = current - next;
					passiveStacks.put(passive, next);
					// Advance the clock by the stacks just decayed so the next tick counts a fresh
					// second per stack instead of re-counting the same overdue seconds (07: no acceleration).
					if (passive.decayPerSecond() > 0) {
						lastTriggerTime.put(passive, last + (removed * 1000L) / passive.decayPerSecond());
					}
					moved.add(passive);
				}
			}
		}
		return Collections.unmodifiableSet(moved);
	}

	/** Drops stacks for one class's passives: class switch leaves no stale counts behind. */
	public void clear(ClassDefinition def) {
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			passiveStacks.remove(passive);
			lastTriggerTime.remove(passive);
		}
	}

	/** Drops everything: respawn starts empty. */
	public void clearAll() {
		passiveStacks.clear();
		lastTriggerTime.clear();
	}
}
