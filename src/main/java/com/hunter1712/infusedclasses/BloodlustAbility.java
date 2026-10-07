package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;

/**
 * Bloodlust decisions: the Berserker's lifesteal opener on {@code R}. Pure gate-and-arm shape
 * mirroring Second Wind, plus the HP gate: a bleeding Berserker opens a 10s lifesteal window on
 * its own 30s lane and seeds Bulwark with its current bucket. A press above the gate fizzles
 * free like Second Wind at 0 stacks: cooldown untouched, no root, no builder.
 */
public final class BloodlustAbility {
	public static final String NAME = "Bloodlust";
	/** Lifesteal window in ticks: 10 seconds, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 200;
	/** Lifesteal rate: bucket x damage x this, 0% at full HP rising to 20% at 0 HP. */
	public static final double LIFESTEAL_PER_BUCKET = 0.02;
	/** Lifesteal cap per melee hit in HP: one heart, so spawners cannot out-heal the world. */
	public static final float MAX_PER_HIT_HP = 2.0F;
	/** HP gate: fires only at bucket 5+, about under 5 hearts at 20 max. */
	public static final int GATE_BUCKET = 5;

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN,
		TOO_HEALTHY
	}

	public record Result(Kind kind, long remainingMs, int builderStacks, ChatLine announcement) {
		public static Result fired(int bucket) {
			return new Result(Kind.FIRED, 0, bucket, new ChatLine(NAME + "!", ChatFormatting.RED));
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0, 0, null);
		}

		public static Result onCooldown(long remainingMs) {
			return new Result(Kind.ON_COOLDOWN, remainingMs, 0, ChatLine.cooldown(NAME, remainingMs));
		}

		public static Result tooHealthy() {
			return new Result(Kind.TOO_HEALTHY, 0, 0, new ChatLine("Too healthy", null));
		}
	}

	private BloodlustAbility() {
	}

	/** Lifesteal HP for one melee hit: damage x bucket x 2%, capped at 2 HP. Zero at bucket 0 by construction. */
	public static float lifestealFor(int bucket, float damageTaken) {
		if (bucket <= 0 || damageTaken <= 0) {
			return 0;
		}
		return (float) Math.min(MAX_PER_HIT_HP, damageTaken * bucket * LIFESTEAL_PER_BUCKET);
	}

	/** Decides opening for Berserkers at or past the HP gate, off cooldown. Pure and read-only: only {@link #fire} arms the lane and the window. */
	public static Result trigger(ClassDefinition def, Cooldowns cooldowns, int bucket, long now) {
		if (def == null || !def.activeAbilities().contains(ClassRegistry.BLOODLUST_ABILITY)) {
			return Result.wrongClass();
		}
		if (bucket < GATE_BUCKET) {
			return Result.tooHealthy();
		}
		long remaining = cooldowns.bloodlustRemaining(now);
		if (remaining > 0) {
			return Result.onCooldown(remaining);
		}
		return Result.fired(bucket);
	}

	/**
	 * Gate and arm together: the production path. A fired press arms the transient runtime's
	 * Bloodlust lane and stamps the window; rejected presses leave both untouched. An arm
	 * that somehow fails reports on-cooldown instead of a fire it did not earn.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, int bucket, long now) {
		Result result = trigger(def, runtime.cooldowns(), bucket, now);
		if (result.kind() == Kind.FIRED && !runtime.tryUseBloodlust(now)) {
			return Result.onCooldown(runtime.cooldowns().bloodlustRemaining(now));
		}
		return result;
	}
}
