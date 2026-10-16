package com.hunter1712.infusedclasses;

/**
 * Crit decisions: the verdict of one landed full-charge hit, decided purely from the
 * class's crit rule, the current Streak, the All In window, the swing charge and a roll
 * the caller drew. No decision module touches a random source, so boundaries are
 * assertable in plain JUnit. Vanilla crits are retired at the mixin; this replaces them
 * for every player: the universal baseline (10% at x1.25, full swings only, no betrayal,
 * no Streak movement) for classless and classed alike, a class rule where one exists.
 * The verdict's multiplier folds into the swing's deal mult and its movement is applied
 * by the hit settle; the entry draws the roll.
 */
public final class CritRoll {
	/** The band cap: the crit chance never loads past 95%, so the normal band can disappear but betrayal never does. */
	public static final double CRIT_CAP = 0.95;

	/** Verdict of one landed hit. NONE is a swing that could not roll: spam never touches the dice. */
	public enum Verdict {
		NONE,
		NORMAL,
		CRIT,
		BETRAYAL
	}

	/** What the verdict does to the attacker's Streak. NONE is a rule without a Streak: the counter stays put. */
	public enum Movement {
		NONE,
		BUMP,
		RESET
	}

	/** One roll's outcome: the verdict, the multiplier it folds into the swing, the Streak movement it asks for. */
	public record Result(Verdict verdict, double mult, Movement movement) {
		/** Nothing rolled: half swings stay off the dice entirely. */
		public static final Result NONE = new Result(Verdict.NONE, 1.0, Movement.NONE);
	}

	private CritRoll() {
	}

	/**
	 * Rolls one landed full-charge hit: betrayal band first, then the crit band (chance
	 * loaded by the Streak, locked at the cap), remainder normal. Full swings only, so
	 * spam never rolls and never moves the Streak. A live All In window lifts the crit
	 * multiplier and bends nothing else: betrayal keeps its band and its mult, a crit
	 * still resets the Streak.
	 */
	public static Result roll(CritRule rule, int streak, float charge, float roll, boolean allIn) {
		if (!BonusCurve.isFullSwing(charge)) {
			return Result.NONE;
		}
		if (roll < rule.betrayalChance()) {
			return new Result(Verdict.BETRAYAL, rule.betrayalMult(), movement(rule, Movement.BUMP));
		}
		double critUpTo = rule.betrayalChance()
				+ Math.min(CRIT_CAP, rule.critChance() + rule.streakStep() * streak);
		if (roll < critUpTo) {
			return new Result(Verdict.CRIT, allIn ? AllInAbility.CRIT_MULT : rule.critMult(),
					movement(rule, Movement.RESET));
		}
		return new Result(Verdict.NORMAL, 1.0, movement(rule, Movement.BUMP));
	}

	/** Streak moves only for rules carrying a step: the universal baseline leaves the counter alone. */
	private static Movement movement(CritRule rule, Movement wanted) {
		return rule.streakStep() > 0 ? wanted : Movement.NONE;
	}
}
