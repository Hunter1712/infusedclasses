package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Per-passive bleed, the lane-keyed arms, the bar's spend/end verbs and the one-state read. */
class PlayerRuntimeDeepeningTest {
	/** A trigger of our own, so per-passive bleed can be read off a passive the registry never carries. */
	private static final Trigger ON_LAND = new Trigger("on_land", "Land a swing");

	private static ConditionalPassive momentum() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives().get(0);
	}

	private static ConditionalPassive swift() {
		return new ConditionalPassive("Swift", List.of(new ConditionalPassive.Earn(ON_LAND, 1)), 6,
				List.of(new ConditionalPassive.Tier(2, "Quick", 0.5, -0.001, 0.25)), 2);
	}

	private static ClassDefinition withPassives(ConditionalPassive... passives) {
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				List.of(passives), List.of(), 0, "", List.of(), List.of(), CritRule.universal());
	}

	@Test
	void decayBleedsEachFedPassiveOnItsOwnClock() {
		ConditionalPassive swift = swift();
		ClassDefinition def = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(def, Trigger.ON_HIT_DEALT, base, 1.0F);
		for (int i = 0; i < 5; i++) {
			runtime.onTrigger(def, ON_LAND, base, 1.0F);
		}

		runtime.decay(def, base + 3_000);

		assertEquals(1, runtime.stacks(momentum()), "Momentum's own 5s grace still holds");
		assertEquals(4, runtime.stacks(swift), "Swift's own 2s grace already bled one");

		runtime.decay(def, base + 6_000);

		assertEquals(0, runtime.stacks(momentum()));
		assertEquals(1, runtime.stacks(swift));
	}

	@Test
	void clearDropsOnlyThatClassStacks() {
		ConditionalPassive swift = swift();
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
	void freshStateIsTheEmptyRead() {
		PlayerRuntime.State state = new PlayerRuntime().state(0);

		for (Lane lane : Lane.values()) {
			assertEquals(0, state.laneRemaining(lane), lane.name());
			assertEquals(0, state.lane(lane).windowRemainingMs(), lane.name());
			assertFalse(state.lane(lane).windowPending(), lane.name());
		}
		assertNull(state.flipTarget());
		assertFalse(state.impetusPending());
		assertNull(state.rouletteOutcome());
		assertEquals(RotateAbility.Stance.SCUTUM, state.stance());
		assertEquals(0, state.streak());
	}

	@Test
	void laneVerbsArmWithoutExposingMutation() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(0, runtime.state(0).laneRemaining(Lane.OFFLOAD));
		assertTrue(runtime.arm(Lane.OFFLOAD, 0, OffloadAbility.WINDOW_TICKS * 50L));
		assertEquals(Lane.OFFLOAD.cooldownMs(), runtime.state(0).laneRemaining(Lane.OFFLOAD));
		runtime.resetAll();
		assertEquals(0, runtime.state(0).laneRemaining(Lane.OFFLOAD));
	}

	/** A rejected arm is one decision: the cooling lane and its window are left untouched together. */
	@Test
	void rejectedArmLeavesTheWindowUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.arm(Lane.OFFLOAD, 0, OffloadAbility.WINDOW_TICKS * 50L);

		assertFalse(runtime.arm(Lane.OFFLOAD, 1_000, OffloadAbility.WINDOW_TICKS * 50L));
		assertEquals(OffloadAbility.WINDOW_TICKS * 50L - 1_000,
				runtime.state(1_000).lane(Lane.OFFLOAD).windowRemainingMs());
	}

	@Test
	void spendStacksMovesTheBarAndReportsTheSpend() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive momentum = momentum();
		for (int i = 0; i < 3; i++) {
			runtime.onTrigger(ClassRegistry.byId(ClassRegistry.JUGGERNAUT),
					Trigger.ON_HIT_DEALT, 0, 1.0F);
		}

		assertEquals(2, runtime.spendStacks(momentum, 2));
		assertEquals(1, runtime.stacks(momentum));
		assertEquals(1, runtime.spendStacks(momentum, 5));
		assertEquals(0, runtime.stacks(momentum));
		assertEquals(0, runtime.spendStacks(momentum, 5));
	}

	@Test
	void endWindowDropsTheWindowAndLeavesTheLaneCooling() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertTrue(runtime.arm(Lane.ONSLAUGHT, 0, OnslaughtAbility.WINDOW_TICKS * 50L));

		runtime.endWindow(Lane.ONSLAUGHT);

		assertEquals(0, runtime.state(0).lane(Lane.ONSLAUGHT).windowRemainingMs());
		assertEquals(Lane.ONSLAUGHT.cooldownMs(), runtime.state(0).laneRemaining(Lane.ONSLAUGHT));
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
		assertTrue(runtime.arm(Lane.OFFLOAD, base, OffloadAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.arm(Lane.ONSLAUGHT, base, OnslaughtAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.rotate(base, RotateAbility.Stance.GLADIUS));
		assertTrue(runtime.arm(Lane.OATH, base, AequilibriumAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.roulette(base, RouletteAbility.Outcome.EDGE));
		assertTrue(runtime.arm(Lane.ALL_IN, base, AllInAbility.WINDOW_TICKS * 50L));

		assertEquals(5_000, runtime.state(base).lane(Lane.OFFLOAD).windowRemainingMs());
		assertEquals(1, runtime.state(base + 4_999).lane(Lane.OFFLOAD).windowRemainingMs());
		assertEquals(0, runtime.state(base + 5_000).lane(Lane.OFFLOAD).windowRemainingMs());

		assertEquals(15_000, runtime.state(base).lane(Lane.ONSLAUGHT).windowRemainingMs());
		assertEquals(1, runtime.state(base + 14_999).lane(Lane.ONSLAUGHT).windowRemainingMs());
		assertEquals(0, runtime.state(base + 15_000).lane(Lane.ONSLAUGHT).windowRemainingMs());

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
		assertTrue(runtime.arm(Lane.OFFLOAD, 0, OffloadAbility.WINDOW_TICKS * 50L));
		assertTrue(runtime.arm(Lane.ONSLAUGHT, 0, OnslaughtAbility.WINDOW_TICKS * 50L));
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
