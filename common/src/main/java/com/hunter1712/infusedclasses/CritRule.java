package com.hunter1712.infusedclasses;

/**
 * A class's dice data: crit chance and mult, betrayal chance and mult, Streak step.
 * One record per class that has dice; the generated Crooked Dice and Streak card lines
 * read their numbers from it, so the Streak reset target cannot drift from the chance
 * base. The universal rule is the baseline every class without dice reads.
 */
public record CritRule(double critChance, double critMult, double betrayalChance, double betrayalMult,
		double streakStep) {
	/** Card line name for the rule's bands. */
	public static final String CROOKED_DICE = "Crooked Dice";
	/** Card line name for the rule's streak step. */
	public static final String STREAK = "Streak";

	private static final CritRule UNIVERSAL = new CritRule(0.10, 1.25, 0.0, 1.0, 0.0);
	private static final CritRule CROOKED_DICE_RULE = new CritRule(0.15, 1.5, 0.05, 0.5, 0.15);

	/** The universal baseline: 10% crit at x1.25, no betrayal, no Streak movement. Classless reads as this. */
	public static CritRule universal() {
		return UNIVERSAL;
	}

	/** The Gambler's Crooked Dice: 15% crit at x1.5, 5% betrayal at x0.5, +15pp of crit per non-crit. */
	public static CritRule crookedDice() {
		return CROOKED_DICE_RULE;
	}

	/** Whether the class carries dice: the universal baseline carries none. */
	public boolean hasDice() {
		return betrayalChance > 0 || streakStep > 0;
	}

	/** Crooked Dice card line, generated from the bands so copy cannot drift from the roll. */
	public String crookedDiceDesc() {
		return "Full-charge hits: " + percent(critChance) + " crit x" + ConditionalPassive.amount(critMult)
				+ ", " + percent(betrayalChance) + " betrayal x" + ConditionalPassive.amount(betrayalMult) + ".";
	}

	/** Streak card line, generated from the step and the computed lock: the reset target is the chance base. */
	public String streakDesc() {
		return "Non-crit hit: +" + percent(streakStep) + " crit (a crit resets to " + percent(critChance)
				+ "). Locks at " + percent(CritRoll.CRIT_CAP) + " from streak " + lockStreak() + ".";
	}

	/** How many non-crit hits the step needs to load the crit band up to the cap. */
	private int lockStreak() {
		return (int) Math.ceil((CritRoll.CRIT_CAP - critChance) / streakStep);
	}

	/** Whole percentages read as the picker says them: 0.15 reads "15%". */
	private static String percent(double chance) {
		return ConditionalPassive.amount(chance * 100) + "%";
	}
}
