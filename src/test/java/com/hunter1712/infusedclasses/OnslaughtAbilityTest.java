package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Onslaught as a decision: the 10-stack gate, the 60s lane and 15s window, and the early
 * end at an empty bar. The per-hit spend and the no-gain suppression are asserted through
 * {@link MeleePlan} in its own suite.
 */
class OnslaughtAbilityTest {
	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	private static ConditionalPassive momentum() {
		return juggernaut().passiveForTrigger(Trigger.ON_HIT_DEALT);
	}

	private static PlayerRuntime loaded(int stacks) {
		PlayerRuntime runtime = new PlayerRuntime();
		for (int i = 0; i < stacks; i++) {
			runtime.onTrigger(juggernaut(), Trigger.ON_HIT_DEALT, 0, 1.0F);
		}
		return runtime;
	}

	@Test
	void nineStacksFizzleAndArmNothing() {
		PlayerRuntime runtime = loaded(9);

		AbilityOutcome result = OnslaughtAbility.fire(juggernaut(), runtime, 0);

		assertFalse(result.fired());
		assertEquals("No Momentum", result.announcement().text());
		assertEquals(9, runtime.stacks(momentum()));
		assertEquals(0, runtime.state(0).laneRemaining(Lane.ONSLAUGHT));
		assertEquals(0, runtime.state(0).lane(Lane.ONSLAUGHT).windowRemainingMs());
	}

	@Test
	void tenStacksFireThe60sLaneAnd15sWindowWithoutSpendingOnCast() {
		PlayerRuntime runtime = loaded(10);

		AbilityOutcome result = OnslaughtAbility.fire(juggernaut(), runtime, 0);

		assertTrue(result.fired());
		assertEquals("Onslaught!", result.announcement().text());
		assertEquals(10, runtime.stacks(momentum()), "the cast arms; each hit pays");
		assertEquals(Lane.ONSLAUGHT.cooldownMs(), runtime.state(0).laneRemaining(Lane.ONSLAUGHT));
		assertEquals(OnslaughtAbility.WINDOW_TICKS * 50L,
				runtime.state(0).lane(Lane.ONSLAUGHT).windowRemainingMs());
	}

	@Test
	void aSecondPressWhileCoolingIsRejectedSilently() {
		PlayerRuntime runtime = loaded(10);
		assertTrue(OnslaughtAbility.fire(juggernaut(), runtime, 0).fired());

		AbilityOutcome second = OnslaughtAbility.fire(juggernaut(), runtime, 1_000);

		assertFalse(second.fired());
		assertNull(second.announcement());
		assertEquals(10, runtime.stacks(momentum()));
		assertEquals(OnslaughtAbility.WINDOW_TICKS * 50L - 1_000,
				runtime.state(1_000).lane(Lane.ONSLAUGHT).windowRemainingMs());
	}

	@Test
	void aWrongClassIsRejectedEvenWithAPrice() {
		PlayerRuntime runtime = loaded(10);

		AbilityOutcome result = OnslaughtAbility.fire(legionary(), runtime, 0);

		assertFalse(result.fired());
		assertNull(result.announcement());
		assertEquals(0, runtime.state(0).laneRemaining(Lane.ONSLAUGHT));
	}

	@Test
	void theWindowPricesOnlyWhileItIsLiveAndTheBarHasAStack() {
		PlayerRuntime runtime = loaded(10);
		assertFalse(OnslaughtAbility.windowLive(runtime.state(0)));

		OnslaughtAbility.fire(juggernaut(), runtime, 0);

		assertTrue(OnslaughtAbility.windowLive(runtime.state(0)));
		assertTrue(OnslaughtAbility.liveFor(runtime.state(0), 1));
		assertFalse(OnslaughtAbility.liveFor(runtime.state(0), 0));
		assertFalse(OnslaughtAbility.liveFor(runtime.state(15_000), 10));
	}

	@Test
	void anEmptyBarEndsTheWindowButLeavesTheLaneCooling() {
		PlayerRuntime runtime = loaded(10);
		OnslaughtAbility.fire(juggernaut(), runtime, 0);
		runtime.spendStacks(momentum(), 10);

		OnslaughtAbility.endIfDrained(juggernaut(), runtime, runtime.state(0));

		assertEquals(0, runtime.state(0).lane(Lane.ONSLAUGHT).windowRemainingMs());
		assertEquals(Lane.ONSLAUGHT.cooldownMs(), runtime.state(0).laneRemaining(Lane.ONSLAUGHT));
	}

	@Test
	void aBarWithStacksKeepsTheWindow() {
		PlayerRuntime runtime = loaded(10);
		OnslaughtAbility.fire(juggernaut(), runtime, 0);
		runtime.spendStacks(momentum(), OnslaughtAbility.COST_PER_HIT);

		OnslaughtAbility.endIfDrained(juggernaut(), runtime, runtime.state(0));

		assertEquals(OnslaughtAbility.WINDOW_TICKS * 50L,
				runtime.state(0).lane(Lane.ONSLAUGHT).windowRemainingMs());
	}

	@Test
	void copySpellsThePriceTheWindowAndTheLane() {
		String copy = OnslaughtAbility.desc();

		assertTrue(copy.contains("Needs 10 Momentum"));
		assertTrue(copy.contains("+1 per full hit"));
		assertTrue(copy.contains("spending 2 each"));
		assertTrue(copy.contains("or until the bar empties"));
		assertTrue(copy.contains("(V, 60s)"));
	}
}
