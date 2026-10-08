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
	// Rush window: press timestamp (millis), live for the ability's window after a fired
	// press, plus a one-shot bonus flag for the next qualifying melee hit. Cleared with
	// everything else, so switch and respawn never inherit a window.
	private final Window rushWindow = new Window();
	private boolean rushBonusPending;
	// Rend empower: stack snapshot (bonus damage) stamped on a fired press, live for the
	// ability's window, spent by the next qualifying melee hit. Cleared with everything
	// else, so switch and respawn never inherit an empower.
	private final Window rendWindow = new Window();
	private int rendEmpowerStacks;
	private boolean rendEmpowerPending;
	// Conditional-passive stacks: counts plus grace timestamps, keyed by the passive
	// definition, never by its name.
	private final PassiveStacks passiveStacks = new PassiveStacks();
	// Legionary form: transient per ADR-0002, every Legionary enters in Scutum.
	// The choice stays in PlayerClasses; this is where the body is, not the pick.
	private RotateAbility.Stance stance = RotateAbility.Stance.SCUTUM;
	// Oath windows: press timestamps (millis), each live 6s after its fired
	// press. Milk-proof (timestamps, not effects); death ends them with
	// everything else, so switch and respawn never inherit a window.
	private final Window impetusWindow = new Window();
	private final Window testudoWindow = new Window();

	/** Current Legionary stance, SCUTUM until the first Rotate. */
	public RotateAbility.Stance stance() {
		return stance;
	}

	/** Flips the stance: Rotate's fire lands here with the lane arm. */
	public void flipStance() {
		stance = RotateAbility.flipped(stance);
	}

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

	/** Arms the shared oath lane and stamps the Impetus window together. */
	public boolean tryUseImpetus(long now) {
		return armOath(now, true);
	}

	/** Arms the shared oath lane and stamps the Testudo window together. */
	public boolean tryUseTestudo(long now) {
		return armOath(now, false);
	}

	/** Single lane-arm for both oath windows: firing one locks the other. */
	private boolean armOath(long now, boolean impetus) {
		if (!cooldowns.useOath(now)) {
			return false;
		}
		if (impetus) {
			impetusWindow.arm(now);
		} else {
			testudoWindow.arm(now);
		}
		return true;
	}

	/** Whether a fired Impetus window is still live. Expiry is silent. */
	public boolean isImpetusLive(long now) {
		return impetusWindow.isLive(ImpetusAbility.WINDOW_TICKS, now);
	}

	/** Whether a fired Testudo window is still live. Expiry is silent. */
	public boolean isTestudoLive(long now) {
		return testudoWindow.isLive(TestudoAbility.WINDOW_TICKS, now);
	}

	public Cooldowns cooldowns() {
		return cooldowns;
	}

	/** Cooldown verbs: arming runs through here, so no caller reaches through the mutable object. */
	public long rushRemaining(long now) {
		return cooldowns.rushRemaining(now);
	}

	/** Arms the Rush lane and stamps the engage window together: fired presses only. */
	public boolean tryUseRush(long now) {
		if (!cooldowns.useRush(now)) {
			return false;
		}
		rushWindow.arm(now);
		rushBonusPending = true;
		return true;
	}

	/** Whether a fired Rush window is still live: press timestamp plus the ability's window. */
	public boolean isRushLive(long now) {
		return rushWindow.isLive(RushAbility.WINDOW_TICKS, now);
	}

	/**
	 * Consumes the Rush next-hit bonus exactly once: true when a pending bonus was waiting,
	 * false when nothing was owed. A stale window with no bonus left grants nothing.
	 */
	public boolean consumeRushBonus(long now) {
		if (!rushBonusPending || !isRushLive(now)) {
			return false;
		}
		rushBonusPending = false;
		return true;
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
		rendWindow.arm(now);
		rendEmpowerStacks = stacks;
		rendEmpowerPending = true;
		return true;
	}

	/** Whether a fired Rend empower is still live: press timestamp plus the ability's window. */
	public boolean isRendEmpowerLive(long now) {
		return rendEmpowerPending && rendWindow.isLive(RendAbility.WINDOW_TICKS, now);
	}

	/**
	 * Spends the Rend empower exactly once: the stamped stack snapshot when a live empower
	 * was waiting, -1 when nothing was owed. An unswung window simply stops being live;
	 * the stacks it cost are already gone.
	 */
	public int consumeRendEmpower(long now) {
		if (!isRendEmpowerLive(now)) {
			return -1;
		}
		rendEmpowerPending = false;
		return rendEmpowerStacks;
	}

	/** Reset for class switch: a new class starts empty. The adapter calls this, not ability callers. */
	public void resetCooldowns() {
		cooldowns.reset();
		rushWindow.clear();
		rushBonusPending = false;
		rendWindow.clear();
		rendEmpowerStacks = 0;
		rendEmpowerPending = false;
		stance = RotateAbility.Stance.SCUTUM;
		impetusWindow.clear();
		testudoWindow.clear();
	}

	/**
	 * One ability window: a press timestamp, live for the ability's window after a fired
	 * press. Cleared with everything else, so switch and respawn never inherit a window.
	 * Ticks ride as 50ms each.
	 */
	private static final class Window {
		private long pressedAt = -1L;

		boolean isLive(int windowTicks, long now) {
			return pressedAt >= 0 && now - pressedAt < windowTicks * 50L;
		}

		void arm(long now) {
			pressedAt = now;
		}

		void clear() {
			pressedAt = -1L;
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
	 *  Unknown triggers, a missing class and trigger-less classes are inert. */
	public void onTrigger(ClassDefinition def, String trigger, long now) {
		passiveStacks.onTrigger(def, trigger, now);
	}

	/**
	 * Charged bump from the melee hook: only a full swing ({@code charge == 1.0})
	 * earns stacks. Spam earns zero; the uncharged overload reads as full.
	 */
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
}
