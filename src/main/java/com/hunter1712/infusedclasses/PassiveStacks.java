package com.hunter1712.infusedclasses;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Conditional-passive stack storage: counts plus last-trigger timestamps, keyed by the passive
 * definition, never by its name. Pure: no player, clock or choice in here — time and the class
 * arrive as parameters, so decay and caps are assertable without a game.
 *
 * <p>Past the grace period every stack drops at once, vanilla-style: there is no per-second
 * bleed. Stacks are transient: they do not survive relog or respawn. {@link PlayerRuntime}
 * owns the only instance per player and coordinates this with cooldowns and ability windows.
 */
final class PassiveStacks {
	private final Map<ConditionalPassive, Integer> passiveStacks = new HashMap<>();
	private final Map<ConditionalPassive, Long> lastTriggerTime = new HashMap<>();

	/** Current stacks for a passive, 0 when never triggered. */
	int stacks(ConditionalPassive passive) {
		return passiveStacks.getOrDefault(passive, 0);
	}

	/** Last-trigger millis for a passive, null when never triggered. Decay counts from it. */
	Long lastTrigger(ConditionalPassive passive) {
		return lastTriggerTime.get(passive);
	}

	/** Bumps every passive on the given class whose trigger matches.
	 *  Unknown triggers, a missing class and trigger-less classes are inert. */
	void onTrigger(ClassDefinition def, String trigger, long now) {
		onTrigger(def, trigger, now, 1.0F);
	}

	/**
	 * Charged bump: only a full swing earns stacks, so spam earns zero.
	 * The no-charge overload above reads as full for callers with no swing
	 * (bonus grants, tests): they already qualified, the gate is for hits.
	 */
	void onTrigger(ClassDefinition def, String trigger, long now, float charge) {
		if (!BonusCurve.isFullSwing(charge)) {
			return;
		}
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
	 * zero. Unmodifiable, possibly empty, never null.
	 */
	Set<ConditionalPassive> tickDecayMoved(ClassDefinition def, long now) {
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
		if (moved.isEmpty()) {
			return Set.of();
		}
		return Collections.unmodifiableSet(moved);
	}

	/** Drops stacks for one class's passives: class switch leaves no stale counts behind. */
	void clear(ClassDefinition def) {
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			clear(passive);
		}
	}

	/** Drops one passive's stacks and timestamp: a spender consumes only what it priced. */
	void clear(ConditionalPassive passive) {
		if (passive == null) {
			return;
		}
		passiveStacks.remove(passive);
		lastTriggerTime.remove(passive);
	}
}
