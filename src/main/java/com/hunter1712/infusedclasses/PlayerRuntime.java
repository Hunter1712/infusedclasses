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

	/** Current Legionary stance, SCUTUM until the first Rotate. */
	public RotateAbility.Stance stance() {
		return stance;
	}

	/** Flips the stance and stamps the flip record: Rotate's fire lands here with the lane arm. */
	public void flipStance(long now, RotateAbility.Stance landing) {
		stance = landing;
		flippedTo = landing;
		flipWindow.arm(now);
	}

	// Joint arms: lane plus window stamped together, so a fired press can never arm
	// one without the other. This coordination is the module's own job; the lane map
	// itself stays hidden inside Cooldowns.
	/** Arms the rotate lock: fired Rotate presses only. The flip rides with it. */
	public boolean tryUseRotate(long now) {
		return cooldowns.useRotate(now);
	}

	public long rotateRemaining(long now) {
		return cooldowns.rotateRemaining(now);
	}

	public long oathRemaining(long now) {
		return cooldowns.oathRemaining(now);
	}

	/** Arms the shared oath lane and stamps the Aequilibrium window together. */
	public boolean tryUseAequilibrium(long now) {
		if (!cooldowns.useOath(now)) {
			return false;
		}
		aequilibriumWindow.arm(now);
		return true;
	}

	/** Whether a fired Aequilibrium window is still live. Persistent: peeked while live, never consumed. Expiry is silent. */
	public boolean isAequilibriumLive(long now) {
		return aequilibriumWindow.isLive(AequilibriumAbility.WINDOW_TICKS * 50L, now);
	}

	/**
	 * Peeks the Impetus flip buff without consuming: what the melee plan decides on.
	 * Armed by flipping to Gladius, live 5s, one-shot through the pending flag.
	 */
	public boolean hasImpetusPending(long now) {
		return flippedTo == RotateAbility.Stance.GLADIUS
				&& flipWindow.hasPending(RotateAbility.SWITCH_DEAL_MS, now);
	}

	/**
	 * Consumes the Impetus flip buff exactly once: true when a pending buff was waiting,
	 * false when nothing was owed. An unswung flip simply stops being live.
	 */
	public boolean consumeImpetusBonus(long now) {
		return flippedTo == RotateAbility.Stance.GLADIUS
				&& flipWindow.consume(RotateAbility.SWITCH_DEAL_MS, now);
	}

	/**
	 * Whether the Testudo flip catch is still live: flipped to Scutum inside 1.5s.
	 * Timed, never consumed: every hit inside the catch is discounted. Expiry is silent.
	 */
	public boolean isTestudoBuffLive(long now) {
		return flippedTo == RotateAbility.Stance.SCUTUM
				&& flipWindow.isLive(RotateAbility.SWITCH_TAKE_MS, now);
	}

	// Lane reads: mechanical forwards over the hidden Cooldowns, so ability decisions
	// never touch the lane map. No coordination lives here, only remaining-time answers.
	/** Cooldown verbs: arming runs through here; the lanes stay the module's implementation. */
	public long rushRemaining(long now) {
		return cooldowns.rushRemaining(now);
	}

	/** Arms the Rush lane and stamps the engage window together: fired presses only. */
	public boolean tryUseRush(long now) {
		if (!cooldowns.useRush(now)) {
			return false;
		}
		rushWindow.arm(now);
		return true;
	}

	/**
	 * Consumes the Rush next-hit bonus exactly once: true when a pending bonus was waiting,
	 * false when nothing was owed. A stale window with no bonus left grants nothing.
	 */
	public boolean consumeRushBonus(long now) {
		return rushWindow.consume(RushAbility.WINDOW_TICKS * 50L, now);
	}

	/** Peeks the Rush bonus without consuming: what the melee plan decides on. */
	public boolean hasRushBonusPending(long now) {
		return rushWindow.hasPending(RushAbility.WINDOW_TICKS * 50L, now);
	}

	/** Whether a fired Rush window is still live: the stride aura rides it, spent bonus or not. */
	public boolean isRushLive(long now) {
		return rushWindow.isLive(RushAbility.WINDOW_TICKS * 50L, now);
	}

	public long rendRemaining(long now) {
		return cooldowns.rendRemaining(now);
	}

	/**
	 * Arms the Rend lane and stamps the empower snapshot together: fired presses only.
	 * The snapshot is what the next hit inside the window is worth; the stacks themselves
	 * are consumed separately by the entry through the passive applier.
	 */
	public boolean tryUseRend(long now, int stacks) {
		if (!cooldowns.useRend(now)) {
			return false;
		}
		rendWindow.arm(now, stacks);
		return true;
	}

	/** Peeks the empower snapshot without consuming: what the melee plan decides on. -1 when nothing is owed. */
	public int peekRendEmpowerStacks(long now) {
		return rendWindow.peekPayload(RendAbility.WINDOW_TICKS * 50L, now);
	}

	/**
	 * Spends the Rend empower exactly once: the stamped stack snapshot when a live empower
	 * was waiting, -1 when nothing was owed. An unswung window simply stops being live;
	 * the stacks it cost are already gone.
	 */
	public int consumeRendEmpower(long now) {
		return rendWindow.consumePayload(RendAbility.WINDOW_TICKS * 50L, now);
	}

	/** Reset for class switch: a new class starts empty. The adapter calls this, not ability callers. */
	public void resetCooldowns() {
		cooldowns.reset();
		for (Window w : new Window[]{rushWindow, rendWindow, aequilibriumWindow, flipWindow}) {
			w.clear();
		}
		flippedTo = null;
		stance = RotateAbility.Stance.SCUTUM;
	}

	/**
	 * One ability window: a press timestamp plus its one-shot payload, live for the ability's
	 * window after a fired press. Cleared with everything else, so switch and respawn never
	 * inherit a window. One clock throughout: durations arrive in millis, so tick windows
	 * convert at their call sites and flip windows pass their millis constants straight in.
	 */
	private static final class Window {
		private long pressedAt = -1L;
		private boolean pending;
		private int payload;

		boolean isLive(long windowMs, long now) {
			return pressedAt >= 0 && now - pressedAt < windowMs;
		}

		void arm(long now) {
			pressedAt = now;
			pending = true;
		}

		void arm(long now, int payload) {
			pressedAt = now;
			pending = true;
			this.payload = payload;
		}

		boolean hasPending(long windowMs, long now) {
			return pending && isLive(windowMs, now);
		}

		boolean consume(long windowMs, long now) {
			if (!hasPending(windowMs, now)) {
				return false;
			}
			pending = false;
			return true;
		}

		int peekPayload(long windowMs, long now) {
			return hasPending(windowMs, now) ? payload : -1;
		}

		int consumePayload(long windowMs, long now) {
			int seen = peekPayload(windowMs, now);
			if (seen < 0) {
				return -1;
			}
			pending = false;
			return seen;
		}

		void clear() {
			pressedAt = -1L;
			pending = false;
			payload = 0;
		}
	}

	/** Current stacks for a passive, 0 when never triggered. */
	public int stacks(ConditionalPassive passive) {
		return passiveStacks.stacks(passive);
	}

	/** Last-trigger millis for a passive, null when never triggered. Decay counts from it. */
	public Long lastTrigger(ConditionalPassive passive) {
		return passiveStacks.lastTrigger(passive);
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
