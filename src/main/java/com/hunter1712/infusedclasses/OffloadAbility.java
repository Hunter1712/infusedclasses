package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Offload decisions: the Juggernaut's spender on {@code R}. Pure gate-and-arm shape:
 * the press spends a flat 5 Momentum, arms its own 10s lane and stamps a 5s window of
 * looser swings and a quicker step. Below the price it fizzles with a {@code No Momentum}
 * line and arms nothing; a wrong-class press and one during the lane's cooldown reject
 * silently. Nothing is owed back when the window goes unspent: the stacks are already gone.
 */
public final class OffloadAbility {
	public static final String NAME = "Offload";
	/** What the press spends: a flat price, paid out of the reserve above the last checkpoint. */
	public static final int COST_STACKS = 5;
	/** Window in ticks: 5 seconds of loosened swings. */
	public static final int WINDOW_TICKS = 100;
	/** Attack speed while the window is live: +0.2, the burden easing off the arm. */
	public static final double HASTE_BONUS = 0.2;
	/** Move speed while the window is live: +0.01, about +10% pace. */
	public static final double SPEED_BONUS = 0.01;
	/** Haste modifier id, resolved by the applier. */
	public static final Identifier HASTE_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "offload_haste");
	/** Speed modifier id, resolved by the applier. */
	public static final Identifier SPEED_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "offload_speed");

	/**
	 * The window's pair (shared, never mutated): single source for the applier's write
	 * and remove paths, so the two sides cannot drift.
	 */
	private static final List<PassiveEffect.Modifier> WINDOW = List.of(
			new PassiveEffect.Modifier(ClassRegistry.ATTACK_SPEED, HASTE_ID, HASTE_BONUS),
			new PassiveEffect.Modifier(ClassRegistry.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS));

	private OffloadAbility() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "Spend " + COST_STACKS + " Momentum: +" + ConditionalPassive.amount(HASTE_BONUS)
				+ " attack speed, +" + ConditionalPassive.amount(SPEED_BONUS) + " speed for "
				+ (WINDOW_TICKS / 20) + "s (R, " + (Lane.OFFLOAD.cooldownMs() / 1000) + "s).";
	}

	/** Every stat modifier the window can write: both the apply and the removal paths iterate this. */
	public static List<PassiveEffect.Modifier> windowModifiers() {
		return WINDOW;
	}

	/**
	 * The loosened-window family row: the haste and speed pair in play, wanted only while
	 * the Offload window is live. The row lives here beside the pair it spells.
	 */
	public static WindowFamily windowFamily() {
		return WindowFamily.whileLive(ClassRegistry.OFFLOAD_ABILITY, Lane.OFFLOAD, WINDOW);
	}

	/**
	 * Decides offloading for Juggernauts holding the price off cooldown; fewer than
	 * {@link #COST_STACKS} fizzles before the cooldown is even read. Pure and read-only:
	 * only {@link #fire} arms the lane and spends.
	 */
	public static AbilityOutcome trigger(ClassDefinition def, long offloadRemaining, int stacks) {
		if (def == null || !def.hasAbility(ClassRegistry.OFFLOAD_ABILITY)) {
			return AbilityOutcome.rejected();
		}
		if (stacks < COST_STACKS) {
			return AbilityOutcome.fizzled(new ChatLine("No Momentum", null));
		}
		if (offloadRemaining > 0) {
			return AbilityOutcome.rejected();
		}
		return AbilityOutcome.fired(new ChatLine(NAME + "!", ChatFormatting.YELLOW));
	}

	/**
	 * Arms the lane and stamps the window, then spends the price, on a fired press only;
	 * rejected and fizzled presses leave the bar untouched. The arm reads the same lane
	 * and now the trigger just checked, so it cannot fail.
	 */
	public static AbilityOutcome fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		ConditionalPassive momentum = def == null ? null : def.passiveForTrigger(Trigger.ON_HIT_DEALT);
		int stacks = momentum == null ? 0 : runtime.stacks(momentum);
		AbilityOutcome result = trigger(def, runtime.state(now).laneRemaining(Lane.OFFLOAD), stacks);
		if (result.fired()) {
			runtime.arm(Lane.OFFLOAD, now, WINDOW_TICKS * 50L);
			runtime.spendStacks(momentum, COST_STACKS);
		}
		return result;
	}
}
