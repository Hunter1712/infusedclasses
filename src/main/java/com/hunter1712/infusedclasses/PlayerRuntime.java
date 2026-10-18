package com.hunter1712.infusedclasses;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Transient per-player state: conditional-passive stacks and ability cooldowns. Nothing here is
 * persisted or copied, so all of it dies with the session. The server entry holds one per player
 * and owns that lifetime: created on first use, empty again after a respawn, dropped on logout.
 *
 * Time arrives as a parameter and the class whose passives these are is passed in too, so this
 * module holds no clock and no reference to anyone's choice. Stack storage (counts, grace
 * timestamps, decay) lives in {@link PassiveStacks}; this module coordinates it with cooldowns
 * and ability windows. Stacks, the cap, the grace period and the decay rate are the passive's
 * own facts, read off the definition it is handed, which is also what a stack is keyed by:
 * a passive is addressed by what it is, not by its name.
 *
 * <p>Ability state is keyed by {@link Lane}: one entry per lane, holding the cooldown
 * ({@link Cooldowns}) and the lane's window. Each ability owns its window constant and passes it
 * in where it arms, so this module never spells a lane's duration and a seventh window adds a
 * {@link Lane} value, not a field.
 *
 * <p>Reads cross one seam: {@link #state} answers the whole transient read at a {@code now},
 * so the status paths (the snapshot builder, {@code /class info}, the melee plan) take one
 * shape and one clock. Everything below it mutates.
 */
public final class PlayerRuntime {
	// ponytail: cooldowns reset on relog, persist to NBT if the reset becomes an exploit.
	private final Cooldowns cooldowns = new Cooldowns();
	// Ability windows: press timestamps plus their one-shot payloads, one per lane. A window
	// carries the duration stamped beside it at arm time (the firing ability's own constant,
	// passed in) and its one-shot: the melee plan peeks it and the hit settle spends it, so
	// each bonus lands at most once. Windows nothing spends (Aequilibrium, All In, Roulette's
	// outcome) are peeked every tick of the applier instead. Cleared with everything else, so
	// switch and respawn never inherit a window.
	private final EnumMap<Lane, Window> windows = freshWindows();
	// Conditional-passive stacks: counts plus grace timestamps, keyed by the passive
	// definition, never by its name.
	private final PassiveStacks passiveStacks = new PassiveStacks();
	// Legionary form: transient per ADR-0002, every Legionary enters in Scutum.
	// The choice stays in PlayerClasses; this is where the body is, not the pick.
	private RotateAbility.Stance stance = RotateAbility.Stance.SCUTUM;
	// Flip record: the last Rotate flip's landing, read beside the rotate lane's window on the
	// same millis clock as every ability window. The Impetus flip buff peeks it (landed on
	// Gladius inside the window, one-shot through the window's pending flag) and the Testudo
	// buff peeks it (landed on Scutum, timed, never consumed). Stamped by rotate, cleared with
	// everything else. Pending is only ever read under a Gladius landing, so flipping to Scutum
	// needs no extra clear.
	private RotateAbility.Stance flippedTo = null;
	// Roulette's landed band, read beside the roulette lane's window: the passive applier peeks
	// it every tick, the melee plan reads its deal mult, nothing spends it. Null when no spin's
	// window is live.
	private RouletteAbility.Outcome rouletteOutcome = null;
	// Streak: the Gambler's loaded pocket, bumped and reset by the hit settle and
	// nothing else. Transient like every earned count.
	private int streak = 0;

	private static EnumMap<Lane, Window> freshWindows() {
		EnumMap<Lane, Window> map = new EnumMap<>(Lane.class);
		for (Lane lane : Lane.values()) {
			map.put(lane, new Window());
		}
		return map;
	}

	/**
	 * The whole transient read at {@code now}: the one shape every status path crosses
	 * this seam with, so lane and window reads cannot re-state their semantics per
	 * caller. One {@link LaneRead} per lane; {@code flipTarget} is the live flip window's
	 * landing, null when no flip window is live; {@code rendBonusStacks} is the unspent
	 * Rend stamp, 0 when nothing is owed; {@code stance} is Scutum until the first Rotate.
	 */
	public record State(
			Map<Lane, LaneRead> lanes,
			int rendBonusStacks,
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
	}

	/** One clock, one shape: every fact the status paths read, taken at the same {@code now}. */
	public State state(long now) {
		EnumMap<Lane, LaneRead> lanes = new EnumMap<>(Lane.class);
		for (Lane lane : Lane.values()) {
			Window window = windows.get(lane);
			lanes.put(lane, new LaneRead(
					cooldowns.remaining(lane, now),
					window.remainingMs(now),
					oneShot(lane, window, now)));
		}
		return new State(
				lanes,
				windows.get(Lane.REND).peekPayload(now),
				windows.get(Lane.ROTATE).isLive(now) ? flippedTo : null,
				windows.get(Lane.ROULETTE).isLive(now) ? rouletteOutcome : null,
				stance,
				streak);
	}

	/**
	 * The lane's one-shot read at {@code now}: its window's pending flag, except Rotate's,
	 * whose one-shot is the Impetus buff — only a flip landed on Gladius carries one, so
	 * the raw window flag (set by every arm) is never the read its consumers want.
	 */
	private boolean oneShot(Lane lane, Window window, long now) {
		return window.hasPending(now)
				&& (lane != Lane.ROTATE || flippedTo == RotateAbility.Stance.GLADIUS);
	}

	// Joint arms: lane plus window stamped together, so a fired press can never arm one
	// without the other. The duration is the firing ability's own constant, passed in at
	// its arm site (or, for the two landings that carry one, named by the landing); the
	// trigger read the same lane at the same now, so the use here cannot fail for a fired press.
	/** Arms the lane and stamps its window together: every fired press whose window carries nothing else. */
	public boolean arm(Lane lane, long now, long windowMs) {
		return arm(lane, now, windowMs, 0);
	}

	/** Arms the lane and stamps the window's one-shot payload together: fired Rend presses. */
	public boolean arm(Lane lane, long now, long windowMs, int payload) {
		if (!cooldowns.use(lane, now)) {
			return false;
		}
		windows.get(lane).arm(now, windowMs, payload);
		return true;
	}

	/** Arms the rotate lock and flips to the landing together: fired Rotate presses only. */
	public boolean rotate(long now, RotateAbility.Stance landing) {
		long windowMs = landing == RotateAbility.Stance.GLADIUS
				? RotateAbility.SWITCH_DEAL_MS : RotateAbility.SWITCH_TAKE_MS;
		if (!arm(Lane.ROTATE, now, windowMs)) {
			return false;
		}
		stance = landing;
		flippedTo = landing;
		return true;
	}

	/** Arms the Roulette lane and records the landed band together: fired spins only. */
	public boolean roulette(long now, RouletteAbility.Outcome outcome) {
		if (!arm(Lane.ROULETTE, now, RouletteAbility.WINDOW_TICKS * 50L)) {
			return false;
		}
		rouletteOutcome = outcome;
		return true;
	}

	/**
	 * Consumes the one-shot a lane's window carries, exactly once: true when one was waiting,
	 * false when nothing was owed. Rush's next-hit credit and Rotate's Impetus both spend
	 * through here; a stale window with no bonus left grants nothing, and an unswung window
	 * simply stops being live.
	 */
	public boolean consumeBonus(Lane lane, long now) {
		return windows.get(lane).consume(now);
	}

	/**
	 * Spends a lane's stamped payload exactly once: the stamped value when a live window was
	 * waiting, 0 when nothing was owed. An unswung window simply stops being live; what it
	 * cost is already gone.
	 */
	public int consumePayload(Lane lane, long now) {
		return windows.get(lane).consumePayload(now);
	}

	/** One non-crit full hit loads the pocket: called by the hit settle with the plan's movement. */
	public void bumpStreak() {
		streak++;
	}

	/** A landed crit spends the pocket: called by the hit settle with the plan's movement. */
	public void resetStreak() {
		streak = 0;
	}

	/**
	 * Reset for class switch and respawn: every lane, window, landing, the stance and the
	 * Streak start empty. The adapter calls this, not ability callers.
	 */
	public void resetAll() {
		cooldowns.reset();
		for (Window window : windows.values()) {
			window.clear();
		}
		flippedTo = null;
		rouletteOutcome = null;
		streak = 0;
		stance = RotateAbility.Stance.SCUTUM;
	}

	/**
	 * One ability window: a press timestamp, the duration stamped beside it at arm time,
	 * and its one-shot payload, live for that duration after a fired press. Cleared with
	 * everything else, so switch and respawn never inherit a window. One clock throughout:
	 * each ability names its window constant once, where it arms.
	 */
	private static final class Window {
		private long pressedAt = -1L;
		private long durationMs;
		private boolean pending;
		private int payload;

		boolean isLive(long now) {
			return pressedAt >= 0 && now - pressedAt < durationMs;
		}

		long remainingMs(long now) {
			if (!isLive(now)) {
				return 0L;
			}
			return durationMs - (now - pressedAt);
		}

		void arm(long now, long durationMs, int payload) {
			pressedAt = now;
			this.durationMs = durationMs;
			pending = true;
			this.payload = payload;
		}

		boolean hasPending(long now) {
			return pending && isLive(now);
		}

		boolean consume(long now) {
			if (!hasPending(now)) {
				return false;
			}
			pending = false;
			return true;
		}

		int peekPayload(long now) {
			return hasPending(now) ? payload : 0;
		}

		int consumePayload(long now) {
			if (!hasPending(now)) {
				return 0;
			}
			pending = false;
			return payload;
		}

		void clear() {
			pressedAt = -1L;
			durationMs = 0;
			pending = false;
			payload = 0;
		}
	}

	/** Current stacks for a passive, 0 when never triggered. */
	public int stacks(ConditionalPassive passive) {
		return passiveStacks.stacks(passive);
	}

	/** Millis left before this passive's stacks drop, 0 when it holds none. Exactly at grace is still live. */
	public long graceRemaining(ConditionalPassive passive, long now) {
		return passiveStacks.graceRemaining(passive, now);
	}

	/** The grace clock's last stamp for this passive, 0 when it holds none. Compared for movement, never read as time. */
	public long graceStamp(ConditionalPassive passive) {
		return passiveStacks.graceStamp(passive);
	}

	/** Bumps every passive on the given class whose trigger matches.
	 *  Unknown triggers, a missing class and trigger-less classes are inert.
	 *  Only a full swing ({@code charge == 1.0}) earns stacks; spam earns zero.
	 *  Callers with no swing (bonus grants, tests) must pass {@code 1.0F} explicitly. */
	public void onTrigger(ClassDefinition def, Trigger trigger, long now, float charge) {
		passiveStacks.onTrigger(def, trigger, now, charge);
	}

	/**
	 * Per-passive movement: which passives lost stacks this tick. The applier needs the set,
	 * not the global flag: a tick that decayed passive A must not remove passive B sitting at
	 * zero. Unmodifiable, possibly empty, never null. Past grace every stack drops at once,
	 * vanilla-style: there is no per-second bleed.
	 */
	public Set<ConditionalPassive> tickDecayMoved(ClassDefinition def, long now) {
		return passiveStacks.tickDecayMoved(def, now);
	}

	/** Drops stacks for one class's passives: class switch leaves no stale counts behind. */
	public void clear(ClassDefinition def) {
		passiveStacks.clear(def);
	}

	/** Drops one passive's stacks and timestamp: a spender consumes only what it priced. */
	public void clear(ConditionalPassive passive) {
		passiveStacks.clear(passive);
	}

	/** Bumps one passive record N times: single cap, single grace stamp. Used by Rush bonus grant. */
	public void bumpPassive(ConditionalPassive passive, int times, long now) {
		passiveStacks.bumpPassive(passive, times, now);
	}
}
