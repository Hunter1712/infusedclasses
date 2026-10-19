package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;

/**
 * Onslaught decisions: the Juggernaut's ultimate on {@code V}. The press needs 10
 * Momentum, arms its own 60s lane and stamps a 15s window; inside it every landed hit
 * deals x1.15 and each full-swing hit spends 2 stacks, and nothing feeds the bar —
 * dealt or taken. The window lasts the bar and the window, whichever empties first: a
 * spend down to zero ends it there and a bleed does too, on the tick that reads the
 * empty bar. Pure gate-and-arm shape on its own lane.
 */
public final class OnslaughtAbility {
	public static final String NAME = "Onslaught";
	/** What arming the window needs: below this the press fizzles. */
	public static final int MIN_STACKS = 10;
	/** What each full-swing hit inside the window spends, for {@link #DEAL_MULT}. */
	public static final int COST_PER_HIT = 2;
	/** Melee dealt mult while the window is live: x1.15, any charge. */
	public static final double DEAL_MULT = 1.15;
	/** Window in ticks: 15 seconds, cut short the moment its bar empties. */
	public static final int WINDOW_TICKS = 300;

	private OnslaughtAbility() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "Needs " + MIN_STACKS + " Momentum: " + (WINDOW_TICKS / 20) + "s (or until the bar empties) of x"
				+ ConditionalPassive.amount(DEAL_MULT) + " melee, spending " + COST_PER_HIT
				+ " per full swing; no Momentum gained while live (V, " + (Lane.ONSLAUGHT.cooldownMs() / 1000) + "s).";
	}

	/**
	 * Decides pressing for Juggernauts holding the price off cooldown; fewer than
	 * {@link #MIN_STACKS} fizzles before the cooldown is even read. Pure and read-only:
	 * only {@link #fire} arms the lane.
	 */
	public static AbilityOutcome trigger(ClassDefinition def, long onslaughtRemaining, int stacks) {
		if (def == null || !def.hasAbility(ClassRegistry.ONSLAUGHT_ABILITY)) {
			return AbilityOutcome.rejected();
		}
		if (stacks < MIN_STACKS) {
			return AbilityOutcome.fizzled(new ChatLine("No Momentum", null));
		}
		if (onslaughtRemaining > 0) {
			return AbilityOutcome.rejected();
		}
		return AbilityOutcome.fired(new ChatLine(NAME + "!", ChatFormatting.RED));
	}

	/**
	 * Arms the lane and stamps the 15s window together on a fired press; rejected
	 * presses leave both untouched. The arm reads the same lane and now the trigger
	 * just checked, so it cannot fail.
	 */
	public static AbilityOutcome fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		ConditionalPassive momentum = def == null ? null : def.passiveForTrigger(Trigger.ON_HIT_DEALT);
		int stacks = momentum == null ? 0 : runtime.stacks(momentum);
		AbilityOutcome result = trigger(def, runtime.state(now).laneRemaining(Lane.ONSLAUGHT), stacks);
		if (result.fired()) {
			runtime.arm(Lane.ONSLAUGHT, now, WINDOW_TICKS * 50L);
		}
		return result;
	}

	/** Whether the window is live at all: the raw lane read, regardless of the bar. */
	public static boolean windowLive(PlayerRuntime.State state) {
		return state != null && state.lane(Lane.ONSLAUGHT).windowRemainingMs() > 0;
	}

	/**
	 * Whether the window prices a hit: live, and its bar still holds a stack to spend.
	 * The plan reads this so a bar the world drained to zero prices nothing, even before
	 * the end-if-drained tick lands.
	 */
	public static boolean liveFor(PlayerRuntime.State state, int stacks) {
		return windowLive(state) && stacks > 0;
	}

	/** Melee dealt mult while the window is live with a stack left: x1.15 any charge, 1.0 otherwise. */
	public static double dealMult(PlayerRuntime.State state, int stacks) {
		return liveFor(state, stacks) ? DEAL_MULT : 1.0;
	}

	/**
	 * The early end: the bar is the window's other half, so an empty bar ends it wherever
	 * it emptied — a hit's spend, a bleed, or another ability's price. Called once per
	 * tick after the bleed, so the lane read the HUD draws and the stack read agree.
	 * Returns whether this call ended a live window, so the tick can re-read state before
	 * it snapshots instead of drawing a rider for a window that already ended.
	 */
	public static boolean endIfDrained(ClassDefinition def, PlayerRuntime runtime, PlayerRuntime.State state) {
		if (def == null || !windowLive(state)) {
			return false;
		}
		ConditionalPassive momentum = def.passiveForTrigger(Trigger.ON_HIT_DEALT);
		if (momentum == null || runtime.stacks(momentum) > 0) {
			return false;
		}
		endIfDrained(runtime, momentum);
		return true;
	}

	/**
	 * The same early end for a caller that already holds the bar — the hit settle spends
	 * the last stacks there. Package-private so the rule has one home.
	 */
	static void endIfDrained(PlayerRuntime runtime, ConditionalPassive momentum) {
		if (momentum != null && runtime.stacks(momentum) <= 0) {
			runtime.endWindow(Lane.ONSLAUGHT);
		}
	}
}
