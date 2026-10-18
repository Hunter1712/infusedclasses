package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Per-passive decay, the lane-keyed arms and the one-state read: the C1/C2 deepening. */
class PlayerRuntimeDeepeningTest {
	/** A second trigger of our own, so per-passive decay can be read off a passive the registry never carries. */
	private static final Trigger ON_LAND = new Trigger("on_land", "Land a swing", "no swing");

	private static ConditionalPassive momentum() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives().get(0);
	}

	private static ClassDefinition withPassives(ConditionalPassive... passives) {
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				List.of(passives), List.of(), 0, "", List.of(), List.of(), CritRule.universal());
	}

	@Test
	void decayedPassiveMovesWithoutTouchingIdleOne() {
		ConditionalPassive swift = new ConditionalPassive("Swift", ON_LAND, 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition def = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(def, Trigger.ON_HIT_DEALT, base, 1.0F);
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
		ConditionalPassive swift = new ConditionalPassive("Swift", ON_LAND, 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition both = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(both, Trigger.ON_HIT_DEALT, base, 1.0F);
		runtime.onTrigger(both, ON_LAND, base, 1.0F);
		assertEquals(1, runtime.stacks(momentum()));

		runtime.clear(withPassives(momentum()));

		assertEquals(0, runtime.stacks(momentum()));
		assertEquals(1, runtime.stacks(swift));
	}

	@Test
	void clearDropsOnlyThePricedPassive() {
		ConditionalPassive swift = new ConditionalPassive("Swift", ON_LAND, 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition both = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(both, Trigger.ON_HIT_DEALT, base, 1.0F);
		runtime.onTrigger(both, ON_LAND, base, 1.0F);

		runtime.clear(momentum());

		assertEquals(0, runtime.stacks(momentum()));
		assertEquals(1, runtime.stacks(swift));
	}

	@Test
	void freshStateIsTheEmptyRead() {
		PlayerRuntime.State state = new PlayerRuntime().state(0);

		for (Lane lane : Lane.values()) {
			assertEquals(0, state.laneRemaining(lane), lane.name());
			assertEquals(0, state.lane(lane).windowRemainingMs(), lane.name());
			assertFalse(state.lane(lane).windowPending(), lane.name());
		}
		assertEquals(0, state.rendBonusStacks());
		assertNull(state.flipTarget());
		assertFalse(state.impetusPending());
		assertNull(state.rouletteOutcome());
		assertEquals(RotateAbility.Stance.SCUTUM, state.stance());
		assertEquals(0, state.streak());
	}

	@Test
	void laneVerbsArmWithoutExposingMutation() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(0, runtime.state(0).laneRemaining(Lane.REND));
		assertTrue(runtime.arm(Lane.REND, 0, RendAbility.WINDOW_TICKS * 50L, 5));
		assertEquals(Lane.REND.cooldownMs(), runtime.state(0).laneRemaining(Lane.REND));
		runtime.resetAll();
		assertEquals(0, runtime.state(0).laneRemaining(Lane.REND));
	}

	/** A rejected arm is one decision: the cooling lane and its window are left untouched together. */
	@Test
	void rejectedArmLeavesTheWindowUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.arm(Lane.REND, 0, RendAbility.WINDOW_TICKS * 50L, 5);

		assertFalse(runtime.arm(Lane.REND, 1_000, RendAbility.WINDOW_TICKS * 50L, 9));
		assertEquals(5, runtime.state(1_000).rendBonusStacks());
		assertEquals(RendAbility.WINDOW_TICKS * 50L - 1_000,
				runtime.state(1_000).lane(Lane.REND).windowRemainingMs());
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
		runtime.resetAll();
		assertEquals(0, runtime.state(0).streak());
	}

	@Test
	void rouletteWindowRoundTripsItsPayloadAndExpires() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertNull(runtime.state(0).rouletteOutcome());

		assertTrue(runtime.roulette(1_000, RouletteAbility.Outcome.JACKPOT));

		assertEquals(RouletteAbility.Outcome.JACKPOT, runtime.state(1_000).rouletteOutcome());
		assertEquals(RouletteAbility.Outcome.JACKPOT, runtime.state(10_999).rouletteOutcome());
		assertNull(runtime.state(11_000).rouletteOutcome());
	}

	@Test
	void everyWindowReadsZeroWhenDeadAndItsExactBoundaryWhenLive() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000L;
		assertTrue(runtime.arm(Lane.RUSH, base, RushAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.arm(Lane.REND, base, RendAbility.WINDOW_TICKS * 50L, 4));
		assertTrue(runtime.rotate(base, RotateAbility.Stance.GLADIUS));
		assertTrue(runtime.arm(Lane.OATH, base, AequilibriumAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.roulette(base, RouletteAbility.Outcome.EDGE));
		assertTrue(runtime.arm(Lane.ALL_IN, base, AllInAbility.WINDOW_TICKS * 50L));

		assertEquals(5_000, runtime.state(base).lane(Lane.RUSH).windowRemainingMs());
		assertEquals(1, runtime.state(base + 4_999).lane(Lane.RUSH).windowRemainingMs());
		assertEquals(0, runtime.state(base + 5_000).lane(Lane.RUSH).windowRemainingMs());

		assertEquals(6_000, runtime.state(base).lane(Lane.REND).windowRemainingMs());
		assertEquals(1, runtime.state(base + 5_999).lane(Lane.REND).windowRemainingMs());
		assertEquals(0, runtime.state(base + 6_000).lane(Lane.REND).windowRemainingMs());

		assertEquals(5_000, runtime.state(base).lane(Lane.ROTATE).windowRemainingMs());
		assertEquals(RotateAbility.Stance.GLADIUS, runtime.state(base).flipTarget());
		assertEquals(1, runtime.state(base + 4_999).lane(Lane.ROTATE).windowRemainingMs());
		assertEquals(0, runtime.state(base + 5_000).lane(Lane.ROTATE).windowRemainingMs());
		assertNull(runtime.state(base + 5_000).flipTarget());

		assertEquals(6_000, runtime.state(base).lane(Lane.OATH).windowRemainingMs());
		assertEquals(1, runtime.state(base + 5_999).lane(Lane.OATH).windowRemainingMs());
		assertEquals(0, runtime.state(base + 6_000).lane(Lane.OATH).windowRemainingMs());

		assertEquals(10_000, runtime.state(base).lane(Lane.ROULETTE).windowRemainingMs());
		assertEquals(1, runtime.state(base + 9_999).lane(Lane.ROULETTE).windowRemainingMs());
		assertEquals(0, runtime.state(base + 10_000).lane(Lane.ROULETTE).windowRemainingMs());

		assertEquals(15_000, runtime.state(base).lane(Lane.ALL_IN).windowRemainingMs());
		assertEquals(1, runtime.state(base + 14_999).lane(Lane.ALL_IN).windowRemainingMs());
		assertEquals(0, runtime.state(base + 15_000).lane(Lane.ALL_IN).windowRemainingMs());

		PlayerRuntime dead = new PlayerRuntime();
		for (Lane lane : Lane.values()) {
			assertEquals(0, dead.state(0).lane(lane).windowRemainingMs(), lane.name());
		}
		assertNull(dead.state(0).flipTarget());
	}

	@Test
	void resetWipesEveryWindowAndTheFlipRecord() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertTrue(runtime.arm(Lane.RUSH, 0, RushAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.arm(Lane.REND, 0, RendAbility.WINDOW_TICKS * 50L, 4));
		assertTrue(runtime.rotate(0, RotateAbility.Stance.GLADIUS));
		assertTrue(runtime.arm(Lane.OATH, 0, AequilibriumAbility.WINDOW_TICKS * 50L));

		runtime.resetAll();

		for (Lane lane : Lane.values()) {
			assertEquals(0, runtime.state(0).lane(lane).windowRemainingMs(), lane.name());
			assertEquals(0, runtime.state(0).laneRemaining(lane), lane.name());
		}
		assertNull(runtime.state(0).flipTarget());
	}

	@Test
	void allInWindowIsFifteenSecondsAndResetWipesIt() {
		PlayerRuntime runtime = new PlayerRuntime();

		assertTrue(runtime.arm(Lane.ALL_IN, 0, AllInAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.state(14_999).lane(Lane.ALL_IN).windowRemainingMs() > 0);
		assertFalse(runtime.state(15_000).lane(Lane.ALL_IN).windowRemainingMs() > 0);

		runtime.resetAll();

		assertFalse(runtime.state(0).lane(Lane.ALL_IN).windowRemainingMs() > 0);
		assertEquals(0, runtime.state(0).laneRemaining(Lane.ALL_IN));
	}

	@Test
	void resetWipesTheLiveRouletteOutcomeToo() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.roulette(0, RouletteAbility.Outcome.EDGE);

		runtime.resetAll();

		assertNull(runtime.state(0).rouletteOutcome());
		assertEquals(0, runtime.state(0).laneRemaining(Lane.ROULETTE));
	}
}
