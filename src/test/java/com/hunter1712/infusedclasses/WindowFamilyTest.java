package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each window ability's family row: its marker, its full sweep set and what the live state
 * wants. The pure half of the reconcile — the write/remove pass needs a player and stays
 * with the applier.
 */
class WindowFamilyTest {
	@Test
	void rotateWantsTheCurrentStanceOnly() {
		PlayerRuntime runtime = new PlayerRuntime();
		WindowFamily family = RotateAbility.windowFamily();

		assertEquals(ClassRegistry.ROTATE_ABILITY, family.marker());
		assertEquals(RotateAbility.auraFor(RotateAbility.Stance.SCUTUM), family.wanted().apply(runtime.state(0)));

		runtime.rotate(0, RotateAbility.Stance.GLADIUS);
		assertEquals(RotateAbility.auraFor(RotateAbility.Stance.GLADIUS), family.wanted().apply(runtime.state(0)));

		// The full set holds both stances, so the reconcile can take the other one off.
		assertTrue(family.all().containsAll(RotateAbility.auraFor(RotateAbility.Stance.SCUTUM)));
		assertTrue(family.all().containsAll(RotateAbility.auraFor(RotateAbility.Stance.GLADIUS)));
	}

	@Test
	void oathFamilyWantsItsPlateOnlyWhileItsWindowIsLive() {
		PlayerRuntime runtime = new PlayerRuntime();
		WindowFamily family = AequilibriumAbility.windowFamily();

		assertEquals(ClassRegistry.AEQUILIBRIUM_ABILITY, family.marker());
		assertEquals(List.of(), family.wanted().apply(runtime.state(0)));

		runtime.arm(Lane.OATH, 0, AequilibriumAbility.WINDOW_TICKS * 50L);
		assertEquals(AequilibriumAbility.windowModifiers(), family.wanted().apply(runtime.state(0)));
		// The window's read is the state's, so it drops at its boundary like every other window.
		assertEquals(List.of(), family.wanted().apply(runtime.state(6_000)));
	}

	@Test
	void wheelWantsOnlyItsLiveSpin() {
		PlayerRuntime runtime = new PlayerRuntime();
		WindowFamily family = RouletteAbility.windowFamily();

		assertEquals(ClassRegistry.ROULETTE_ABILITY, family.marker());
		assertEquals(List.of(), family.wanted().apply(runtime.state(0)));

		runtime.roulette(0, RouletteAbility.Outcome.EDGE);
		assertEquals(RouletteAbility.statAuraFor(RouletteAbility.Outcome.EDGE), family.wanted().apply(runtime.state(0)));
		assertEquals(List.of(), family.wanted().apply(runtime.state(10_000)));
	}

	@Test
	void offloadWantsItsPairOnlyWhileTheWindowIsLive() {
		PlayerRuntime runtime = new PlayerRuntime();
		WindowFamily family = OffloadAbility.windowFamily();

		assertEquals(ClassRegistry.OFFLOAD_ABILITY, family.marker());
		assertEquals(List.of(), family.wanted().apply(runtime.state(0)));

		runtime.arm(Lane.OFFLOAD, 0, OffloadAbility.WINDOW_TICKS * 50L);
		assertEquals(OffloadAbility.windowModifiers(), family.wanted().apply(runtime.state(0)));
		assertEquals(OffloadAbility.windowModifiers(), family.wanted().apply(runtime.state(4_999)));
		assertEquals(List.of(), family.wanted().apply(runtime.state(5_000)));
	}
}
