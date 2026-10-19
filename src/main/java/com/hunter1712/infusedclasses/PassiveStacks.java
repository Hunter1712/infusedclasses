package com.hunter1712.infusedclasses;

import java.util.HashMap;
import java.util.Map;

/**
 * Conditional-passive stack storage: the bar, its feed stamp and the clock its bleed runs
 * on, keyed by the passive definition, never by its name. Pure: no player, clock or choice
 * in here — time and the class arrive as parameters, so feeding, spending and the
 * per-second bleed are assertable without a game.
 *
 * <p>Past the grace the bar bleeds one stack per second; the step is a pure function of
 * the count, the next-loss stamp and {@code now}, so a catch-up tick loses exactly what
 * the seconds owed it. Spending moves the count only and never re-stamps the clock. Stacks
 * are transient: they do not survive relog or respawn. {@link PlayerRuntime} owns the only
 * instance per player and coordinates this with cooldowns and ability windows.
 */
final class PassiveStacks {
	/** One held bar: the count, the last feed stamp and the next moment a stack is owed. */
	private record Held(int stacks, long lastFeedAt, long nextDecayAt) {
		static final Held EMPTY = new Held(0, 0L, 0L);
	}

	/** One decay step's answer: the count after it, the next-loss stamp, and how many were lost. */
	record Decay(int stacks, long nextDecayAt, int lost) {
	}

	private final Map<ConditionalPassive, Held> held = new HashMap<>();

	/** Current stacks for a passive, 0 when never triggered. */
	int stacks(ConditionalPassive passive) {
		return held.getOrDefault(passive, Held.EMPTY).stacks();
	}

	/**
	 * Feeds every passive on the given class whose earns name the trigger, each by its own
	 * amount, capped, and re-stamps the bleed: a feed at any stack count restarts the quiet
	 * clock. Unknown triggers, a missing class and trigger-less classes are inert. Only a
	 * full swing ({@code charge == 1.0}) earns; spam earns zero. Callers with no swing
	 * (taken hits, tests) must pass {@code 1.0F} explicitly.
	 */
	void onTrigger(ClassDefinition def, Trigger trigger, long now, float charge) {
		if (!BonusCurve.isFullSwing(charge) || def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			int amount = passive.amountFor(trigger);
			if (amount > 0) {
				feed(passive, amount, now);
			}
		}
	}

	/** One feed: bump by the amount, capped, and re-stamp the quiet clock. */
	private void feed(ConditionalPassive passive, int amount, long now) {
		Held current = held.getOrDefault(passive, Held.EMPTY);
		int capped = Math.min(passive.maxStacks(), current.stacks() + amount);
		held.put(passive, new Held(capped, now, now + passive.graceSeconds() * 1000L));
	}

	/**
	 * Spends up to {@code n} stacks: the amount actually spent, 0 when the bar is empty.
	 * A spend moves the count only; the bleed clock keeps running from the last feed.
	 */
	int spend(ConditionalPassive passive, int n) {
		Held current = held.getOrDefault(passive, Held.EMPTY);
		int spent = Math.min(Math.max(0, n), current.stacks());
		if (spent <= 0) {
			return 0;
		}
		int left = current.stacks() - spent;
		if (left <= 0) {
			held.remove(passive);
		} else {
			held.put(passive, new Held(left, current.lastFeedAt(), current.nextDecayAt()));
		}
		return spent;
	}

	/**
	 * The tick's bleed for every passive on the class: one stack per whole second past
	 * the grace, folded through the pure step so a catch-up tick covers every owed second.
	 */
	void decay(ClassDefinition def, long now) {
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			Held current = held.get(passive);
			if (current == null) {
				continue;
			}
			Decay step = nextDecay(current.stacks(), current.nextDecayAt(), now);
			if (step.lost() <= 0) {
				continue;
			}
			if (step.stacks() <= 0) {
				held.remove(passive);
			} else {
				held.put(passive, new Held(step.stacks(), current.lastFeedAt(), step.nextDecayAt()));
			}
		}
	}

	/**
	 * One decay step, pure: the count after the seconds owed at {@code now}, the next
	 * loss's stamp, and how many were lost. At the grace's own moment nothing is lost
	 * ({@code >}, never {@code >=}); a millisecond past it one stack goes, and every
	 * further whole second one more, so a long-quiet bar catches up in one step.
	 */
	static Decay nextDecay(int stacks, long nextDecayAt, long now) {
		if (stacks <= 0) {
			return new Decay(0, 0L, 0);
		}
		long overdue = now - nextDecayAt;
		if (overdue <= 0) {
			return new Decay(stacks, nextDecayAt, 0);
		}
		int lost = (int) Math.min(stacks, (overdue + 999L) / 1000L);
		int left = stacks - lost;
		return new Decay(left, left <= 0 ? 0L : nextDecayAt + lost * 1000L, lost);
	}

	/** Drops stacks for one class's passives: class switch leaves no stale counts behind. */
	void clear(ClassDefinition def) {
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			held.remove(passive);
		}
	}

	/** Millis until the next stack is owed, 0 when the bar holds none. The same clock the bleed runs on. */
	long graceRemaining(ConditionalPassive passive, long now) {
		Held current = held.get(passive);
		if (current == null || current.stacks() <= 0) {
			return 0L;
		}
		return Math.max(0L, current.nextDecayAt() - now);
	}

	/**
	 * The feed clock's last stamp for this passive, 0 when it holds none. Not remaining
	 * time: the same value across a quiet second, and a new value every feed — a hit at
	 * the stack cap re-stamps without moving the count, and this is how that reads.
	 */
	long graceStamp(ConditionalPassive passive) {
		Held current = held.get(passive);
		return current == null ? 0L : current.lastFeedAt();
	}
}
