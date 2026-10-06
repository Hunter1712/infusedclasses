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
	// Whether the client may hold banked War Cry ticks. Edge-triggered, so the tick clears the
	// row once on effect loss; the signal owns the flag, this only holds it.
	private final WarCrySignal warCrySignal = new WarCrySignal();
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

	public long secondWindRemaining(long now) {
		return cooldowns.secondWindRemaining(now);
	}

	public boolean tryUseSecondWind(long now) {
		return cooldowns.useSecondWind(now);
	}

	public void resetCooldowns() {
		cooldowns.reset();
	}

	/** The War Cry clock flag: marked on every signal, polled edge-triggered on effect loss. */
	public WarCrySignal warCrySignal() {
		return warCrySignal;
	}

	/** Current stacks for a passive, 0 when never triggered. */
	public int stacks(ConditionalPassive passive) {
		return passiveStacks.getOrDefault(passive, 0);
	}

	/** Last-trigger millis for a passive, null when never triggered. The sync signal counts from it. */
	public Long lastTrigger(ConditionalPassive passive) {
		return lastTriggerTime.get(passive);
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
	 * Decay rule: past its own grace, a passive drops every stack at once.
	 */
	public boolean tickDecay(ClassDefinition def, long now) {
		return !tickDecayMoved(def, now).isEmpty();
	}

	/**
	 * Per-passive movement: which passives lost stacks this tick. The applier needs the set,
	 * not the global flag: a tick that decayed passive A must not remove passive B sitting at
	 * zero. Unmodifiable, possibly empty, never null. Past grace every stack drops at once,
	 * vanilla-style: there is no per-second bleed.
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
			if (PassiveExpiry.expired(lastTriggerTime.get(passive), passive.graceSeconds(), now)) {
				passiveStacks.put(passive, 0);
				lastTriggerTime.remove(passive);
				moved.add(passive);
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
			clear(passive);
		}
	}

	/** Drops one passive's stacks and timestamp: a spender consumes only what it priced. */
	public void clear(ConditionalPassive passive) {
		if (passive == null) {
			return;
		}
		passiveStacks.remove(passive);
		lastTriggerTime.remove(passive);
	}

	/** Drops everything: respawn starts empty. */
	public void clearAll() {
		passiveStacks.clear();
		lastTriggerTime.clear();
	}
}
