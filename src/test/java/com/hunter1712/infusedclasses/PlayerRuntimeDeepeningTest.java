package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Per-passive decay and switch cleanup: the C2 deepening. */
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
	void cooldownVerbsArmWithoutExposingMutation() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(0, runtime.rendRemaining(0));
		assertTrue(runtime.tryUseRend(0, 5));
		assertEquals(Cooldowns.REND_MS, runtime.rendRemaining(0));
		runtime.resetCooldowns();
		assertEquals(0, runtime.rendRemaining(0));
	}

	@Test
	void streakBumpsResetsAndWipesWithTheClass() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(0, runtime.streak());

		runtime.bumpStreak();
		runtime.bumpStreak();
		assertEquals(2, runtime.streak());

		runtime.resetStreak();
		assertEquals(0, runtime.streak());

		runtime.bumpStreak();
		runtime.resetCooldowns();
		assertEquals(0, runtime.streak());
	}

	@Test
	void rouletteWindowRoundTripsItsPayloadAndExpires() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertNull(runtime.rouletteOutcome(0));

		assertTrue(runtime.tryUseRoulette(1_000, RouletteAbility.Outcome.JACKPOT));

		assertEquals(RouletteAbility.Outcome.JACKPOT, runtime.rouletteOutcome(1_000));
		assertEquals(RouletteAbility.Outcome.JACKPOT, runtime.rouletteOutcome(10_999));
		assertNull(runtime.rouletteOutcome(11_000));
	}

	@Test
	void allInWindowIsFifteenSecondsAndResetWipesIt() {
		PlayerRuntime runtime = new PlayerRuntime();

		assertTrue(runtime.tryUseAllIn(0));
		assertTrue(runtime.isAllInLive(14_999));
		assertFalse(runtime.isAllInLive(15_000));

		runtime.resetCooldowns();

		assertFalse(runtime.isAllInLive(0));
		assertEquals(0, runtime.allInRemaining(0));
	}

	@Test
	void resetWipesTheLiveRouletteOutcomeToo() {
		PlayerRuntime runtime = new PlayerRuntime();
		runtime.tryUseRoulette(0, RouletteAbility.Outcome.EDGE);

		runtime.resetCooldowns();

		assertNull(runtime.rouletteOutcome(0));
		assertEquals(0, runtime.rouletteRemaining(0));
	}

	@Test
	void windowRemainingVerbsReadExactValuesAndZeroWhenDead() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		assertEquals(0, runtime.rushWindowRemaining(base));
		assertEquals(0, runtime.rendWindowRemaining(base));
		assertEquals(0, runtime.flipWindowRemaining(base));
		assertEquals(0, runtime.rouletteWindowRemaining(base));
		assertEquals(0, runtime.aequilibriumWindowRemaining(base));
		assertEquals(0, runtime.allInWindowRemaining(base));

		runtime.tryUseRush(base);
		runtime.tryUseRend(base, 0);
		runtime.tryUseRotate(base);
		runtime.flipStance(base, RotateAbility.Stance.GLADIUS);
		runtime.tryUseRoulette(base, RouletteAbility.Outcome.EDGE);
		runtime.tryUseAequilibrium(base);
		runtime.tryUseAllIn(base);

		assertEquals(RushAbility.WINDOW_TICKS * 50L, runtime.rushWindowRemaining(base));
		assertEquals(1, runtime.rushWindowRemaining(base + 4_999));
		assertEquals(0, runtime.rushWindowRemaining(base + 5_000));
		assertEquals(RendAbility.WINDOW_TICKS * 50L, runtime.rendWindowRemaining(base));
		assertEquals(1, runtime.rendWindowRemaining(base + 5_999));
		assertEquals(0, runtime.rendWindowRemaining(base + 6_000));
		assertEquals(RotateAbility.SWITCH_DEAL_MS, runtime.flipWindowRemaining(base));
		assertEquals(1, runtime.flipWindowRemaining(base + 4_999));
		assertEquals(0, runtime.flipWindowRemaining(base + 5_000));
		assertEquals(RouletteAbility.WINDOW_TICKS * 50L, runtime.rouletteWindowRemaining(base));
		assertEquals(0, runtime.rouletteWindowRemaining(base + 10_000));
		assertEquals(AequilibriumAbility.WINDOW_TICKS * 50L, runtime.aequilibriumWindowRemaining(base));
		assertEquals(0, runtime.aequilibriumWindowRemaining(base + 6_000));
		assertEquals(AllInAbility.WINDOW_TICKS * 50L, runtime.allInWindowRemaining(base));
		assertEquals(0, runtime.allInWindowRemaining(base + 15_000));

		runtime.resetCooldowns();

		assertEquals(0, runtime.rushWindowRemaining(base));
		assertEquals(0, runtime.rendWindowRemaining(base));
		assertEquals(0, runtime.flipWindowRemaining(base));
		assertEquals(0, runtime.rouletteWindowRemaining(base));
		assertEquals(0, runtime.aequilibriumWindowRemaining(base));
		assertEquals(0, runtime.allInWindowRemaining(base));
	}

	@Test
	void graceRemainingCountsDownAndReadsZeroWithoutStacks() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		assertEquals(0, runtime.graceRemaining(momentum(), base));

		runtime.bumpPassive(momentum(), 1, base);

		assertEquals(momentum().graceSeconds() * 1000L, runtime.graceRemaining(momentum(), base));
		assertEquals(1, runtime.graceRemaining(momentum(), base + 4_999));
		assertEquals(0, runtime.graceRemaining(momentum(), base + 5_000));

		runtime.clear(momentum());
		assertEquals(0, runtime.graceRemaining(momentum(), base));
	}
}
