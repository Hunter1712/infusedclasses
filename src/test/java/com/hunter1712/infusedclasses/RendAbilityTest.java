package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RendAbilityTest {
	/** A second trigger of our own, so the scan can be read off a passive the registry never carries. */
	private static final Trigger ON_LAND = new Trigger("on_land", "Land a swing", "no swing");

	private static PlayerClasses juggernaut() {
		PlayerClasses classes = new PlayerClasses();
		classes.readId("infusedclasses:juggernaut");
		return classes;
	}

	private static ClassDefinition juggernautDef() {
		return juggernaut().currentDefinition();
	}

	private static PlayerRuntime withStacks(int stacks, long now) {
		PlayerRuntime runtime = new PlayerRuntime();
		for (int i = 0; i < stacks; i++) {
			runtime.onTrigger(juggernautDef(), Trigger.ON_HIT_DEALT, now, 1.0F);
		}
		return runtime;
	}

	@Test
	void bonusMathAtZeroOneTwenty() {
		assertEquals(0, RendAbility.bonusFor(0));
		assertEquals(0.5F, RendAbility.bonusFor(1));
		assertEquals(5.0F, RendAbility.bonusFor(10));
		assertEquals(10.0F, RendAbility.bonusFor(20));
		assertEquals("minecraft:block.anvil.land", RendAbility.CAST_SOUND.toString());
	}

	@Test
	void noExecuteBonusAtAnyTargetHp() {
		// The bonus stands alone: target health never enters the decision.
		assertEquals(5.0F, RendAbility.bonusFor(10));
	}

	@Test
	void spendsTheHitDealtPassiveWhateverItsPosition() {
		ConditionalPassive swift = new ConditionalPassive("Swift", ON_LAND, 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ConditionalPassive momentum = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives().get(0);
		ClassDefinition shuffled = new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				List.of(swift, momentum), List.of(), 0, "", List.of(), List.of(),
				CritRule.universal());

		assertEquals(momentum, shuffled.passiveForTrigger(Trigger.ON_HIT_DEALT));
		assertNull(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "empty"), "Empty",
				List.of(), List.of(), 0, "", List.of(), List.of(),
				CritRule.universal())
				.passiveForTrigger(Trigger.ON_HIT_DEALT));
	}

	@Test
	void fireArmsOnlyItsOwnLane() {
		PlayerRuntime runtime = withStacks(5, 0);
		RendAbility.Result result = RendAbility.fire(juggernautDef(), runtime, 0);

		assertTrue(result.outcome().fired());
		assertTrue(result.consume());
		assertEquals(Lane.REND.cooldownMs(), runtime.state(0).laneRemaining(Lane.REND));
		assertEquals(120, RendAbility.WINDOW_TICKS);
	}

	@Test
	void fireStampsASingleUseBonus() {
		PlayerRuntime runtime = withStacks(10, 0);
		RendAbility.fire(juggernautDef(), runtime, 0);

		assertEquals(10, runtime.state(0).rendBonusStacks());
		assertEquals(10, runtime.state(5_999).rendBonusStacks());
		assertEquals(0, runtime.state(6_000).rendBonusStacks());
		assertEquals(10, runtime.consumePayload(Lane.REND, 0));
		assertEquals(0, runtime.consumePayload(Lane.REND, 0));
	}

	@Test
	void unswungWindowExpiresWithoutRefunding() {
		PlayerRuntime runtime = withStacks(10, 0);
		RendAbility.fire(juggernautDef(), runtime, 0);

		assertEquals(0, runtime.state(6_001).rendBonusStacks());
		assertEquals(0, runtime.consumePayload(Lane.REND, 6_001));
		// The lane stays armed: the whiff cost time, and the stacks are already spent.
		assertEquals(Lane.REND.cooldownMs() - 6_001, runtime.state(6_001).laneRemaining(Lane.REND));
	}

	@Test
	void fizzleBurnsNothing() {
		PlayerRuntime runtime = withStacks(0, 0);
		RendAbility.Result result = RendAbility.fire(juggernautDef(), runtime, 0);

		assertFalse(result.outcome().fired());
		assertEquals("No Momentum", result.outcome().announcement().text());
		assertFalse(result.consume());
		assertEquals(0, runtime.state(0).laneRemaining(Lane.REND));
		assertEquals(0, runtime.state(0).rendBonusStacks());
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = withStacks(5, 0);
		RendAbility.Result result = RendAbility.fire(null, runtime, 0);

		assertFalse(result.outcome().fired());
		assertNull(result.outcome().announcement());
		assertEquals(0, runtime.state(0).laneRemaining(Lane.REND));
		assertEquals(0, runtime.state(0).rendBonusStacks());
	}

	@Test
	void cooldownIndependentAndNeverRearmedByRejects() {
		PlayerRuntime runtime = withStacks(5, 0);
		assertEquals(0, runtime.state(0).laneRemaining(Lane.REND));

		RendAbility.fire(juggernautDef(), runtime, 1_000);
		RendAbility.Result second = RendAbility.fire(juggernautDef(), runtime, 16_000);

		assertFalse(second.outcome().fired());
		assertNull(second.outcome().announcement());
		assertEquals(45_000, runtime.state(16_000).laneRemaining(Lane.REND));
	}

	@Test
	void resetReadiesTheLaneAndClearsTheBonus() {
		PlayerRuntime runtime = withStacks(5, 0);
		RendAbility.fire(juggernautDef(), runtime, 0);
		runtime.resetAll();

		assertEquals(0, runtime.state(0).laneRemaining(Lane.REND));
		assertEquals(0, runtime.state(0).rendBonusStacks());
		assertEquals(0, runtime.consumePayload(Lane.REND, 0));
	}

	@Test
	void fireThenDropSpendsWhatFirePriced() {
		PlayerRuntime runtime = withStacks(4, 0);
		RendAbility.Result result = RendAbility.fire(juggernautDef(), runtime, 0);

		assertTrue(result.outcome().fired());
		assertTrue(result.consume());
		// The entry honors the flag through consumeSpent; its pure half is pinned
		// here: stacks drop once, a second drop finds nothing, and the armed
		// snapshot survives fire-time (hit-time spend is a separate event).
		assertTrue(PassiveApplier.dropStacks(runtime, juggernautDef().passiveForTrigger(Trigger.ON_HIT_DEALT)));
		assertEquals(0, runtime.stacks(juggernautDef().passives().get(0)));
		assertFalse(PassiveApplier.dropStacks(runtime, juggernautDef().passiveForTrigger(Trigger.ON_HIT_DEALT)));
		assertEquals(4, runtime.state(0).rendBonusStacks());
	}
}
