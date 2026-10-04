package com.hunter1712.infusedclasses;

/**
 * War Cry ability decisions. Pure: gating by active ability plus cooldown,
 * no live player needed, so the result is assertable directly. The server
 * entry maps the result to effects, sound, particles, and chat.
 */
public final class WarCryAbility {
	public static final int EFFECT_TICKS = 160; // 8s
	public static final int EFFECT_AMPLIFIER = 1;

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN
	}

	public record Result(Kind kind, long remainingMs) {
		public static Result fired() {
			return new Result(Kind.FIRED, 0);
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0);
		}

		public static Result onCooldown(long remainingMs) {
			return new Result(Kind.ON_COOLDOWN, remainingMs);
		}
	}

	private WarCryAbility() {
	}

	/** Fires when the current class carries the War Cry ability and is off cooldown. */
	public static Result trigger(ClassDefinition def, Cooldowns cooldowns, long now) {
		if (def == null || !ClassRegistry.WARRIOR_ABILITY.equals(def.activeAbility())) {
			return Result.wrongClass();
		}
		if (!cooldowns.useWarCry(now)) {
			return Result.onCooldown(cooldowns.warCryRemaining(now));
		}
		return Result.fired();
	}
}
