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
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(passives), List.of(), 0.0, List.of(), 0, "", List.of());
	}

	@Test
	void decayedPassiveMovesWithoutTouchingIdleOne() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ClassDefinition def = withPassives(momentum(), swift);
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(def, "on_hit_dealt", base);
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
		runtime.onTrigger(both, "on_hit_dealt", base);
		runtime.onTrigger(both, "on_land", base);
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
		runtime.onTrigger(both, "on_hit_dealt", base);
		runtime.onTrigger(both, "on_land", base);

		runtime.clear(momentum());

		assertEquals(0, runtime.stacks(momentum()));
		assertNull(runtime.lastTrigger(momentum()));
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
}
