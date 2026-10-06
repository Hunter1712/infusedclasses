package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * War Cry decisions: the Warrior's debuff shout on {@code R}. Pure gate-and-arm shape
 * mirroring Second Wind, minus the stacks: a free shout on its own 60s lane laying
 * Weakness I and Slowness I on hostiles in radius. Effect ids, not holders, so the
 * decision stays bootstrap-free and the adapter resolves them.
 */
public final class WarCryAbility {
	public static final String NAME = "War Cry";
	/** Shout radius in blocks: about one chunk. */
	public static final double RADIUS_BLOCKS = 16.0;
	/** Debuff window in ticks: 8 seconds. */
	public static final int EFFECT_TICKS = 160;
	public static final Identifier WEAKNESS = Identifier.withDefaultNamespace("weakness");
	public static final Identifier SLOWNESS = Identifier.withDefaultNamespace("slowness");
	/** Amplifier for both effects: level I. */
	public static final int AMPLIFIER = 0;

	/** The shout's payload, fixed: Weakness I plus Slowness I for the window. Ticks and
	 * amplifier are shared constants, so the effects travel as bare ids. */
	public static final List<Identifier> DEBUFF_EFFECTS = List.of(WEAKNESS, SLOWNESS);

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN
	}

	public record Result(Kind kind, long remainingMs, ChatLine announcement) {
		public static Result fired() {
			return new Result(Kind.FIRED, 0, new ChatLine(NAME + "!", ChatFormatting.RED));
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0, null);
		}

		public static Result onCooldown(long remainingMs) {
			return new Result(Kind.ON_COOLDOWN, remainingMs, ChatLine.cooldown(NAME, remainingMs));
		}
	}

	private WarCryAbility() {
	}

	/** Decides shouting for classes carrying the shout, off cooldown. Pure and read-only: only {@link #fire} arms the lane. */
	public static Result trigger(ClassDefinition def, Cooldowns cooldowns, long now) {
		if (def == null || !def.activeAbilities().contains(ClassRegistry.WAR_CRY_ABILITY)) {
			return Result.wrongClass();
		}
		long remaining = cooldowns.warCryRemaining(now);
		if (remaining > 0) {
			return Result.onCooldown(remaining);
		}
		return Result.fired();
	}

	/**
	 * Gate and arm together: the production path. Rejected presses leave the lane untouched.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		Result result = trigger(def, runtime.cooldowns(), now);
		if (result.kind() == Kind.FIRED) {
			runtime.tryUseWarCry(now);
		}
		return result;
	}
}
