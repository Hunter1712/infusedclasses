package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

/**
 * Rush decisions: the Juggernaut's engage on {@code R}. Pure gate-and-arm shape
 * mirroring Rend, minus every fizzle but cooldown: a topped-off Juggernaut rushing
 * with zero stacks is the normal case, not a reject. Firing arms the transient
 * runtime's Rush lane and stamps the 4s window plus the one-shot next-hit bonus.
 */
public final class RushAbility {
	public static final String NAME = "Rush";
	/** Engage window in ticks: 4 seconds, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 80;
	/** Speed effect length in ticks: as long as the window, so the icon is the timer. */
	public static final int SPEED_TICKS = WINDOW_TICKS;
	/** Speed amplifier: Speed I — sticky, not silly. */
	public static final int SPEED_AMPLIFIER = 0;
	/** Next-hit Momentum bonus: what the first qualifying melee hit inside the window earns. */
	public static final int BONUS_STACKS = 3;
	/** Speed effect id, resolved by the adapter; vanilla Speed, so the icon is honest. */
	public static final Identifier SPEED_EFFECT = Identifier.withDefaultNamespace("speed");
	/** Cast sound id, resolved by the adapter; the wind burst is the engage. */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("entity.wind_charge.wind_burst");

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN
	}

	public record Result(Kind kind, long remainingMs, ChatLine announcement) {
		public static Result fired() {
			return new Result(Kind.FIRED, 0, new ChatLine(NAME + "!", ChatFormatting.YELLOW));
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0, null);
		}

		public static Result onCooldown(long remainingMs) {
			return new Result(Kind.ON_COOLDOWN, remainingMs, ChatLine.cooldown(NAME, remainingMs));
		}
	}

	private RushAbility() {
	}

	/** Decides engaging for Juggernauts off cooldown. Pure and read-only: only {@link #fire} arms the lane and the window. */
	public static Result trigger(ClassDefinition def, long rushRemaining) {
		if (def == null || !def.hasAbility(ClassRegistry.RUSH_ABILITY)) {
			return Result.wrongClass();
		}
		if (rushRemaining > 0) {
			return Result.onCooldown(rushRemaining);
		}
		return Result.fired();
	}

	/**
	 * Gate and arm together: the production path. A fired press arms the transient runtime's
	 * Rush lane and stamps the window plus the next-hit bonus; rejected presses leave all
	 * three untouched. An arm that somehow fails reports on-cooldown instead of a fire it
	 * did not earn.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		Result result = trigger(def, runtime.rushRemaining(now));
		return AbilityGate.gate(result, result.kind() == Kind.FIRED, () -> runtime.tryUseRush(now),
				() -> runtime.rushRemaining(now), Result::onCooldown);
	}
}
