package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Aequilibrium decisions: the Legionary's clean ultimate on {@code V}. Pure gate-and-arm
 * shape on the reused shared oath lane: fires in either stance, firing arms the lane and
 * stamps a 6s window. Edge plus plate ride the stance applier while live; the stance stays
 * underneath, so both layers count. No deal/take mults, no knockback immunity, no downsides.
 */
public final class AequilibriumAbility {
	public static final String NAME = "Aequilibrium";
	/** Edge granted while live: +1 attack damage. */
	public static final double EDGE_BONUS = 1.0;
	/** Plate granted while live: +3 armor. */
	public static final double PLATE_BONUS = 3.0;
	/** Ultimate window in ticks: 6 seconds, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 120;
	/** Modifier id for the window edge. */
	public static final Identifier EDGE_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "aequilibrium_edge");
	/** Modifier id for the window plate. */
	public static final Identifier PLATE_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "aequilibrium_plate");
	/** Cast sound id, resolved by the adapter; fail silent if missing. */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("entity.player.levelup");

	/**
	 * The window aura as modifiers: what sits on the player while Aequilibrium is live.
	 * Single source for the applier's write and remove paths, so they cannot drift.
	 * Shared instance: the tick iterates it, never mutates.
	 */
	public static List<PassiveEffect.Modifier> windowModifiers() {
		return WINDOW_AURA;
	}

	private static final List<PassiveEffect.Modifier> WINDOW_AURA = List.of(
			new PassiveEffect.Modifier(ClassRegistry.ATTACK_DAMAGE, EDGE_ID, EDGE_BONUS),
			new PassiveEffect.Modifier(ClassRegistry.ARMOR, PLATE_ID, PLATE_BONUS));

	private AequilibriumAbility() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "Any stance: +" + ConditionalPassive.amount(EDGE_BONUS)
				+ " damage, +" + ConditionalPassive.amount(PLATE_BONUS)
				+ " armor, " + (WINDOW_TICKS / 20) + "s (V, "
				+ (Lane.OATH.cooldownMs() / 1000) + "s oath). No downside.";
	}

	/**
	 * Decides firing for Legionaries off the shared oath lane, in either stance, its
	 * fired line gold. Pure and read-only: only {@link #fire} arms the lane and the window.
	 */
	public static AbilityOutcome trigger(ClassDefinition def, long oathRemaining) {
		if (def == null || !def.hasAbility(ClassRegistry.AEQUILIBRIUM_ABILITY)) {
			return AbilityOutcome.rejected();
		}
		if (oathRemaining > 0) {
			return AbilityOutcome.rejected();
		}
		return AbilityOutcome.fired(new ChatLine(NAME + "!", ChatFormatting.GOLD));
	}

	/**
	 * Arms the shared oath lane and stamps the window together on a fired press; rejected
	 * presses leave both untouched. The arm reads the same lane and now the trigger just
	 * checked, so it cannot fail.
	 */
	public static AbilityOutcome fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		AbilityOutcome result = trigger(def, runtime.state(now).laneRemaining(Lane.OATH));
		if (result.fired()) {
			runtime.arm(Lane.OATH, now, WINDOW_TICKS * 50L);
		}
		return result;
	}
}
