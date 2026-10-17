package com.hunter1712.infusedclasses;

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
 * <p>Reads cross one seam: {@link #state} answers the whole transient read at a {@code now},
 * so the status paths (the snapshot builder, {@code /class info}, the melee plan) take one
 * shape and one clock. Everything below it mutates.
 */
public final class PlayerRuntime {
	// ponytail: cooldowns reset on relog, persist to NBT if the reset becomes an exploit.
	private final Cooldowns cooldowns = new Cooldowns();
	// Ability windows: press timestamps plus their one-shot payloads. Rush carries a bare
	// pending bonus, Rend a pending stack snapshot. One-shot: the melee plan peeks them
	// and the hit settle spends them, so each bonus lands at most once. Cleared with
	// everything else, so switch and respawn never inherit a window.
	private final Window rushWindow = new Window();
	private final Window rendWindow = new Window();
	// Conditional-passive stacks: counts plus grace timestamps, keyed by the passive
	// definition, never by its name.
	private final PassiveStacks passiveStacks = new PassiveStacks();
	// Legionary form: transient per ADR-0002, every Legionary enters in Scutum.
	// The choice stays in PlayerClasses; this is where the body is, not the pick.
	private RotateAbility.Stance stance = RotateAbility.Stance.SCUTUM;
	// Flip record: the last Rotate flip as a window-family member on the same millis
	// clock as every ability window. The Impetus flip buff peeks it (flipped to Gladius
	// inside 5s, one-shot through the window's pending flag) and the Testudo buff peeks
	// it (flipped to Scutum inside 1.5s, timed, never consumed). Stamped by flipStance,
	// cleared with everything else. Pending is only ever read under a Gladius landing,
	// so flipping to Scutum needs no extra clear.
	private final Window flipWindow = new Window();
	private RotateAbility.Stance flippedTo = null;
	// Aequilibrium window: press timestamp (millis), live 6s after its fired press.
	// Persistent, never consumed: every tick inside a live window wears the aura, so the
	// applier peeks it with isLive and nothing spends it. Milk-proof (timestamps, not
	// effects); death ends it with everything else, so switch and respawn never
	// inherit a window.
	private final Window aequilibriumWindow = new Window();
	// Roulette outcome window: press timestamp plus the spun Outcome beside it, live
	// 10s after a fired spin. Persistent like Aequilibrium's: the passive applier
	// peeks the outcome every tick, the melee plan reads its deal mult, nothing spends
	// it. All In window: the ultimate's 15s stamp, same persistent shape. Both cleared
	// with everything else, so no stale dice survive a switch or respawn.
	private final Window rouletteWindow = new Window();
	private RouletteAbility.Outcome rouletteOutcome = null;
	private final Window allInWindow = new Window();
	// Streak: the Gambler's loaded pocket, bumped and reset by the hit settle and
	// nothing else. Transient like every earned count.
	private int streak = 0;

	/**
	 * The whole transient read at {@code now}: the one shape every status path crosses
	 * this seam with, so lane and window reads cannot re-state their semantics per
	 * caller. {@code flipTarget} is the live flip window's landing, null when no flip
	 * window is live; {@code rendBonusStacks} is the unspent Rend stamp, 0 when nothing
	 * is owed; {@code stance} is Scutum until the first Rotate.
	 */
	public record State(
			long rushRemaining,
			long rendRemaining,
			long rotateRemaining,
			long oathRemaining,
			long rouletteRemaining,
			long allInRemaining,
			long rushWindowRemaining,
			boolean rushBonusPending,
			long rendWindowRemaining,
			int rendBonusStacks,
			long flipWindowRemaining,
			RotateAbility.Stance flipTarget,
			boolean impetusPending,
			long rouletteWindowRemaining,
			RouletteAbility.Outcome rouletteOutcome,
			long aequilibriumWindowRemaining,
			long allInWindowRemaining,
			RotateAbility.Stance stance,
			int streak) {
	}

	/** One clock, one shape: every fact the status paths read, taken at the same {@code now}. */
	public State state(long now) {
		return new State(
				cooldowns.rushRemaining(now),
				cooldowns.rendRemaining(now),
				cooldowns.rotateRemaining(now),
				cooldowns.oathRemaining(now),
				cooldowns.rouletteRemaining(now),
				cooldowns.allInRemaining(now),
				rushWindow.remainingMs(now),
				rushWindow.hasPending(now),
				rendWindow.remainingMs(now),
				rendWindow.peekPayload(now),
				flipWindow.remainingMs(now),
				flipWindow.isLive(now) ? flippedTo : null,
				flippedTo == RotateAbility.Stance.GLADIUS && flipWindow.hasPending(now),
				rouletteWindow.remainingMs(now),
				rouletteWindow.isLive(now) ? rouletteOutcome : null,
				aequilibriumWindow.remainingMs(now),
				allInWindow.remainingMs(now),
				stance,
				streak);
	}

	/** Flips the stance and stamps the flip record: the joint rotate arm lands here, tests stamp states. */
	void flipStance(long now, RotateAbility.Stance landing) {
		stance = landing;
		flippedTo = landing;
		long windowMs = landing == RotateAbility.Stance.GLADIUS
				? RotateAbility.SWITCH_DEAL_MS : RotateAbility.SWITCH_TAKE_MS;
		flipWindow.arm(now, windowMs);
	}

	// Joint arms: lane plus window stamped together, so a fired press can never arm
	// one without the other. This coordination is the module's own job; the lane map
	// itself stays hidden inside Cooldowns.
	/** Arms the rotate lock and flips to the landing together: fired Rotate presses only. */
	public boolean tryUseRotate(long now, RotateAbility.Stance landing) {
		if (!cooldowns.useRotate(now)) {
			return false;
		}
		flipStance(now, landing);
		return true;
	}

	/** Arms the Roulette lane and stamps the outcome window together: fired spins only. */
	public boolean tryUseRoulette(long now, RouletteAbility.Outcome outcome) {
		if (!cooldowns.useRoulette(now)) {
			return false;
		}
		rouletteWindow.arm(now, RouletteAbility.WINDOW_TICKS * 50L);
		rouletteOutcome = outcome;
		return true;
	}

	/** Arms the All In lane and stamps its window together: fired presses only. */
	public boolean tryUseAllIn(long now) {
		if (!cooldowns.useAllIn(now)) {
			return false;
		}
		allInWindow.arm(now, AllInAbility.WINDOW_TICKS * 50L);
		return true;
	}

	/** Arms the shared oath lane and stamps the Aequilibrium window together. */
	public boolean tryUseAequilibrium(long now) {
		if (!cooldowns.useOath(now)) {
			return false;
		}
		aequilibriumWindow.arm(now, AequilibriumAbility.WINDOW_TICKS * 50L);
		return true;
	}

	/** Arms the Rush lane and stamps the engage window together: fired presses only. */
	public boolean tryUseRush(long now) {
		if (!cooldowns.useRush(now)) {
			return false;
		}
		rushWindow.arm(now, RushAbility.WINDOW_TICKS * 50L);
		return true;
	}

	/**
	 * Arms the Rend lane and stamps the window's bonus together: fired presses only.
	 * The stamped stack count is what the next hit inside the window is worth; the stacks
	 * themselves are consumed separately by the entry through the passive applier.
	 */
	public boolean tryUseRend(long now, int stacks) {
		if (!cooldowns.useRend(now)) {
			return false;
		}
		rendWindow.arm(now, RendAbility.WINDOW_TICKS * 50L, stacks);
		return true;
	}

	/**
	 * Consumes the Impetus flip buff exactly once: true when a pending buff was waiting,
	 * false when nothing was owed. An unswung flip simply stops being live.
	 */
	public boolean consumeImpetusBonus(long now) {
		return flippedTo == RotateAbility.Stance.GLADIUS
				&& flipWindow.consume(now);
	}

	/**
	 * Consumes the Rush next-hit bonus exactly once: true when a pending bonus was waiting,
	 * false when nothing was owed. A stale window with no bonus left grants nothing.
	 */
	public boolean consumeRushBonus(long now) {
		return rushWindow.consume(now);
	}

	/**
	 * Spends the Rend bonus exactly once: the stamped stacks when a live window was
	 * waiting, 0 when nothing was owed. An unswung window simply stops being live;
	 * the stacks it cost are already gone.
	 */
	public int consumeRendBonus(long now) {
		return rendWindow.consumePayload(now);
	}

	/** One non-crit full hit loads the pocket: called by the hit settle with the plan's movement. */
	public void bumpStreak() {
		streak++;
	}

	/** A landed crit spends the pocket: called by the hit settle with the plan's movement. */
	public void resetStreak() {
		streak = 0;
	}

	/** Reset for class switch: a new class starts empty. The adapter calls this, not ability callers. */
	public void resetCooldowns() {
		cooldowns.reset();
		for (Window w : new Window[]{rushWindow, rendWindow, aequilibriumWindow, flipWindow, rouletteWindow, allInWindow}) {
			w.clear();
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

		void arm(long now, long durationMs) {
			pressedAt = now;
			this.durationMs = durationMs;
			pending = true;
			payload = 0;
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
	public void onTrigger(ClassDefinition def, String trigger, long now, float charge) {
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
