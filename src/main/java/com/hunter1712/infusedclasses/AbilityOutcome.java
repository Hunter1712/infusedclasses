package com.hunter1712.infusedclasses;

/**
 * The shared ability outcome: whether a press fired, what it says, and the time it
 * still owes when a lane rejected it. Every ability decision returns one (bare, or
 * wrapped with its own payload), so the fired line and the two silent rejections
 * (wrong class, and on cooldown — silent per ADR-0017, whose silence carried
 * at ADR-0019) have one home; a fizzle says its line without a cast.
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

	/** On cooldown: says nothing and arms nothing, carrying the machine-readable remaining. */
	public static AbilityOutcome onCooldown(long remainingMs) {
		return new AbilityOutcome(false, null, remainingMs);
	}
}
