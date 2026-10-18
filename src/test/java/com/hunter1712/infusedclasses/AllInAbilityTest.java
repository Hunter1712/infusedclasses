package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * All In decisions: the Gambler-only ultimate on its 75s lane. Fires arm the lane and
 * the 15s window together; rejects arm nothing; the Roulette lane never notices.
 */
class AllInAbilityTest {
	private static ClassDefinition gambler() {
		return ClassRegistry.byId(ClassRegistry.GAMBLER);
	}

	@Test
	void fireArmsLaneAndFifteenSecondWindow() {
		PlayerRuntime runtime = new PlayerRuntime();

		AbilityOutcome result = AllInAbility.fire(gambler(), runtime, 0);

		assertTrue(result.fired());
		assertNotNull(result.announcement());
		assertEquals(Cooldowns.ALL_IN_MS, runtime.state(0).allInRemaining());
		assertTrue(runtime.state(0).allInWindowRemaining() > 0);
		assertTrue(runtime.state(14_999).allInWindowRemaining() > 0);
		assertFalse(runtime.state(15_000).allInWindowRemaining() > 0);
		assertEquals(15, AllInAbility.WINDOW_TICKS / 20);
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();

		AbilityOutcome result = AllInAbility.fire(
				ClassRegistry.byId(ClassRegistry.JUGGERNAUT), runtime, 0);

		assertFalse(result.fired());
		assertNull(result.announcement());
		assertEquals(0, runtime.state(0).allInRemaining());
		assertFalse(runtime.state(0).allInWindowRemaining() > 0);
	}

	@Test
	void cooldownRejectArmsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		AllInAbility.fire(gambler(), runtime, 1_000);

		AbilityOutcome second = AllInAbility.fire(gambler(), runtime, 40_000);

		assertFalse(second.fired());
		assertNull(second.announcement());
		// The first window is long gone; the reject did not restamp it.
		assertFalse(runtime.state(40_000).allInWindowRemaining() > 0);
	}

	@Test
	void gamblerLanesAdvanceIndependently() {
		PlayerRuntime runtime = new PlayerRuntime();

		AllInAbility.fire(gambler(), runtime, 0);
		assertTrue(RouletteAbility.fire(gambler(), runtime, 0, 0.1F).outcome().fired());

		// The wheel fired at 0 and did not touch the All In lane; the ultimate did not touch Roulette's.
		assertFalse(AllInAbility.fire(gambler(), runtime, 1).fired());
		assertEquals(Cooldowns.ROULETTE_MS, runtime.state(0).rouletteRemaining());
	}

	@Test
	void descIsGeneratedFromTheNumbers() {
		assertEquals("Crits x2 for 15s (V, 75s). Dice rules unchanged.", AllInAbility.desc());
	}
}
