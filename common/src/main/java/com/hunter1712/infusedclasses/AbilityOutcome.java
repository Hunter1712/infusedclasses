package com.hunter1712.infusedclasses;

/**
 * The shared ability outcome: whether a press fired and what it says. Every ability
 * decision returns one (bare, or wrapped with its own payload), so the fired line and
 * the silent rejection (a wrong class or a lane not yet ready — the same silence, per
 * ADR-0017, whose silence carried at ADR-0019) have one home; a fizzle says its line
 * without a cast.
 */
public record AbilityOutcome(boolean fired, ChatLine announcement) {
	/** Fired: announces its line, plays the cast fanfare. */
	public static AbilityOutcome fired(ChatLine line) {
		return new AbilityOutcome(true, line);
	}

	/** Silent reject: a wrong class or a lane still cooling down says nothing, grants nothing, plays nothing. */
	public static AbilityOutcome rejected() {
		return new AbilityOutcome(false, null);
	}

	/** Fizzle: says its line, no cast, no cooldown. */
	public static AbilityOutcome fizzled(ChatLine line) {
		return new AbilityOutcome(false, line);
	}
}
