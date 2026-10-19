package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The applier's one window-aura family table: what each family wants for a runtime state.
 * The pure half of the reconcile — the write/remove pass needs a player and stays with it.
 */
class WindowFamilyTest {
	@Test
	void stanceWantsTheCurrentStanceAndTheOathWindowWhenLive() {
		PlayerRuntime runtime = new PlayerRuntime();

		assertEquals(RotateAbility.auraFor(RotateAbility.Stance.SCUTUM),
				PassiveApplier.stanceWanted(runtime.state(0)));

		runtime.rotate(0, RotateAbility.Stance.GLADIUS);
		assertEquals(RotateAbility.auraFor(RotateAbility.Stance.GLADIUS),
				PassiveApplier.stanceWanted(runtime.state(0)));

		runtime.arm(Lane.OATH, 0, AequilibriumAbility.WINDOW_TICKS * 50L);
		List<PassiveEffect.Modifier> expected = new ArrayList<>(RotateAbility.auraFor(RotateAbility.Stance.GLADIUS));
		expected.addAll(AequilibriumAbility.windowModifiers());
		assertEquals(expected, PassiveApplier.stanceWanted(runtime.state(0)));
		// The window's read is the state's, so it drops at its boundary like every other window.
		assertEquals(RotateAbility.auraFor(RotateAbility.Stance.GLADIUS),
				PassiveApplier.stanceWanted(runtime.state(6_000)));
	}

	@Test
	void wheelWantsOnlyItsLiveSpin() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(List.of(), PassiveApplier.rouletteWanted(runtime.state(0)));

		runtime.roulette(0, RouletteAbility.Outcome.EDGE);
		assertEquals(RouletteAbility.statAuraFor(RouletteAbility.Outcome.EDGE),
				PassiveApplier.rouletteWanted(runtime.state(0)));
		assertEquals(List.of(), PassiveApplier.rouletteWanted(runtime.state(10_000)));
	}

	@Test
	void offloadWantsItsPairOnlyWhileTheWindowIsLive() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(List.of(), PassiveApplier.offloadWanted(runtime.state(0)));

		runtime.arm(Lane.OFFLOAD, 0, OffloadAbility.WINDOW_TICKS * 50L);
		assertEquals(OffloadAbility.windowModifiers(), PassiveApplier.offloadWanted(runtime.state(0)));
		assertEquals(OffloadAbility.windowModifiers(), PassiveApplier.offloadWanted(runtime.state(4_999)));
		assertEquals(List.of(), PassiveApplier.offloadWanted(runtime.state(5_000)));
	}
}
