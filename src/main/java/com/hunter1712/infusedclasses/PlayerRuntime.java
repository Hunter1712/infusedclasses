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
	// Bloodlust window: press timestamp (millis), live for the ability's window after a fired
	// press. Cleared with everything else, so switch and respawn never inherit a window.
	private long bloodlustPressedAt = -1L;
	// Conditional-passive stacks: keyed by the passive definition, never by its name, so a
	// passive is addressed by what it is rather than by what it is called.
	private final Map<ConditionalPassive, Integer> passiveStacks = new HashMap<>();
	// Last-trigger timestamps (millis) for decay: keyed by the passive definition.
	private final Map<ConditionalPassive, Long> lastTriggerTime = new HashMap<>();

	public Cooldowns cooldowns() {
		return cooldowns;
	}

	/** Cooldown verbs: arming runs through here, so no caller reaches through the mutable object. */
	public long bloodlustRemaining(long now) {
		return cooldowns.bloodlustRemaining(now);
	}

	/** Arms the Bloodlust lane and stamps the lifesteal window together: fired presses only. */
	public boolean tryUseBloodlust(long now) {
		if (!cooldowns.useBloodlust(now)) {
			return false;
		}
		bloodlustPressedAt = now;
		return true;
	}

	/** Whether a fired Bloodlust window is still live: press timestamp plus the ability's window. */
	public boolean isBloodlustLive(long now) {
		return bloodlustPressedAt >= 0 && now - bloodlustPressedAt < BloodlustAbility.WINDOW_TICKS * 50L;
	}

	public long secondWindRemaining(long now) {
		return cooldowns.secondWindRemaining(now);
	}

	public boolean tryUseSecondWind(long now) {
		return cooldowns.useSecondWind(now);
	}

	/** Reset for class switch: a new class starts empty. The adapter calls this, not ability callers. */
	public void resetCooldowns() {
		cooldowns.reset();
		bloodlustPressedAt = -1L;
	}

	/** Current stacks for a passive, 0 when never triggered. */
	public int stacks(ConditionalPassive passive) {
		return passiveStacks.getOrDefault(passive, 0);
	}

	/** Last-trigger millis for a passive, null when never triggered. Decay counts from it. */
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
}
