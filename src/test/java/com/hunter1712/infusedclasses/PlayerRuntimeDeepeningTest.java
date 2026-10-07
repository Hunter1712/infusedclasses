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
	private static ConditionalPassive bulwark() {
		return ClassRegistry.byId(ClassRegistry.BERSERKER).passives().get(0);
	}

	private static ClassDefinition withPassives(ConditionalPassive... passives) {
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(passives), List.of(), 0.0, List.of(), 0, "", List.of());
	}

	@Test
	void decayedPassiveMovesWithoutTouchingIdleOne() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition def = withPassives(bulwark(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(def, "on_hit_taken", base);
		// Swift never triggered: 0 stacks, never moved.

		Set<ConditionalPassive> moved = runtime.tickDecayMoved(def, base + 6_000);

		assertTrue(moved.contains(bulwark()));
		assertFalse(moved.contains(swift));
		// Old global flag would have removed Swift spuriously; per-passive must not.
		assertTrue(PassiveEffect.planFor(swift, runtime.stacks(swift), moved.contains(swift))
				instanceof PassiveEffect.Plan.Skip);
	}

	@Test
	void clearDropsOnlyThatClassStacks() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition both = withPassives(bulwark(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(both, "on_hit_taken", base);
		runtime.onTrigger(both, "on_land", base);
		assertEquals(1, runtime.stacks(bulwark()));

		runtime.clear(withPassives(bulwark()));

		assertEquals(0, runtime.stacks(bulwark()));
		assertEquals(1, runtime.stacks(swift));
	}

	@Test
	void clearDropsOnlyThePricedPassive() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition both = withPassives(bulwark(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(both, "on_hit_taken", base);
		runtime.onTrigger(both, "on_land", base);

		runtime.clear(bulwark());

		assertEquals(0, runtime.stacks(bulwark()));
		assertNull(runtime.lastTrigger(bulwark()));
		assertEquals(1, runtime.stacks(swift));
	}

	@Test
	void cooldownVerbsArmWithoutExposingMutation() {
		PlayerRuntime runtime = new PlayerRuntime();
		assertEquals(0, runtime.secondWindRemaining(0));
		assertTrue(runtime.tryUseSecondWind(0));
		assertEquals(Cooldowns.SECOND_WIND_MS, runtime.secondWindRemaining(0));
		runtime.resetCooldowns();
		assertEquals(0, runtime.secondWindRemaining(0));
	}
}
