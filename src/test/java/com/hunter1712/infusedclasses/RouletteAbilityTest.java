package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
		assertEquals(RouletteAbility.Outcome.EDGE, RouletteAbility.outcomeFor(0.249F));
		assertEquals(RouletteAbility.Outcome.WARD, RouletteAbility.outcomeFor(0.25F));
		assertEquals(RouletteAbility.Outcome.WARD, RouletteAbility.outcomeFor(0.499F));
		assertEquals(RouletteAbility.Outcome.HASTE, RouletteAbility.outcomeFor(0.50F));
		assertEquals(RouletteAbility.Outcome.HASTE, RouletteAbility.outcomeFor(0.749F));
		assertEquals(RouletteAbility.Outcome.JACKPOT, RouletteAbility.outcomeFor(0.75F));
		assertEquals(RouletteAbility.Outcome.JACKPOT, RouletteAbility.outcomeFor(0.849F));
		assertEquals(RouletteAbility.Outcome.BUST, RouletteAbility.outcomeFor(0.85F));
		assertEquals(RouletteAbility.Outcome.BUST, RouletteAbility.outcomeFor(0.949F));
		assertEquals(RouletteAbility.Outcome.ZERO, RouletteAbility.outcomeFor(0.95F));
		assertEquals(RouletteAbility.Outcome.ZERO, RouletteAbility.outcomeFor(1.0F));
	}

	@Test
	void bandOddsReadOffTheTopsAndFillTheWheel() {
		int total = 0;
		for (RouletteAbility.Outcome outcome : RouletteAbility.Outcome.values()) {
			total += outcome.percent();
		}

		assertEquals(100, total);
		assertEquals(25, RouletteAbility.Outcome.EDGE.percent());
		assertEquals(25, RouletteAbility.Outcome.WARD.percent());
		assertEquals(25, RouletteAbility.Outcome.HASTE.percent());
		assertEquals(10, RouletteAbility.Outcome.JACKPOT.percent());
		assertEquals(10, RouletteAbility.Outcome.BUST.percent());
		assertEquals(5, RouletteAbility.Outcome.ZERO.percent());
	}

	@Test
	void goodAndBadBandsCarryTheirMults() {
		assertEquals(RouletteAbility.EDGE_MULT, RouletteAbility.Outcome.EDGE.dealMult(), 1e-9);
		assertEquals(RouletteAbility.EDGE_MULT, RouletteAbility.Outcome.JACKPOT.dealMult(), 1e-9);
		assertEquals(RouletteAbility.BUST_MULT, RouletteAbility.Outcome.BUST.dealMult(), 1e-9);
		assertEquals(1.0, RouletteAbility.Outcome.WARD.dealMult(), 1e-9);
		assertEquals(1.0, RouletteAbility.Outcome.ZERO.dealMult(), 1e-9);
	}

	@Test
	void onlyWardHasteAndJackpotStatTheWindow() {
		List<PassiveEffect.Modifier> all = RouletteAbility.allStatModifiers();

		assertEquals(1, RouletteAbility.statAuraFor(RouletteAbility.Outcome.WARD).size());
		assertEquals(1, RouletteAbility.statAuraFor(RouletteAbility.Outcome.HASTE).size());
		assertEquals(2, RouletteAbility.statAuraFor(RouletteAbility.Outcome.JACKPOT).size());
		for (RouletteAbility.Outcome quiet : List.of(RouletteAbility.Outcome.EDGE,
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

		assertTrue(result.outcome().fired());
		assertEquals(RouletteAbility.Outcome.HASTE, result.band());
		assertNotNull(result.outcome().announcement());
		assertTrue(result.outcome().announcement().text().startsWith("Haste"));
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

		assertFalse(result.outcome().fired());
		assertNull(result.band());
		assertNull(result.outcome().announcement());
		assertEquals(0, runtime.rouletteRemaining(0));
		assertNull(runtime.rouletteOutcome(0));
	}

	@Test
	void cooldownRejectSpinsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		RouletteAbility.fire(gambler(), runtime, 1_000, 0.0F); // Edge

		RouletteAbility.Result second = RouletteAbility.fire(gambler(), runtime, 1_001, 0.5F);

		assertFalse(second.outcome().fired());
		assertEquals(Cooldowns.ROULETTE_MS - 1, second.outcome().remainingMs());
		assertNull(second.band());
		// Nothing was re-stamped: the first spin's outcome still stands.
		assertEquals(RouletteAbility.Outcome.EDGE, runtime.rouletteOutcome(1_001));
		// And the wheel fires again exactly when its 25s lane runs out.
		assertTrue(RouletteAbility.fire(gambler(), runtime, 26_000, 1.0F).outcome().fired());
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
		assertEquals("One roll: Edge 25% x1.25, Ward 25% +2 armor, Haste 25% +0.02 speed,"
				+ " Jackpot 10% all, Bust 10% x0.75, Zero 5%; 10s (R, 25s).",
				RouletteAbility.desc());
	}
}
