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
	/** Rotate lock in millis: 8s between stance flips. Canonical home is {@link Cooldowns#ROTATE_MS}. */
	public static final long ROTATE_MS = Cooldowns.ROTATE_MS;

	/** Legionary form. Scutum is the default: every Legionary enters in plate. */
	public enum Stance {
		GLADIUS,
		SCUTUM
	}

	// Stance auras. Downsides ride in the damage hooks as multipliers, never as
	// negative attributes (armor reads back 0 under negatives).
	public static final Identifier GLADIUS_DAMAGE_ID =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "gladius_damage");
	public static final Identifier GLADIUS_ALACRITY_ID =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "gladius_alacrity");
	public static final Identifier SCUTUM_PLATE_ID =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "scutum_plate");
	public static final Identifier SCUTUM_TOUGHNESS_ID =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "scutum_toughness");
	public static final Identifier SCUTUM_WEIGHT_ID =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "scutum_weight");
	/** Gladius aura: +2 attack damage, glass. */
	public static final double GLADIUS_DAMAGE = 2.0;
	/** Gladius aura: +0.2 attack speed. */
	public static final double GLADIUS_SPEED = 0.2;
	/** Scutum aura: +6 armor, plate. */
	public static final double SCUTUM_ARMOR = 6.0;
	/** Scutum aura: +1 armor toughness. */
	public static final double SCUTUM_TOUGHNESS = 1.0;
	// ponytail: absolute -0.008 (~-8% of the 0.1 base) through the ADD_VALUE
	// write path, so no per-operation plumbing for one slow. Revisit with a
	// multiplied operation if base speed ever stops being 0.1.
	/** Scutum aura: slower, about -8% move speed. */
	public static final double SCUTUM_WEIGHT = -0.008;

	/** Gladius downside: melee taken x1.2, glass reads as glass. */
	public static final double GLADIUS_TAKEN_MULT = 1.2;
	/** Scutum downside: melee dealt x0.8, plate reads as plate. */
	public static final double SCUTUM_DEAL_MULT = 0.8;

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN
	}

	/**
	 * What firing should do. {@code stance} is the new stance on fire, null on
	 * rejects; the announcement names it verbatim.
	 */
	public record Result(Kind kind, long remainingMs, Stance stance, ChatLine announcement) {
		public static Result fired(Stance stance) {
			ChatLine line = stance == Stance.GLADIUS
					? new ChatLine("Gladius!", ChatFormatting.RED)
					: new ChatLine("Scutum!", ChatFormatting.GRAY);
			return new Result(Kind.FIRED, 0, stance, line);
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0, null, null);
		}

		public static Result onCooldown(long remainingMs) {
			return new Result(Kind.ON_COOLDOWN, remainingMs, null, ChatLine.cooldown(NAME, remainingMs));
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
			new PassiveEffect.Modifier(ClassRegistry.ATTACK_DAMAGE, GLADIUS_DAMAGE_ID, GLADIUS_DAMAGE),
			new PassiveEffect.Modifier(ClassRegistry.ATTACK_SPEED, GLADIUS_ALACRITY_ID, GLADIUS_SPEED));
	private static final List<PassiveEffect.Modifier> SCUTUM_AURA = List.of(
			new PassiveEffect.Modifier(ClassRegistry.ARMOR, SCUTUM_PLATE_ID, SCUTUM_ARMOR),
			new PassiveEffect.Modifier(ClassRegistry.ARMOR_TOUGHNESS, SCUTUM_TOUGHNESS_ID, SCUTUM_TOUGHNESS),
			new PassiveEffect.Modifier(ClassRegistry.MOVEMENT_SPEED, SCUTUM_WEIGHT_ID, SCUTUM_WEIGHT));

	/**
	 * Outgoing melee multiplier for a stance plus a live Testudo window.
	 * Scutum deals x0.8; Testudo multiplies a further x0.7 (x0.56 together).
	 */
	public static double dealMult(Stance stance, boolean testudoLive) {
		double mult = stance == Stance.SCUTUM ? SCUTUM_DEAL_MULT : 1.0;
		return testudoLive ? mult * TestudoAbility.TESTUDO_DEAL_MULT : mult;
	}

	/**
	 * Incoming melee multiplier for a stance plus a live Impetus window.
	 * Gladius takes x1.2; Impetus multiplies a further x1.25 (x1.5 together).
	 */
	public static double takeMult(Stance stance, boolean impetusLive) {
		double mult = stance == Stance.GLADIUS ? GLADIUS_TAKEN_MULT : 1.0;
		return impetusLive ? mult * ImpetusAbility.IMPETUS_TAKEN_MULT : mult;
	}

	/** The other stance: firing always lands here, whichever is current. */
	public static Stance flipped(Stance stance) {
		return stance == Stance.GLADIUS ? Stance.SCUTUM : Stance.GLADIUS;
	}

	/** Decides flipping for Legionaries off the rotate lock. Never fizzles on stance. Pure and read-only: only {@link #fire} arms the lane and flips. */
	public static Result trigger(ClassDefinition def, long rotateRemaining, Stance current) {
		if (def == null || !def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			return Result.wrongClass();
		}
		if (rotateRemaining > 0) {
			return Result.onCooldown(rotateRemaining);
		}
		return Result.fired(flipped(current));
	}

	/**
	 * Gate and arm together: the production path. A fired press arms the transient
	 * runtime's rotate lane and flips the stance inside the same arm, so the flip
	 * cannot run on a failed arm or be forgotten by a caller; rejected presses leave
	 * both untouched. An arm that somehow fails reports on-cooldown instead of a
	 * fire it did not earn.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		Result result = trigger(def, runtime.rotateRemaining(now), runtime.stance());
		return AbilityGate.gate(result, result.kind() == Kind.FIRED, () -> {
			if (!runtime.tryUseRotate(now)) {
				return false;
			}
			runtime.flipStance();
			return true;
		}, () -> runtime.rotateRemaining(now), Result::onCooldown);
	}
}
