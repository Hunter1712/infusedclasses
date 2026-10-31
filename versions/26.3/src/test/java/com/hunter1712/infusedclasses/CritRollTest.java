package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crit roll decisions: one full-charge landed hit's verdict, with boundary rolls and no
 * random source. The universal baseline and the Gambler's Crooked Dice are both pinned,
 * including the Streak bands, the 95/5 lock and the All In lift.
 */
class CritRollTest {
	private static final CritRule UNIVERSAL = CritRule.universal();
	private static final CritRule DICE = CritRule.crookedDice();

	private static CritRoll.Result roll(CritRule rule, int streak, float roll) {
		return CritRoll.roll(rule, streak, 1.0F, roll, false);
	}

	@Test
	void universalBaselineCritsBelowTenPercent() {
		assertEquals(CritRoll.Verdict.CRIT, roll(UNIVERSAL, 0, 0.099F).verdict());
		assertEquals(CritRoll.Verdict.NORMAL, roll(UNIVERSAL, 0, 0.10F).verdict());
		assertEquals(CritRule.universal().critMult(), roll(UNIVERSAL, 0, 0.0F).mult(), 1e-9);
		assertEquals(1.25, roll(UNIVERSAL, 0, 0.0F).mult(), 1e-9);
	}

	@Test
	void universalBaselineHasNoBetrayalAndNoStreakMovement() {
		CritRoll.Result crit = roll(UNIVERSAL, 3, 0.0F);
		CritRoll.Result normal = roll(UNIVERSAL, 3, 0.5F);

		assertEquals(CritRoll.Verdict.CRIT, crit.verdict());
		assertEquals(CritRoll.Movement.NONE, crit.movement());
		assertEquals(CritRoll.Verdict.NORMAL, normal.verdict());
		assertEquals(CritRoll.Movement.NONE, normal.movement());
		assertFalse(CritRule.universal().hasDice());
	}

	@Test
	void halfSwingsNeverRoll() {
		CritRoll.Result spam = CritRoll.roll(DICE, 4, 0.5F, 0.0F, false);

		assertEquals(CritRoll.Verdict.NONE, spam.verdict());
		assertEquals(1.0, spam.mult(), 1e-9);
		assertEquals(CritRoll.Movement.NONE, spam.movement());
	}

	@Test
	void diceBandsAtStreakZero() {
		CritRoll.Result betrayal = roll(DICE, 0, 0.049F);
		CritRoll.Result crit = roll(DICE, 0, 0.05F);
		CritRoll.Result critHigh = roll(DICE, 0, 0.199F);
		CritRoll.Result normal = roll(DICE, 0, 0.21F);

		assertEquals(CritRoll.Verdict.BETRAYAL, betrayal.verdict());
		assertEquals(0.5, betrayal.mult(), 1e-9);
		assertEquals(CritRoll.Verdict.CRIT, crit.verdict());
		assertEquals(1.5, crit.mult(), 1e-9);
		assertEquals(CritRoll.Verdict.CRIT, critHigh.verdict());
		assertEquals(CritRoll.Verdict.NORMAL, normal.verdict());
	}

	@Test
	void streakBumpsOnNonCritsAndResetsOnCrits() {
		assertEquals(CritRoll.Movement.BUMP, roll(DICE, 0, 0.5F).movement());
		assertEquals(CritRoll.Movement.BUMP, roll(DICE, 0, 0.049F).movement());
		assertEquals(CritRoll.Movement.RESET, roll(DICE, 0, 0.05F).movement());
	}

	@Test
	void streakLoadsTheCritBandAndLocksAtTheCap() {
		// Streak 3: the crit band tops out at 0.65.
		assertEquals(CritRoll.Verdict.CRIT, roll(DICE, 3, 0.64F).verdict());
		assertEquals(CritRoll.Verdict.NORMAL, roll(DICE, 3, 0.66F).verdict());

		// Streak 4: still a normal band above 0.80; streak 6 fills the wheel and the normal band is gone.
		assertEquals(CritRoll.Verdict.NORMAL, roll(DICE, 4, 0.81F).verdict());
		assertEquals(CritRoll.Verdict.CRIT, roll(DICE, 5, 0.94F).verdict());
		assertEquals(CritRoll.Verdict.CRIT, roll(DICE, 6, 0.9999F).verdict());
		assertEquals(CritRoll.Verdict.CRIT, roll(DICE, 9, 0.9999F).verdict());
		// Betrayal keeps its fixed 5% forever: the lock never swallows it.
		assertEquals(CritRoll.Verdict.BETRAYAL, roll(DICE, 9, 0.049F).verdict());
	}

	@Test
	void allInLiftsTheCritMultAndBendsNothingElse() {
		CritRoll.Result crit = CritRoll.roll(DICE, 0, 1.0F, 0.10F, true);
		CritRoll.Result betrayal = CritRoll.roll(DICE, 0, 1.0F, 0.01F, true);
		CritRoll.Result normal = CritRoll.roll(DICE, 0, 1.0F, 0.5F, true);

		assertEquals(CritRoll.Verdict.CRIT, crit.verdict());
		assertEquals(AllInAbility.CRIT_MULT, crit.mult(), 1e-9);
		assertEquals(CritRoll.Movement.RESET, crit.movement());
		assertEquals(CritRoll.Verdict.BETRAYAL, betrayal.verdict());
		assertEquals(0.5, betrayal.mult(), 1e-9);
		assertEquals(CritRoll.Movement.BUMP, betrayal.movement());
		assertEquals(CritRoll.Verdict.NORMAL, normal.verdict());
		assertEquals(1.0, normal.mult(), 1e-9);
		assertEquals(CritRoll.Movement.BUMP, normal.movement());
	}

	@Test
	void generatedDiceCopyMatchesTheRuleNumbers() {
		assertEquals("Full-charge hits: 15% crit x1.5, 5% betrayal x0.5.", DICE.crookedDiceDesc());
		assertEquals("Non-crit hit: +15% crit (a crit resets to 15%). Locks at 95% from streak 6.",
				DICE.streakDesc());
		assertTrue(DICE.hasDice());
	}
}
