package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

/**
 * Rush decisions: the Juggernaut's engage on {@code R}. Pure gate-and-arm shape
 * mirroring Rend, minus every fizzle but cooldown: a topped-off Juggernaut rushing
 * with zero stacks is the normal case, not a reject. Firing arms the transient
 * runtime's Rush lane and stamps the 5s window plus the one-shot next-hit bonus.
 */
public final class RushAbility {
	public static final String NAME = "Rush";
	/** Engage window in ticks: 5 seconds, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 100;
	/** Speed granted while the window is live: +0.02 movement speed, about +20% pace. A stat, never an effect. */
	public static final double SPEED_BONUS = 0.02;
	/** Speed modifier id, resolved by the adapter. */
	public static final Identifier SPEED_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "rush_speed");
	/** Next-hit Momentum bonus: what the first qualifying melee hit inside the window earns. */
	public static final int BONUS_STACKS = 5;
	/** Cast sound id, resolved by the adapter; the wind burst is the engage. */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("entity.wind_charge.wind_burst");

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "Cleanses harmful effects and quickens you for " + (WINDOW_TICKS / 20)
				+ "s. Next full-swing hit: +" + BONUS_STACKS + " Momentum (R, "
				+ (Cooldowns.RUSH_MS / 1000) + "s).";
	}

	/**
	 * The window's speed as a modifier (shared, never mutated): single source for the
	 * applier's write and remove paths, so they cannot drift.
	 */
	public static final PassiveEffect.Modifier SPEED = new PassiveEffect.Modifier(
			ClassRegistry.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS);

	private RushAbility() {
	}

	/**
	 * Decides engaging for Juggernauts off cooldown, its fired line yellow. Pure and
	 * read-only: only {@link #fire} arms the lane and the window.
	 */
	public static AbilityOutcome trigger(ClassDefinition def, long rushRemaining) {
		if (def == null || !def.hasAbility(ClassRegistry.RUSH_ABILITY)) {
			return AbilityOutcome.rejected();
		}
		if (rushRemaining > 0) {
			return AbilityOutcome.onCooldown(rushRemaining);
		}
		return AbilityOutcome.fired(new ChatLine(NAME + "!", ChatFormatting.YELLOW));
	}

	/**
	 * Arms the transient runtime's Rush lane and stamps the window plus the next-hit bonus
	 * on a fired press; rejected presses leave them untouched. The arm reads the same lane
	 * and now the trigger just checked, so it cannot fail.
	 */
	public static AbilityOutcome fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		AbilityOutcome result = trigger(def, runtime.rushRemaining(now));
		if (result.fired()) {
			runtime.tryUseRush(now);
		}
		return result;
	}
}
