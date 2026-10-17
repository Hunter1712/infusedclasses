package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Per-passive decay, switch cleanup and the one-state read: the C2 deepening. */
class PlayerRuntimeDeepeningTest {
	private static ConditionalPassive momentum() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives().get(0);
	}

	private static ClassDefinition withPassives(ConditionalPassive... passives) {
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				List.of(passives), List.of(), 0, "", List.of(), List.of(), CritRule.universal());
	}

	@Test
	void decayedPassiveMovesWithoutTouchingIdleOne() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition def = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(def, "on_hit_dealt", base, 1.0F);
		// Swift never triggered: 0 stacks, never moved.

		Set<ConditionalPassive> moved = runtime.tickDecayMoved(def, base + 6_000);

		assertTrue(moved.contains(momentum()));
		assertFalse(moved.contains(swift));
		// Old global flag would have removed Swift spuriously; per-passive must not.
		assertTrue(PassiveEffect.planFor(swift, runtime.stacks(swift), moved.contains(swift))
				instanceof PassiveEffect.Plan.Skip);
	}

	@Test
	void clearDropsOnlyThatClassStacks() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition both = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(both, "on_hit_dealt", base, 1.0F);
		runtime.onTrigger(both, "on_land", base, 1.0F);
		assertEquals(1, runtime.stacks(momentum()));

		runtime.clear(withPassives(momentum()));

		assertEquals(0, runtime.stacks(momentum()));
		assertEquals(1, runtime.stacks(swift));
	}

	@Test
	void clearDropsOnlyThePricedPassive() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition both = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(both, "on_hit_dealt", base, 1.0F);
		runtime.onTrigger(both, "on_land", base, 1.0F);

		runtime.clear(momentum());

		assertEquals(0, runtime.stacks(momentum()));
		assertEquals(1, runtime.stacks(swift));
	}

	@Test
	void freshStateIsTheEmptyRead() {
		PlayerRuntime.State state = new PlayerRuntime().state(0);

		assertEquals(0, state.rushRemaining());
		assertEquals(0, state.rendRemaining());
		assertEquals(0, state.rotateRemaining());
		assertEquals(0, state.oathRemaining());
		assertEquals(0, state.rouletteRemaining());
		assertEquals(0, state.allInRemaining());
		assertEquals(0, state.rushWindowRemaining());
		assertFalse(state.rushBonusPending());
		assertEquals(0, state.rendWindowRemaining());
		assertEquals(0, state.rendBonusStacks());
		assertEquals(0, state.flipWindowRemaining());
		assertNull(state.flipTarget());
		assertFalse(state.impetusPending());
		assertEquals(0, state.rouletteWindowRemaining());
		assertNull(state.rouletteOutcome());
		assertEquals(0, state.aequilibriumWindowRemaining());
		assertEquals(0, state.allInWindowRemaining());
		assertEquals(RotateAbility.Stance.SCUTUM, state.stance());
		assertEquals(0, state.streak());
	}

	@Test
	void cooldownVerbsArmWithoutExposingMutation() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(0, runtime.state(0).rendRemaining());
		assertTrue(runtime.tryUseRend(0, 5));
		assertEquals(Cooldowns.REND_MS, runtime.state(0).rendRemaining());
		runtime.resetCooldowns();
		assertEquals(0, runtime.state(0).rendRemaining());
	}

	@Test
	void streakBumpsResetsAndWipesWithTheClass() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(0, runtime.state(0).streak());

		runtime.bumpStreak();
		runtime.bumpStreak();
		assertEquals(2, runtime.state(0).streak());

		runtime.resetStreak();
		assertEquals(0, runtime.state(0).streak());

		runtime.bumpStreak();
		runtime.resetCooldowns();
		assertEquals(0, runtime.state(0).streak());
	}

	@Test
	void rouletteWindowRoundTripsItsPayloadAndExpires() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertNull(runtime.state(0).rouletteOutcome());

		assertTrue(runtime.tryUseRoulette(1_000, RouletteAbility.Outcome.JACKPOT));

		assertEquals(RouletteAbility.Outcome.JACKPOT, runtime.state(1_000).rouletteOutcome());
		assertEquals(RouletteAbility.Outcome.JACKPOT, runtime.state(10_999).rouletteOutcome());
		assertNull(runtime.state(11_000).rouletteOutcome());
	}

	@Test
	void everyWindowReadsZeroWhenDeadAndItsExactBoundaryWhenLive() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000L;
		assertTrue(runtime.tryUseRush(base));
		assertTrue(runtime.tryUseRend(base, 4));
		assertTrue(runtime.tryUseRotate(base, RotateAbility.Stance.GLADIUS));
		assertTrue(runtime.tryUseAequilibrium(base));
		assertTrue(runtime.tryUseRoulette(base, RouletteAbility.Outcome.EDGE));
		assertTrue(runtime.tryUseAllIn(base));

		assertEquals(5_000, runtime.state(base).rushWindowRemaining());
		assertEquals(1, runtime.state(base + 4_999).rushWindowRemaining());
		assertEquals(0, runtime.state(base + 5_000).rushWindowRemaining());

		assertEquals(6_000, runtime.state(base).rendWindowRemaining());
		assertEquals(1, runtime.state(base + 5_999).rendWindowRemaining());
		assertEquals(0, runtime.state(base + 6_000).rendWindowRemaining());

		assertEquals(5_000, runtime.state(base).flipWindowRemaining());
		assertEquals(RotateAbility.Stance.GLADIUS, runtime.state(base).flipTarget());
		assertEquals(1, runtime.state(base + 4_999).flipWindowRemaining());
		assertEquals(0, runtime.state(base + 5_000).flipWindowRemaining());
		assertNull(runtime.state(base + 5_000).flipTarget());

		assertEquals(6_000, runtime.state(base).aequilibriumWindowRemaining());
		assertEquals(1, runtime.state(base + 5_999).aequilibriumWindowRemaining());
		assertEquals(0, runtime.state(base + 6_000).aequilibriumWindowRemaining());

		assertEquals(10_000, runtime.state(base).rouletteWindowRemaining());
		assertEquals(1, runtime.state(base + 9_999).rouletteWindowRemaining());
		assertEquals(0, runtime.state(base + 10_000).rouletteWindowRemaining());

		assertEquals(15_000, runtime.state(base).allInWindowRemaining());
		assertEquals(1, runtime.state(base + 14_999).allInWindowRemaining());
		assertEquals(0, runtime.state(base + 15_000).allInWindowRemaining());

		PlayerRuntime dead = new PlayerRuntime();
		assertEquals(0, dead.state(0).rushWindowRemaining());
		assertEquals(0, dead.state(0).rendWindowRemaining());
		assertEquals(0, dead.state(0).flipWindowRemaining());
		assertEquals(0, dead.state(0).aequilibriumWindowRemaining());
		assertEquals(0, dead.state(0).rouletteWindowRemaining());
		assertEquals(0, dead.state(0).allInWindowRemaining());
		assertNull(dead.state(0).flipTarget());
	}

	@Test
	void resetWipesEveryWindowAndTheFlipRecord() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertTrue(runtime.tryUseRush(0));
		assertTrue(runtime.tryUseRend(0, 4));
		assertTrue(runtime.tryUseRotate(0, RotateAbility.Stance.GLADIUS));
		assertTrue(runtime.tryUseAequilibrium(0));

		runtime.resetCooldowns();

		assertEquals(0, runtime.state(0).rushWindowRemaining());
		assertEquals(0, runtime.state(0).rendWindowRemaining());
		assertEquals(0, runtime.state(0).flipWindowRemaining());
		assertEquals(0, runtime.state(0).aequilibriumWindowRemaining());
		assertNull(runtime.state(0).flipTarget());
	}

	@Test
	void allInWindowIsFifteenSecondsAndResetWipesIt() {
		PlayerRuntime runtime = new PlayerRuntime();

		assertTrue(runtime.tryUseAllIn(0));
		assertTrue(runtime.state(14_999).allInWindowRemaining() > 0);
		assertFalse(runtime.state(15_000).allInWindowRemaining() > 0);

		runtime.resetCooldowns();

		assertFalse(runtime.state(0).allInWindowRemaining() > 0);
		assertEquals(0, runtime.state(0).allInRemaining());
	}

	@Test
	void resetWipesTheLiveRouletteOutcomeToo() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRoulette(0, RouletteAbility.Outcome.EDGE);

		runtime.resetCooldowns();

		assertNull(runtime.state(0).rouletteOutcome());
		assertEquals(0, runtime.state(0).rouletteRemaining());
	}
}
