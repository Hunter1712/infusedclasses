package com.hunter1712.infusedclasses;

/**
 * The shared ability outcome: whether a press fired, what it says, and the cooldown
 * answer when a lane rejected it. Every ability decision returns one (bare, or wrapped
 * with its own payload), so the fired question, the silent wrong-class reject and the
 * cooldown line have one home; a fizzle says its line without a cast.
 */
public record AbilityOutcome(boolean fired, ChatLine announcement, long remainingMs) {
	/** Fired: announces its line, plays the cast fanfare. */
	public static AbilityOutcome fired(ChatLine line) {
		return new AbilityOutcome(true, line, 0);
	}

	/** Silent reject: a wrong class says nothing, grants nothing, plays nothing. */
	public static AbilityOutcome rejected() {
		return new AbilityOutcome(false, null, 0);
	}

	/** Fizzle: says its line, no cast, no cooldown. */
	public static AbilityOutcome fizzled(ChatLine line) {
		return new AbilityOutcome(false, line, 0);
	}

	/** On cooldown: the clock line, plus the machine-readable remaining. */
	public static AbilityOutcome onCooldown(String abilityName, long remainingMs) {
		return new AbilityOutcome(false, ChatLine.cooldown(abilityName, remainingMs), remainingMs);
	}
}
