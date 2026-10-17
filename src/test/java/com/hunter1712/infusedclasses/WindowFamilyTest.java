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

		runtime.flipStance(0, RotateAbility.Stance.GLADIUS);
		assertEquals(RotateAbility.auraFor(RotateAbility.Stance.GLADIUS),
				PassiveApplier.stanceWanted(runtime.state(0)));

		runtime.tryUseAequilibrium(0);
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

		runtime.tryUseRoulette(0, RouletteAbility.Outcome.EDGE);
		assertEquals(RouletteAbility.statAuraFor(RouletteAbility.Outcome.EDGE),
				PassiveApplier.rouletteWanted(runtime.state(0)));
		assertEquals(List.of(), PassiveApplier.rouletteWanted(runtime.state(10_000)));
	}

	@Test
	void rushWantsItsSpeedOnlyWhileTheWindowIsLive() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(List.of(), PassiveApplier.rushWanted(runtime.state(0)));

		runtime.tryUseRush(0);
		assertEquals(List.of(RushAbility.SPEED), PassiveApplier.rushWanted(runtime.state(0)));
		assertEquals(List.of(RushAbility.SPEED), PassiveApplier.rushWanted(runtime.state(4_999)));
		assertEquals(List.of(), PassiveApplier.rushWanted(runtime.state(5_000)));
	}
}
