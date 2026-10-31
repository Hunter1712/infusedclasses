package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The declared balance budget: one row per converted damage power, one row per movement
 * flat, plus the tuning round's four knobs. This pins what the spec's sheet
 * (.scratch/balance-pass/spec.md) records as the declared constants — the iron-sword
 * reference match (x1.15 at 6 damage), the 0.005 movement grid and the tuned knobs —
 * never a simulation.
 */
class BalanceBudgetTest {
	/** The reference kit: an iron sword's 6 damage. */
	private static final double IRON_SWORD_DAMAGE = 6.0;

	@Test
	void everyConvertedDamageRowDeclaresTheIronSwordMatch() {
		List<Double> converted = List.of(
				Burden.DEAL_MULT,
				RotateAbility.GLADIUS_DEAL_MULT,
				AequilibriumAbility.DEAL_MULT,
				OnslaughtAbility.DEAL_MULT);

		for (double mult : converted) {
			assertEquals(1.0 + 1.0 / IRON_SWORD_DAMAGE, mult, 0.02,
					"a converted row must stay near the iron-sword match");
		}
	}

	@Test
	void theFourConvertedRowsAgreeWithEachOther() {
		double chassis = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).chassisDealMult();

		assertEquals(Burden.DEAL_MULT, chassis, 1e-9);
		assertEquals(Burden.DEAL_MULT, RotateAbility.GLADIUS_DEAL_MULT, 1e-9);
		assertEquals(Burden.DEAL_MULT, AequilibriumAbility.DEAL_MULT, 1e-9);
		assertEquals(Burden.DEAL_MULT, OnslaughtAbility.DEAL_MULT, 1e-9);
	}

	@Test
	void movementRowsSitOnTheFlatGrid() {
		assertEquals(List.of(-0.005, -0.01, -0.015),
				Momentum.TIERS.stream().map(ConditionalPassive.Tier::weight).toList());
		assertEquals(-0.005, RotateAbility.SCUTUM_WEIGHT, 1e-9);
		assertEquals(0.01, OffloadAbility.SPEED_BONUS, 1e-9);
		assertEquals(0.02, RouletteAbility.HASTE_SPEED, 1e-9);
	}

	@Test
	void theTuningRoundHoldsItsDeclaredRows() {
		// The sheet's candidate set: Juggernaut -0.4 swing, Gladius take x1.2, Gambler
		// 15% crit base and x1.75 All In. The model measures; these pin what it measured.
		assertEquals(-0.4, Burden.SWING_PENALTY, 1e-9);
		assertEquals(1.2, RotateAbility.GLADIUS_TAKEN_MULT, 1e-9);
		assertEquals(0.15, CritRule.crookedDice().critChance(), 1e-9);
		assertEquals(1.75, AllInAbility.CRIT_MULT, 1e-9);
	}
}
