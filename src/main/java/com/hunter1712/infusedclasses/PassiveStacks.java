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

	/** Bumps every passive on the given class whose trigger matches.
	 *  Unknown triggers, a missing class and trigger-less classes are inert.
	 *  Only a full swing ({@code charge == 1.0}) earns stacks; spam earns zero.
	 *  Callers with no swing (bonus grants, tests) must pass {@code 1.0F} explicitly. */
	void onTrigger(ClassDefinition def, Trigger trigger, long now, float charge) {
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
			if (pastGrace(lastTriggerTime.get(passive), passive.graceSeconds(), now)) {
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

	/** Bumps one passive record N times: single cap application, single grace stamp, same clamp semantics. */
	void bumpPassive(ConditionalPassive passive, int times, long now) {
		if (passive == null || times <= 0) {
			return;
		}
		int current = stacks(passive);
		int capped = Math.min(passive.maxStacks(), current + times);
		if (capped > current) {
			passiveStacks.put(passive, capped);
			lastTriggerTime.put(passive, now);
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

	/**
	 * Grace-expiry math, folded home: one caller, one predicate. Exactly at grace is
	 * still live ({@code >}, never {@code >=}); a passive never triggered has nothing
	 * to decay, so a null timestamp never expires.
	 */
	private static boolean pastGrace(Long lastTrigger, int graceSeconds, long now) {
		if (lastTrigger == null) {
			return false;
		}
		return (now - lastTrigger) > graceSeconds * 1000L;
	}

	/** Millis left before this passive's stacks drop, 0 when it holds none. The same boundary as decay. */
	long graceRemaining(ConditionalPassive passive, long now) {
		Long lastTrigger = lastTriggerTime.get(passive);
		if (stacks(passive) <= 0 || lastTrigger == null) {
			return 0L;
		}
		return Math.max(0L, passive.graceSeconds() * 1000L - (now - lastTrigger));
	}

	/**
	 * The grace clock's last stamp for this passive, 0 when it holds none. Not remaining
	 * time: the same value across a quiet second, and a new value every re-stamp — a hit
	 * at the stack cap re-stamps without moving the count, and this is how that reads.
	 */
	long graceStamp(ConditionalPassive passive) {
		return lastTriggerTime.getOrDefault(passive, 0L);
	}
}
