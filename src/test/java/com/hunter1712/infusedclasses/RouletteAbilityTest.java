package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Roulette decisions: one roll's band cuts, the free gate-and-arm fire with its window,
 * and the reject paths that arm nothing. No HP cost exists anywhere in the wheel.
 */
class RouletteAbilityTest {
	private static ClassDefinition gambler() {
		return ClassRegistry.byId(ClassRegistry.GAMBLER);
	}

	@Test
	void bandsCutOnThePublishedTops() {
		assertEquals(RouletteAbility.Outcome.EDGE, RouletteAbility.outcomeFor(0.199F));
		assertEquals(RouletteAbility.Outcome.WARD, RouletteAbility.outcomeFor(0.20F));
		assertEquals(RouletteAbility.Outcome.WARD, RouletteAbility.outcomeFor(0.399F));
		assertEquals(RouletteAbility.Outcome.HASTE, RouletteAbility.outcomeFor(0.40F));
		assertEquals(RouletteAbility.Outcome.HASTE, RouletteAbility.outcomeFor(0.599F));
		assertEquals(RouletteAbility.Outcome.VIGOR, RouletteAbility.outcomeFor(0.60F));
		assertEquals(RouletteAbility.Outcome.VIGOR, RouletteAbility.outcomeFor(0.799F));
		assertEquals(RouletteAbility.Outcome.JACKPOT, RouletteAbility.outcomeFor(0.80F));
		assertEquals(RouletteAbility.Outcome.JACKPOT, RouletteAbility.outcomeFor(0.879F));
		assertEquals(RouletteAbility.Outcome.BUST, RouletteAbility.outcomeFor(0.88F));
		assertEquals(RouletteAbility.Outcome.BUST, RouletteAbility.outcomeFor(0.959F));
		assertEquals(RouletteAbility.Outcome.ZERO, RouletteAbility.outcomeFor(0.96F));
		assertEquals(RouletteAbility.Outcome.ZERO, RouletteAbility.outcomeFor(1.0F));
	}

	@Test
	void bandOddsReadOffTheTopsAndFillTheWheel() {
		int total = 0;
		for (RouletteAbility.Outcome outcome : RouletteAbility.Outcome.values()) {
			total += outcome.percent();
		}

		assertEquals(100, total);
		assertEquals(20, RouletteAbility.Outcome.EDGE.percent());
		assertEquals(20, RouletteAbility.Outcome.VIGOR.percent());
		assertEquals(8, RouletteAbility.Outcome.JACKPOT.percent());
		assertEquals(4, RouletteAbility.Outcome.ZERO.percent());
	}

	@Test
	void goodAndBadBandsCarryTheirMultsAndHeals() {
		assertEquals(RouletteAbility.EDGE_MULT, RouletteAbility.Outcome.EDGE.dealMult(), 1e-9);
		assertEquals(RouletteAbility.EDGE_MULT, RouletteAbility.Outcome.JACKPOT.dealMult(), 1e-9);
		assertEquals(RouletteAbility.BUST_MULT, RouletteAbility.Outcome.BUST.dealMult(), 1e-9);
		assertEquals(1.0, RouletteAbility.Outcome.WARD.dealMult(), 1e-9);
		assertEquals(1.0, RouletteAbility.Outcome.ZERO.dealMult(), 1e-9);
		assertEquals(RouletteAbility.VIGOR_HEAL, RouletteAbility.Outcome.VIGOR.healHp(), 1e-9);
		assertEquals(RouletteAbility.JACKPOT_HEAL, RouletteAbility.Outcome.JACKPOT.healHp(), 1e-9);
		assertEquals(0, RouletteAbility.Outcome.EDGE.healHp(), 1e-9);
		assertEquals(0, RouletteAbility.Outcome.BUST.healHp(), 1e-9);
	}

	@Test
	void onlyWardHasteAndJackpotStatTheWindow() {
		List<PassiveEffect.Modifier> all = RouletteAbility.allStatModifiers();

		assertEquals(1, RouletteAbility.statAuraFor(RouletteAbility.Outcome.WARD).size());
		assertEquals(1, RouletteAbility.statAuraFor(RouletteAbility.Outcome.HASTE).size());
		assertEquals(2, RouletteAbility.statAuraFor(RouletteAbility.Outcome.JACKPOT).size());
		for (RouletteAbility.Outcome quiet : List.of(RouletteAbility.Outcome.EDGE, RouletteAbility.Outcome.VIGOR,
				RouletteAbility.Outcome.BUST, RouletteAbility.Outcome.ZERO)) {
			assertTrue(RouletteAbility.statAuraFor(quiet).isEmpty(), quiet + " grants no stat");
		}
		// The apply and remove paths iterate the same set, so an aura can never be written but never taken off.
		assertTrue(all.containsAll(RouletteAbility.statAuraFor(RouletteAbility.Outcome.JACKPOT)));
	}

	@Test
	void freeFireArmsLaneAndStampsTheOutcomeWindow() {
		PlayerRuntime runtime = new PlayerRuntime();

		RouletteAbility.Result result = RouletteAbility.fire(gambler(), runtime, 0, 0.5F);

		assertEquals(RouletteAbility.Kind.FIRED, result.kind());
		assertEquals(RouletteAbility.Outcome.HASTE, result.outcome());
		assertNotNull(result.announcement());
		assertTrue(result.announcement().text().startsWith("Haste"));
		assertEquals(Cooldowns.ROULETTE_MS, runtime.rouletteRemaining(0));
		assertEquals(RouletteAbility.Outcome.HASTE, runtime.rouletteOutcome(0));
		assertEquals(10, RouletteAbility.WINDOW_TICKS / 20);
	}

	@Test
	void theOutcomeWindowExpiresTenSecondsAfterTheSpin() {
		PlayerRuntime runtime = new PlayerRuntime();
		RouletteAbility.fire(gambler(), runtime, 0, 0.1F); // Edge

		assertEquals(RouletteAbility.Outcome.EDGE, runtime.rouletteOutcome(9_999));
		assertNull(runtime.rouletteOutcome(10_000));
		// The lane is the wheel's own clock: still counting down when the window dies.
		assertEquals(Cooldowns.ROULETTE_MS - 10_000, runtime.rouletteRemaining(10_000));
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();

		RouletteAbility.Result result = RouletteAbility.fire(
				ClassRegistry.byId(ClassRegistry.JUGGERNAUT), runtime, 0, 0.1F);

		assertEquals(RouletteAbility.Kind.WRONG_CLASS, result.kind());
		assertNull(result.outcome());
		assertNull(result.announcement());
		assertEquals(0, runtime.rouletteRemaining(0));
		assertNull(runtime.rouletteOutcome(0));
	}

	@Test
	void cooldownRejectSpinsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		RouletteAbility.fire(gambler(), runtime, 1_000, 0.0F); // Edge

		RouletteAbility.Result second = RouletteAbility.fire(gambler(), runtime, 1_001, 0.5F);

		assertEquals(RouletteAbility.Kind.ON_COOLDOWN, second.kind());
		assertEquals(Cooldowns.ROULETTE_MS - 1, second.remainingMs());
		assertNull(second.outcome());
		// Nothing was re-stamped: the first spin's outcome still stands.
		assertEquals(RouletteAbility.Outcome.EDGE, runtime.rouletteOutcome(1_001));
		// And the wheel fires again exactly when its 25s lane runs out.
		assertEquals(RouletteAbility.Kind.FIRED, RouletteAbility.fire(gambler(), runtime, 26_000, 1.0F).kind());
	}

	@Test
	void theWheelNeverTouchesStreak() {
		PlayerRuntime runtime = new PlayerRuntime();
		RouletteAbility.fire(gambler(), runtime, 0, 0.1F);
		RouletteAbility.fire(gambler(), runtime, 0, 0.5F);
		RouletteAbility.fire(null, runtime, 0, 0.5F);

		assertEquals(0, runtime.streak());
	}

	@Test
	void descIsGeneratedFromTheNumbers() {
		assertEquals("One roll: Edge 20% x1.25, Ward 20% +2 armor, Haste 20% +0.02 speed,"
				+ " Vigor 20% heal 3, Jackpot 8% all + heal 4, Bust 8% x0.75, Zero 4%;"
				+ " 10s (R, 25s).", RouletteAbility.desc());
	}
}
