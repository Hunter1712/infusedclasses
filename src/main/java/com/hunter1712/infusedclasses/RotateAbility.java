package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Rotate decisions: the Legionary's stance toggle on {@code R}. Pure gate-and-arm
 * shape with no build-up and no fizzle on stance: any stance fires off cooldown,
 * firing flips the transient runtime stance and arms only the rotate lane.
 * The stance auras themselves are applied by the passive applier; the deal/take
 * multipliers below are applied by the melee hook.
 */
public final class RotateAbility {
	public static final String NAME = "Rotate";

	/** Legionary form. Scutum is the default: every Legionary enters in plate. */
	public enum Stance {
		GLADIUS("Gladius"),
		SCUTUM("Scutum");

		private final String displayName;

		Stance(String displayName) {
			this.displayName = displayName;
		}

		/** Display copy: the fire announcement and the info line share it. */
		public String displayName() {
			return displayName;
		}
	}

	// Stance auras. Downsides ride in the damage hooks as multipliers, never as
	// negative attributes (armor reads back 0 under negatives).
	public static final Identifier GLADIUS_ALACRITY_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "gladius_alacrity");
	public static final Identifier SCUTUM_PLATE_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "scutum_plate");
	public static final Identifier SCUTUM_TOUGHNESS_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "scutum_toughness");
	public static final Identifier SCUTUM_WEIGHT_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "scutum_weight");
	/** Gladius aura: melee dealt x1.15, glass. */
	public static final double GLADIUS_DEAL_MULT = 1.15;
	/** Gladius aura: +0.3 attack speed, paying back part of the damage cut as rate. */
	public static final double GLADIUS_SPEED = 0.3;
	/** Scutum aura: +3 armor, plate. */
	public static final double SCUTUM_ARMOR = 3.0;
	/** Scutum aura: +1 armor toughness. */
	public static final double SCUTUM_TOUGHNESS = 1.0;
	/** Scutum aura: slower, flat -0.005 move speed like every movement row. */
	public static final double SCUTUM_WEIGHT = -0.005;

	/** Gladius downside: melee taken x1.2, glass reads as glass. */
	public static final double GLADIUS_TAKEN_MULT = 1.2;
	/** Scutum downside: melee dealt x0.9, plate reads as plate. */
	public static final double SCUTUM_DEAL_MULT = 0.9;

	public static final String IMPETUS_NAME = "Impetus";
	/** Flip buff: the next melee hit after flipping to Gladius deals x1.10, at any charge. */
	public static final double SWITCH_DEAL_MULT = 1.10;
	/** Flip buff window in millis: 5s after the flip, then the buff expires unspent. */
	public static final long SWITCH_DEAL_MS = 5_000L;

	public static final String TESTUDO_NAME = "Testudo";
	/** Flip buff: melee taken within the catch window pays x0.85. */
	public static final double SWITCH_TAKE_MULT = 0.85;
	/** Flip buff window in millis: 1.5s after the flip, then the catch closes. */
	public static final long SWITCH_TAKE_MS = 1_500L;

	/**
	 * What firing should do. {@code stance} is the new stance on fire, null on
	 * rejects; the announcement names it verbatim.
	 */
	public record Result(AbilityOutcome outcome, Stance stance) {
		public static Result fired(Stance stance) {
			ChatFormatting style = stance == Stance.GLADIUS ? ChatFormatting.RED : ChatFormatting.GRAY;
			return new Result(AbilityOutcome.fired(new ChatLine(stance.displayName() + "!", style)), stance);
		}

		public static Result rejected() {
			return new Result(AbilityOutcome.rejected(), null);
		}
	}

	private RotateAbility() {
	}

	/**
	 * The stance aura as modifiers: what sits on the player while in this stance.
	 * Single source for the applier's write and remove paths, so they cannot drift.
	 * Shared instances: the tick iterates them, never mutates.
	 */
	public static List<PassiveEffect.Modifier> auraFor(Stance stance) {
		return stance == Stance.GLADIUS ? GLADIUS_AURA : SCUTUM_AURA;
	}

	private static final List<PassiveEffect.Modifier> GLADIUS_AURA = List.of(
			new PassiveEffect.Modifier(ClassRegistry.ATTACK_SPEED, GLADIUS_ALACRITY_ID, GLADIUS_SPEED));
	private static final List<PassiveEffect.Modifier> SCUTUM_AURA = List.of(
			new PassiveEffect.Modifier(ClassRegistry.ARMOR, SCUTUM_PLATE_ID, SCUTUM_ARMOR),
			new PassiveEffect.Modifier(ClassRegistry.ARMOR_TOUGHNESS, SCUTUM_TOUGHNESS_ID, SCUTUM_TOUGHNESS),
			new PassiveEffect.Modifier(ClassRegistry.MOVEMENT_SPEED, SCUTUM_WEIGHT_ID, SCUTUM_WEIGHT));

	/**
	 * Outgoing melee multiplier for a stance (no flip buff).
	 * Gladius deals x1.15; Scutum deals x0.9. The Impetus flip buff applies its own factor in MeleePlan.
	 */
	public static double dealMult(Stance stance) {
		return stance == Stance.SCUTUM ? SCUTUM_DEAL_MULT : GLADIUS_DEAL_MULT;
	}

	/**
	 * Incoming melee multiplier for a stance (no flip buff).
	 * Gladius takes x1.2; Scutum takes x1.0. The Testudo flip buff applies its own factor in MeleePlan.
	 */
	public static double takeMult(Stance stance) {
		return stance == Stance.GLADIUS ? GLADIUS_TAKEN_MULT : 1.0;
	}

	/** The other stance: firing always lands here, whichever is current. */
	public static Stance flipped(Stance stance) {
		return stance == Stance.GLADIUS ? Stance.SCUTUM : Stance.GLADIUS;
	}

	/** Picker copy for the toggle, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "Gladius (x" + ConditionalPassive.amount(GLADIUS_DEAL_MULT)
				+ " damage, +" + ConditionalPassive.amount(GLADIUS_SPEED)
				+ " speed, take x" + GLADIUS_TAKEN_MULT
				+ ") or Scutum (+" + ConditionalPassive.amount(SCUTUM_ARMOR)
				+ " armor, +" + ConditionalPassive.amount(SCUTUM_TOUGHNESS)
				+ " toughness, " + ConditionalPassive.amount(SCUTUM_WEIGHT) + " speed) (R, "
				+ (Lane.ROTATE.cooldownMs() / 1000) + "s).";
	}

	/** Picker copy for the Gladius-flip buff, generated from the numbers so it cannot drift from behaviour. */
	public static String impetusDesc() {
		return "Flip to Gladius: next hit x" + SWITCH_DEAL_MULT
				+ ", any charge (" + (SWITCH_DEAL_MS / 1000) + "s, one-shot).";
	}

	/** Picker copy for the Scutum-flip buff, generated from the numbers so it cannot drift from behaviour. */
	public static String testudoDesc() {
		return "Flip to Scutum: take x" + SWITCH_TAKE_MULT
				+ " for " + ConditionalPassive.amount(SWITCH_TAKE_MS / 1000.0) + "s.";
	}

	/** Decides flipping for Legionaries off the rotate lock. Never fizzles on stance. Pure and read-only: only {@link #fire} arms the lane and flips. */
	public static Result trigger(ClassDefinition def, long rotateRemaining, Stance current) {
		if (def == null || !def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return Result.rejected();
		}
		if (rotateRemaining > 0) {
			return Result.rejected();
		}
		return Result.fired(flipped(current));
	}

	/**
	 * Arms the transient runtime's rotate lane and flips the stance together on a fired
	 * press; rejected presses leave both untouched. The arm reads the same lane and now
	 * the trigger just checked, so it cannot fail.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		PlayerRuntime.State state = runtime.state(now);
		Result result = trigger(def, state.laneRemaining(Lane.ROTATE), state.stance());
		if (result.outcome().fired()) {
			runtime.rotate(now, result.stance());
		}
		return result;
	}
}
