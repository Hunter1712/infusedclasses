package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The strip's rows decision: given the last snapshot and the millis elapsed since it
 * arrived, what the strip shows. Pure, so every boundary is pinned here instead of in
 * a running client; the HUD element only draws what this returns.
 */
class StatusHudTest {
	private static final long NOW = 1_000_000L;

	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	private static ClassDefinition gambler() {
		return ClassRegistry.byId(ClassRegistry.GAMBLER);
	}

	private static PlayerStatusPayload status(Identifier id, PlayerRuntime runtime, long now) {
		return PlayerStatusPayload.of(id, runtime, now);
	}

	private static StatusHud.Row row(List<StatusHud.Row> rows, String badge) {
		for (StatusHud.Row row : rows) {
			if (row.chips().get(0).badge().equals(badge)) {
				return row;
			}
		}
		throw new AssertionError("no row anchored by " + badge + " in " + rows);
	}

	private static StatusHud.Chip chip(StatusHud.Row row, String badge) {
		for (StatusHud.Chip chip : row.chips()) {
			if (chip.badge().equals(badge)) {
				return chip;
			}
		}
		throw new AssertionError("no chip " + badge + " in " + row);
	}

	/** The rider riding a lane chip: chips are lane first, riders after. */
	private static StatusHud.Chip rider(StatusHud.Row row) {
		assertTrue(row.chips().size() > 1, "expected a rider beside " + row);
		return row.chips().get(1);
	}

	@Test
	void laneRowsAreAlwaysPresentDimWhileReady() {
		PlayerRuntime runtime = new PlayerRuntime();
		List<StatusHud.Row> rows = StatusHud.rows(status(ClassRegistry.JUGGERNAUT, runtime, NOW), 0);

		assertEquals(2, rows.size());
		StatusHud.Chip sig = row(rows, StatusHud.SIG).chips().get(0);
		assertEquals("SIG", sig.badge());
		assertEquals("Rush", sig.label());
		assertEquals("", sig.value());
		assertFalse(sig.lit());
		StatusHud.Chip ult = row(rows, StatusHud.ULT).chips().get(0);
		assertEquals("ULT", ult.badge());
		assertEquals("Rend", ult.label());
		assertEquals("", ult.value());
		assertFalse(ult.lit());
	}

	@Test
	void laneRowCountsDownLitWhileCooling() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, NOW);
		PlayerStatusPayload fired = status(ClassRegistry.JUGGERNAUT, runtime, NOW);

		StatusHud.Chip fresh = row(StatusHud.rows(fired, 0), StatusHud.SIG).chips().get(0);
		assertEquals("25.0s", fresh.value());
		assertTrue(fresh.lit());

		StatusHud.Chip later = row(StatusHud.rows(fired, 1_000), StatusHud.SIG).chips().get(0);
		assertEquals("24.0s", later.value());
		assertTrue(later.lit());

		StatusHud.Chip ready = row(StatusHud.rows(fired, 25_000), StatusHud.SIG).chips().get(0);
		assertEquals("", ready.value());
		assertFalse(ready.lit());
	}

	@Test
	void strideRiderKeepsCountingWithPlusUntilTheSpend() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, NOW);
		PlayerStatusPayload loaded = status(ClassRegistry.JUGGERNAUT, runtime, NOW);

		StatusHud.Row sig = row(StatusHud.rows(loaded, 0), StatusHud.SIG);
		assertEquals(2, sig.chips().size());
		StatusHud.Chip loadedRider = rider(sig);
		assertEquals("", loadedRider.badge());
		assertEquals("", loadedRider.label());
		assertEquals("+5.0s", loadedRider.value());
		assertTrue(loadedRider.lit());

		runtime.consumeRushBonus(NOW);
		PlayerStatusPayload spent = status(ClassRegistry.JUGGERNAUT, runtime, NOW);

		StatusHud.Chip spentRider = rider(row(StatusHud.rows(spent, 0), StatusHud.SIG));
		assertEquals("5.0s", spentRider.value());

		assertEquals(1, row(StatusHud.rows(spent, 5_000), StatusHud.SIG).chips().size());
	}

	@Test
	void empowerRiderVanishesOnTheSpend() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRend(NOW, 2);
		PlayerStatusPayload loaded = status(ClassRegistry.JUGGERNAUT, runtime, NOW);

		StatusHud.Row ult = row(StatusHud.rows(loaded, 0), StatusHud.ULT);
		assertEquals(2, ult.chips().size());
		assertEquals("6.0s", rider(ult).value());

		runtime.consumeRendEmpower(NOW);
		PlayerStatusPayload spent = status(ClassRegistry.JUGGERNAUT, runtime, NOW);

		assertEquals(1, row(StatusHud.rows(spent, 0), StatusHud.ULT).chips().size());
	}

	@Test
	void impetusRiderVanishesOnTheSpendAndTestudoIsTimed() {
		PlayerRuntime runtime = new PlayerRuntime();
		RotateAbility.fire(legionary(), runtime, NOW);
		PlayerStatusPayload gladius = status(ClassRegistry.LEGIONARY, runtime, NOW);

		StatusHud.Row sig = row(StatusHud.rows(gladius, 0), StatusHud.SIG);
		StatusHud.Chip impetus = rider(sig);
		assertEquals("IMP", impetus.badge());
		assertEquals(RotateAbility.IMPETUS_NAME, impetus.label());
		assertEquals("5.0s", impetus.value());

		runtime.consumeImpetusBonus(NOW);
		PlayerStatusPayload spent = status(ClassRegistry.LEGIONARY, runtime, NOW);
		assertEquals(1, row(StatusHud.rows(spent, 0), StatusHud.SIG).chips().size());

		long nextFlip = NOW + Cooldowns.ROTATE_MS;
		RotateAbility.fire(legionary(), runtime, nextFlip);
		PlayerStatusPayload scutum = status(ClassRegistry.LEGIONARY, runtime, nextFlip);

		StatusHud.Row row = row(StatusHud.rows(scutum, 0), StatusHud.SIG);
		StatusHud.Chip testudo = rider(row);
		assertEquals("TES", testudo.badge());
		assertEquals(RotateAbility.TESTUDO_NAME, testudo.label());
		assertEquals("1.5s", testudo.value());

		assertEquals(1, row(StatusHud.rows(scutum, 1_500), StatusHud.SIG).chips().size());
	}

	@Test
	void aequilibriumRiderRidesTheUltimateRowAndExpires() {
		PlayerRuntime runtime = new PlayerRuntime();
		AequilibriumAbility.fire(legionary(), runtime, NOW);
		PlayerStatusPayload payload = status(ClassRegistry.LEGIONARY, runtime, NOW);

		StatusHud.Row ult = row(StatusHud.rows(payload, 0), StatusHud.ULT);
		StatusHud.Chip aeq = rider(ult);
		assertEquals("AEQ", aeq.badge());
		assertEquals("Aequilibrium", aeq.label());
		assertEquals("6.0s", aeq.value());

		assertEquals(1, row(StatusHud.rows(payload, 6_000), StatusHud.ULT).chips().size());
	}

	@Test
	void allInRiderRidesTheUltimateRow() {
		PlayerRuntime runtime = new PlayerRuntime();
		AllInAbility.fire(gambler(), runtime, NOW);
		PlayerStatusPayload payload = status(ClassRegistry.GAMBLER, runtime, NOW);

		StatusHud.Chip all = rider(row(StatusHud.rows(payload, 0), StatusHud.ULT));
		assertEquals("ALL", all.badge());
		assertEquals("All In", all.label());
		assertEquals("15.0s", all.value());
	}

	@Test
	void outcomeChipCarriesItsInitialAndCountdown() {
		assertOutcome(RouletteAbility.Outcome.EDGE, "E");
		assertOutcome(RouletteAbility.Outcome.WARD, "W");
		assertOutcome(RouletteAbility.Outcome.HASTE, "H");
		assertOutcome(RouletteAbility.Outcome.JACKPOT, "J");
		assertOutcome(RouletteAbility.Outcome.BUST, "B");
	}

	private static void assertOutcome(RouletteAbility.Outcome outcome, String initial) {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRoulette(NOW, outcome);
		PlayerStatusPayload payload = status(ClassRegistry.GAMBLER, runtime, NOW);

		StatusHud.Chip chip = rider(row(StatusHud.rows(payload, 0), StatusHud.SIG));
		assertEquals(initial, chip.badge());
		assertEquals(outcome.label(), chip.label());
		assertEquals("10.0s", chip.value());

		assertEquals(1, row(StatusHud.rows(payload, 10_000), StatusHud.SIG).chips().size());
	}

	@Test
	void zeroDrawsNoOutcomeChip() {
		PlayerRuntime zero = new PlayerRuntime();
		zero.tryUseRoulette(NOW, RouletteAbility.Outcome.ZERO);
		PlayerStatusPayload zeroPayload = status(ClassRegistry.GAMBLER, zero, NOW);

		assertEquals(1, row(StatusHud.rows(zeroPayload, 0), StatusHud.SIG).chips().size());
	}

	@Test
	void momentumRowCarriesStacksAndGraceAndVanishesAtZero() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, NOW, 1.0F);
		runtime.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, NOW, 1.0F);
		PlayerStatusPayload payload = status(ClassRegistry.JUGGERNAUT, runtime, NOW);

		StatusHud.Chip mom = row(StatusHud.rows(payload, 0), StatusHud.MOM).chips().get(0);
		assertEquals("MOM", mom.badge());
		assertEquals("Momentum", mom.label());
		assertEquals("2 5.0s", mom.value());
		assertTrue(mom.lit());

		StatusHud.Chip later = row(StatusHud.rows(payload, 1_000), StatusHud.MOM).chips().get(0);
		assertEquals("2 4.0s", later.value());

		PlayerRuntime empty = new PlayerRuntime();
		List<StatusHud.Row> rows = StatusHud.rows(status(ClassRegistry.JUGGERNAUT, empty, NOW), 0);
		assertThrows(AssertionError.class, () -> row(rows, StatusHud.MOM));
	}

	@Test
	void streakRowShowsOnlyAboveZero() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload none = status(ClassRegistry.GAMBLER, runtime, NOW);
		assertThrows(AssertionError.class, () -> row(StatusHud.rows(none, 0), StatusHud.STR));

		runtime.bumpStreak();
		PlayerStatusPayload one = status(ClassRegistry.GAMBLER, runtime, NOW);

		StatusHud.Chip str = row(StatusHud.rows(one, 0), StatusHud.STR).chips().get(0);
		assertEquals("STR", str.badge());
		assertEquals("Streak", str.label());
		assertEquals("1", str.value());
	}

	@Test
	void stanceRowIsAnchoredFirstAndFlips() {
		PlayerRuntime runtime = new PlayerRuntime();
		PlayerStatusPayload scutum = status(ClassRegistry.LEGIONARY, runtime, NOW);

		List<StatusHud.Row> before = StatusHud.rows(scutum, 0);
		StatusHud.Chip scutumChip = before.get(0).chips().get(0);
		assertEquals("SCU", scutumChip.badge());
		assertEquals("Scutum", scutumChip.label());

		RotateAbility.fire(legionary(), runtime, NOW);
		PlayerStatusPayload gladius = status(ClassRegistry.LEGIONARY, runtime, NOW);

		List<StatusHud.Row> after = StatusHud.rows(gladius, 0);
		StatusHud.Chip gladiusChip = after.get(0).chips().get(0);
		assertEquals("GLA", gladiusChip.badge());
		assertEquals("Gladius", gladiusChip.label());
	}

	@Test
	void rowOrderIsStanceSignatureUltimateState() {
		PlayerRuntime legion = new PlayerRuntime();
		assertEquals(List.of(StatusHud.SCU, StatusHud.SIG, StatusHud.ULT),
				StatusHud.rows(status(ClassRegistry.LEGIONARY, legion, NOW), 0).stream()
						.map(r -> r.chips().get(0).badge()).toList());

		PlayerRuntime juggernaut = new PlayerRuntime();
		juggernaut.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, NOW, 1.0F);
		assertEquals(List.of(StatusHud.SIG, StatusHud.ULT, StatusHud.MOM),
				StatusHud.rows(status(ClassRegistry.JUGGERNAUT, juggernaut, NOW), 0).stream()
						.map(r -> r.chips().get(0).badge()).toList());

		PlayerRuntime gambler = new PlayerRuntime();
		gambler.bumpStreak();
		assertEquals(List.of(StatusHud.SIG, StatusHud.ULT, StatusHud.STR),
				StatusHud.rows(status(ClassRegistry.GAMBLER, gambler, NOW), 0).stream()
						.map(r -> r.chips().get(0).badge()).toList());
	}

	@Test
	void classlessAndUnknownClassesRenderNoRows() {
		PlayerRuntime runtime = new PlayerRuntime();

		assertTrue(StatusHud.rows(PlayerStatusPayload.of(null, runtime, NOW), 0).isEmpty());
		assertTrue(StatusHud.rows(status(
				Identifier.fromNamespaceAndPath("infusedclasses", "nope"), runtime, NOW), 0).isEmpty());
	}

	@Test
	void countdownsPinTheirRoundingBoundariesAndClampAtZero() {
		PlayerRuntime gambler = new PlayerRuntime();
		gambler.tryUseRoulette(NOW, RouletteAbility.Outcome.EDGE);
		PlayerStatusPayload spin = status(ClassRegistry.GAMBLER, gambler, NOW);

		assertEquals("10.0s", rider(row(StatusHud.rows(spin, 0), StatusHud.SIG)).value());
		// 9_999 ms left rounds to the same text as 10_000: one decimal, not truncation.
		assertEquals("10.0s", rider(row(StatusHud.rows(spin, 1), StatusHud.SIG)).value());

		PlayerRuntime juggernaut = new PlayerRuntime();
		juggernaut.onTrigger(juggernaut(), ConditionalPassive.ON_HIT_DEALT, NOW, 1.0F);
		PlayerStatusPayload stacks = status(ClassRegistry.JUGGERNAUT, juggernaut, NOW);

		assertEquals("1 5.0s", chip(row(StatusHud.rows(stacks, 0), StatusHud.MOM), StatusHud.MOM).value());
		assertEquals("1 0.0s", chip(row(StatusHud.rows(stacks, 5_000), StatusHud.MOM), StatusHud.MOM).value());
		// Past grace the countdown clamps at zero; it never reads negative.
		assertEquals("1 0.0s", chip(row(StatusHud.rows(stacks, 9_999), StatusHud.MOM), StatusHud.MOM).value());
	}
}
