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

		AllInAbility.Result result = AllInAbility.fire(gambler(), runtime, 0);

		assertEquals(AllInAbility.Kind.FIRED, result.kind());
		assertNotNull(result.announcement());
		assertEquals(Cooldowns.ALL_IN_MS, runtime.allInRemaining(0));
		assertTrue(runtime.isAllInLive(0));
		assertTrue(runtime.isAllInLive(14_999));
		assertFalse(runtime.isAllInLive(15_000));
		assertEquals(15, AllInAbility.WINDOW_TICKS / 20);
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();

		AllInAbility.Result result = AllInAbility.fire(
				ClassRegistry.byId(ClassRegistry.JUGGERNAUT), runtime, 0);

		assertEquals(AllInAbility.Kind.WRONG_CLASS, result.kind());
		assertNull(result.announcement());
		assertEquals(0, runtime.allInRemaining(0));
		assertFalse(runtime.isAllInLive(0));
	}

	@Test
	void cooldownRejectArmsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		AllInAbility.fire(gambler(), runtime, 1_000);

		AllInAbility.Result second = AllInAbility.fire(gambler(), runtime, 40_000);

		assertEquals(AllInAbility.Kind.ON_COOLDOWN, second.kind());
		assertEquals(Cooldowns.ALL_IN_MS - 39_000, second.remainingMs());
		// The first window is long gone; the reject did not restamp it.
		assertFalse(runtime.isAllInLive(40_000));
	}

	@Test
	void gamblerLanesAdvanceIndependently() {
		PlayerRuntime runtime = new PlayerRuntime();

		AllInAbility.fire(gambler(), runtime, 0);
		assertEquals(RouletteAbility.Kind.FIRED, RouletteAbility.fire(gambler(), runtime, 0, 0.1F).kind());

		// The wheel fired at 0 and did not touch the All In lane; the ultimate did not touch Roulette's.
		assertEquals(AllInAbility.Kind.ON_COOLDOWN, AllInAbility.fire(gambler(), runtime, 1).kind());
		assertEquals(Cooldowns.ROULETTE_MS, runtime.rouletteRemaining(0));
	}

	@Test
	void descIsGeneratedFromTheNumbers() {
		assertEquals("Crits x2 for 15s (G, 75s). Dice rules unchanged.", AllInAbility.desc());
	}
}
