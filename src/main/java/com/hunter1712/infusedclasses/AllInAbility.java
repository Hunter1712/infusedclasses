package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

/**
 * All In decisions: the Gambler's ultimate on {@code V}. Pure gate-and-arm shape on its
 * own 75s lane: fires only for Gamblers, arming the lane and a 15s window together.
 * While the window is live the crit multiplier reads {@link #CRIT_MULT} instead of the
 * rule's own; betrayal, Streak resets and every band stay exactly as they are.
 */
public final class AllInAbility {
	public static final String NAME = "All In";
	/** Window in ticks: 15 seconds, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 300;
	/** Crit multiplier while live: what a Gambler's x1.5 becomes for the window. */
	public static final double CRIT_MULT = 2.0;
	/** Cast sound id, resolved by the adapter; the beacon swell is the commitment. */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("block.beacon.activate");

	private AllInAbility() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "Crits x" + ConditionalPassive.amount(CRIT_MULT) + " for " + (WINDOW_TICKS / 20)
				+ "s (V, " + (Lane.ALL_IN.cooldownMs() / 1000) + "s). Dice rules unchanged.";
	}

	/**
	 * Decides firing for Gamblers off their lane, its fired line gold. Pure and
	 * read-only: only {@link #fire} arms the lane and the window.
	 */
	public static AbilityOutcome trigger(ClassDefinition def, long allInRemaining) {
		if (def == null || !def.hasAbility(ClassRegistry.ALL_IN_ABILITY)) {
			return AbilityOutcome.rejected();
		}
		if (allInRemaining > 0) {
			return AbilityOutcome.rejected();
		}
		return AbilityOutcome.fired(new ChatLine(NAME + "!", ChatFormatting.GOLD));
	}

	/**
	 * Arms the lane and a 15s window together on a fired press; rejected presses leave
	 * both untouched. The arm reads the same lane and now the trigger just checked,
	 * so it cannot fail.
	 */
	public static AbilityOutcome fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		AbilityOutcome result = trigger(def, runtime.state(now).laneRemaining(Lane.ALL_IN));
		if (result.fired()) {
			runtime.arm(Lane.ALL_IN, now, WINDOW_TICKS * 50L);
		}
		return result;
	}
}
