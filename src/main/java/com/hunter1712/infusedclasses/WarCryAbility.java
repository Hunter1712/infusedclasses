package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * War Cry ability decisions. Pure: gating by active ability plus cooldown, and the
 * description of what the result makes the server entry do. No live player needed,
 * so every fact the entry applies is assertable directly.
 */
public final class WarCryAbility {
	public static final String NAME = "War Cry";
	public static final int EFFECT_TICKS = 160; // 8s
	public static final int EFFECT_AMPLIFIER = 1;

	/** Registry ids, not registry objects, so deciding stays free of a game bootstrap. */
	public static final Identifier EFFECT = Identifier.fromNamespaceAndPath("minecraft", "strength");
	public static final Identifier SOUND = Identifier.fromNamespaceAndPath("minecraft", "entity.ravager.roar");
	public static final Identifier PARTICLE = Identifier.fromNamespaceAndPath("minecraft", "angry_villager");

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN
	}

	/**
	 * A chat line the entry sends verbatim: the text, and the chat style it shows in. A null style
	 * is a decision too: the line goes out unstyled and inherits whatever the client shows by
	 * default, which is what the cooldown line has always done.
	 */
	public record ChatLine(String text, ChatFormatting style) {
	}

	/**
	 * What firing the ability applies: which effect, for how long, at what amplifier, plus the
	 * sound and particle that go with it. Registry ids, so a second ability is another of these.
	 */
	public record Outcome(Identifier effect, int durationTicks, int amplifier, Identifier sound, Identifier particle) {
	}

	/**
	 * What the entry should do with a press. Outcome is null when there is nothing to apply
	 * (wrong class, on cooldown), announcement is null when nothing should be said.
	 */
	public record Result(Kind kind, long remainingMs, Outcome outcome, ChatLine announcement) {
		public static Result fired() {
			return new Result(Kind.FIRED, 0, new Outcome(EFFECT, EFFECT_TICKS, EFFECT_AMPLIFIER, SOUND, PARTICLE),
					new ChatLine(NAME + "!", ChatFormatting.RED));
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0, null, null);
		}

		public static Result onCooldown(long remainingMs) {
			// Unstyled on purpose: this line has never carried a colour of its own.
			return new Result(Kind.ON_COOLDOWN, remainingMs, null,
					new ChatLine(NAME + ": " + String.format(Locale.ROOT, "%.0fs", remainingMs / 1000.0), null));
		}
	}

	private WarCryAbility() {
	}

	/** Fires when the current class carries the War Cry ability and is off cooldown. Read-only: firing advances the cooldown, and only the caller does that. */
	public static Result trigger(ClassDefinition def, Cooldowns cooldowns, long now) {
		if (def == null || !ClassRegistry.WARRIOR_ABILITY.equals(def.activeAbility())) {
			return Result.wrongClass();
		}
		long remaining = cooldowns.warCryRemaining(now);
		if (remaining > 0) {
			return Result.onCooldown(remaining);
		}
		return Result.fired();
	}

	/**
	 * Gate and arm together: the production path. Decides with {@link #trigger} and arms the
	 * transient runtime's cooldown when it fires, so no caller can fire without arming.
	 * Wrong-class and on-cooldown presses leave the cooldown untouched.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		Result result = trigger(def, runtime.cooldowns(), now);
		if (result.kind() == Kind.FIRED) {
			runtime.tryUseWarCry(now);
		}
		return result;
	}
}