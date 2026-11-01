package com.hunter1712.infusedclasses;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Transient per-player state: conditional-passive stacks and ability cooldowns. Nothing here is
 * persisted or copied, so all of it dies with the session. The server entry holds one per player
 * and owns that lifetime: created on first use, empty again after a respawn, dropped on logout.
 *
 * <p>Time arrives as a parameter and the class whose passives these are is passed in too, so this
 * module holds no clock and no reference to anyone's choice. Stack storage lives in
 * {@link PassiveStacks}, ability windows in {@link AbilityWindows}, stance/flip/Streak in
 * {@link AbilityState}, and cooldowns in {@link Cooldowns}. This module coordinates them and
 * exposes one transient read: {@link #state}.
 *
 * <p>Reads cross one seam: {@link #state} answers the whole transient read at a {@code now},
 * so the status paths take one shape and one clock. Everything below it mutates.
 */
public final class PlayerRuntime {
	private final Cooldowns cooldowns = new Cooldowns();
	private final AbilityWindows windows = new AbilityWindows();
	private final PassiveStacks passiveStacks = new PassiveStacks();
	private final AbilityState abilityState = new AbilityState();

	/**
	 * The whole transient read at {@code now}: the one shape every status path crosses
	 * this seam with, so lane and window reads cannot re-state their semantics per
	 * caller. One {@link LaneRead} per lane; {@code flipTarget} is the live flip window's
	 * landing, null when no flip window is live; {@code stance} is Scutum until the first
	 * Rotate. The bar is not lane state and is read off the runtime by the callers that
	 * need it (the snapshot, the applier, the melee plan).
	 */
	public record State(
			Map<Lane, LaneRead> lanes,
			RotateAbility.Stance flipTarget,
			RouletteAbility.Outcome rouletteOutcome,
			RotateAbility.Stance stance,
			int streak) {
		/** Milliseconds left on a lane, 0 when ready. */
		public long laneRemaining(Lane lane) {
			return lanes.get(lane).remainingMs();
		}

		/** A lane's whole read: its cooldown, its window remainder and its one-shot flag. */
		public LaneRead lane(Lane lane) {
			return lanes.get(lane);
		}

		/** The Impetus flip buff: the rotate lane's one-shot, which only a Gladius landing carries. */
		public boolean impetusPending() {
			return lanes.get(Lane.ROTATE).windowPending();
		}

		/**
		 * Whether anything a client cannot derive moved since the given read: the discrete
		 * facts plus each lane's armed flags, never raw remainders, which decay on every
		 * rebuild. A null previous read is always a move (nothing was sent).
		 */
		public boolean movedSince(State prev) {
			if (prev == null) {
				return true;
			}
			if (!Objects.equals(flipTarget, prev.flipTarget())
					|| !Objects.equals(rouletteOutcome, prev.rouletteOutcome())
					|| !Objects.equals(stance, prev.stance())
					|| streak != prev.streak()) {
				return true;
			}
			for (Lane lane : Lane.values()) {
				LaneRead self = lanes.get(lane);
				LaneRead before = prev.lane(lane);
				if (armed(self.remainingMs()) != armed(before.remainingMs())
						|| armed(self.windowRemainingMs()) != armed(before.windowRemainingMs())
						|| self.windowPending() != before.windowPending()) {
					return true;
				}
			}
			return false;
		}

		private static boolean armed(long remainingMs) {
			return remainingMs > 0;
		}
	}

	/** One clock, one shape: every fact the status paths read, taken at the same {@code now}. */
	public State state(long now) {
		EnumMap<Lane, LaneRead> lanes = new EnumMap<>(Lane.class);
		for (Lane lane : Lane.values()) {
			lanes.put(lane, new LaneRead(
					cooldowns.remaining(lane, now),
					windows.remainingMs(lane, now),
					oneShot(lane, now)));
		}
		return new State(
				lanes,
				windows.isLive(Lane.ROTATE, now) ? abilityState.flippedTo() : null,
				windows.isLive(Lane.ROULETTE, now) ? abilityState.rouletteOutcome() : null,
				abilityState.stance(),
				abilityState.streak());
	}

	/**
	 * The lane's one-shot read at {@code now}: its window's pending flag, except Rotate's,
	 * whose one-shot is the Impetus buff — only a flip landed on Gladius carries one, so
	 * the raw window flag (set by every arm) is never the read its consumers want.
	 */
	private boolean oneShot(Lane lane, long now) {
		return windows.hasPending(lane, now)
				&& (lane != Lane.ROTATE || abilityState.flippedTo() == RotateAbility.Stance.GLADIUS);
	}

	// Joint arms: lane plus window stamped together, so a fired press can never arm one
	// without the other. The duration is the firing ability's own constant, passed in at
	// its arm site (or, for the two landings that carry one, named by the landing); the
	// trigger read the same lane at the same now, so the use here cannot fail for a fired press.
	/** Arms the lane and stamps its window together: every fired press. */
	public boolean arm(Lane lane, long now, long windowMs) {
		if (!cooldowns.use(lane, now)) {
			return false;
		}
		windows.arm(lane, now, windowMs);
		return true;
	}

	/** Arms the rotate lock and flips to the landing together: fired Rotate presses only. */
	public boolean rotate(long now, RotateAbility.Stance landing) {
		long windowMs = landing == RotateAbility.Stance.GLADIUS
				? RotateAbility.SWITCH_DEAL_MS : RotateAbility.SWITCH_TAKE_MS;
		if (!arm(Lane.ROTATE, now, windowMs)) {
			return false;
		}
		abilityState.stance(landing);
		abilityState.flippedTo(landing);
		return true;
	}

	/** Arms the Roulette lane and records the landed band together: fired spins only. */
	public boolean roulette(long now, RouletteAbility.Outcome outcome) {
		if (!arm(Lane.ROULETTE, now, RouletteAbility.WINDOW_TICKS * 50L)) {
			return false;
		}
		abilityState.rouletteOutcome(outcome);
		return true;
	}

	/**
	 * Consumes the one-shot a lane's window carries, exactly once: true when one was waiting,
	 * false when nothing was owed. Rotate's Impetus spends through here; a stale window with
	 * no bonus left grants nothing, and an unswung window simply stops being live.
	 */
	public boolean consumeBonus(Lane lane, long now) {
		return windows.consume(lane, now);
	}

	/**
	 * Ends a lane's window early, its cooldown untouched: the owner of a window whose
	 * duration is not purely the clock (Onslaught lasts its bar too) says when it is over.
	 * Nothing owed is spilled — a timed window's rate is read while it lives and nowhere else.
	 */
	public void endWindow(Lane lane) {
		windows.clear(lane);
	}

	/** One non-crit full hit loads the pocket: called by the hit settle with the plan's movement. */
	public void bumpStreak() {
		abilityState.bumpStreak();
	}

	/** A landed crit spends the pocket: called by the hit settle with the plan's movement. */
	public void resetStreak() {
		abilityState.resetStreak();
	}

	/**
	 * Reset for class switch and respawn: every lane, window, landing, the stance and the
	 * Streak start empty. The adapter calls this, not ability callers.
	 */
	public void resetAll() {
		cooldowns.reset();
		windows.clearAll();
		abilityState.resetAll();
	}

	/** Current stacks for a passive, 0 when never triggered. */
	public int stacks(ConditionalPassive passive) {
		return passiveStacks.stacks(passive);
	}

	/**
	 * One bar's whole read at {@code now}: its count, the grace millis left and the feed
	 * stamp. The snapshot takes this one value instead of three forwards, so the three
	 * facts that travel together are assembled once.
	 */
	public BarRead bar(ConditionalPassive passive, long now) {
		return new BarRead(passiveStacks.stacks(passive),
				passiveStacks.graceRemaining(passive, now),
				passiveStacks.graceStamp(passive));
	}

	/** One bar's whole wire read: the count, the grace remainder and the feed stamp. */
	public record BarRead(int stacks, long graceRemainingMs, long graceStampMs) {
		/** No bar at all, for a classless player. */
		public static final BarRead NONE = new BarRead(0, 0L, 0L);
	}

	/** Bumps every passive on the given class whose earns name the trigger, each by its own amount.
	 *  Unknown triggers, a missing class and trigger-less classes are inert.
	 *  Only a full swing ({@code charge == 1.0}) earns stacks; spam earns zero.
	 *  Callers with no swing (taken hits, tests) must pass {@code 1.0F} explicitly. */
	public void onTrigger(ClassDefinition def, Trigger trigger, long now, float charge) {
		passiveStacks.onTrigger(def, trigger, now, charge);
	}

	/** Spends up to {@code n} stacks through the passive applier's price: the amount actually spent, 0 when the bar is empty. */
	public int spendStacks(ConditionalPassive passive, int n) {
		return passiveStacks.spend(passive, n);
	}

	/**
	 * The tick's bleed: one stack per whole second past the grace, for every passive on
	 * the class. The applier runs this before reconciling, so a drained bar reconciles to
	 * the row it names now and the Onslaught end reads the same count.
	 */
	public void decay(ClassDefinition def, long now) {
		passiveStacks.decay(def, now);
	}

	/** Drops stacks for one class's passives: class switch leaves no stale counts behind. */
	public void clear(ClassDefinition def) {
		passiveStacks.clear(def);
	}
}
