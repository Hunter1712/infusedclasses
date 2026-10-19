package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offload as a decision: the flat price gates the press, a fired press arms its own lane
 * and window and pays, and the family's pair follows the window. No player is constructed.
 */
class OffloadAbilityTest {
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
	void fourStacksFizzleAndArmNothing() {
		PlayerRuntime runtime = loaded(4);

		AbilityOutcome result = OffloadAbility.fire(juggernaut(), runtime, 0);

		assertFalse(result.fired());
		assertEquals("No Momentum", result.announcement().text());
		assertEquals(4, runtime.stacks(momentum()));
		assertEquals(0, runtime.state(0).laneRemaining(Lane.OFFLOAD));
		assertEquals(0, runtime.state(0).lane(Lane.OFFLOAD).windowRemainingMs());
	}

	@Test
	void fiveStacksFireSpendFiveAndArmBothClocks() {
		PlayerRuntime runtime = loaded(5);

		AbilityOutcome result = OffloadAbility.fire(juggernaut(), runtime, 0);

		assertTrue(result.fired());
		assertEquals("Offload!", result.announcement().text());
		assertEquals(0, runtime.stacks(momentum()));
		assertEquals(Lane.OFFLOAD.cooldownMs(), runtime.state(0).laneRemaining(Lane.OFFLOAD));
		assertEquals(OffloadAbility.WINDOW_TICKS * 50L,
				runtime.state(0).lane(Lane.OFFLOAD).windowRemainingMs());
	}

	@Test
	void aSecondPressWhileCoolingIsRejectedSilentlyAndSpendsNothing() {
		PlayerRuntime runtime = loaded(10);
		assertTrue(OffloadAbility.fire(juggernaut(), runtime, 0).fired());

		AbilityOutcome second = OffloadAbility.fire(juggernaut(), runtime, 1_000);

		assertFalse(second.fired());
		assertNull(second.announcement());
		assertEquals(5, runtime.stacks(momentum()));
		assertEquals(Lane.OFFLOAD.cooldownMs() - 1_000,
				runtime.state(1_000).laneRemaining(Lane.OFFLOAD));
	}

	@Test
	void aWrongClassIsRejectedEvenWithAPrice() {
		PlayerRuntime runtime = loaded(10);

		AbilityOutcome result = OffloadAbility.fire(legionary(), runtime, 0);

		assertFalse(result.fired());
		assertNull(result.announcement());
		assertEquals(10, runtime.stacks(momentum()));
		assertEquals(0, runtime.state(0).laneRemaining(Lane.OFFLOAD));
	}

	@Test
	void theFamilyWantsItsPairOnlyWhileTheWindowIsLive() {
		PlayerRuntime runtime = loaded(5);
		assertEquals(List.of(), PassiveApplier.offloadWanted(runtime.state(0)));

		OffloadAbility.fire(juggernaut(), runtime, 0);

		assertEquals(OffloadAbility.windowModifiers(), PassiveApplier.offloadWanted(runtime.state(0)));
		assertEquals(OffloadAbility.windowModifiers(), PassiveApplier.offloadWanted(runtime.state(4_999)));
		assertEquals(List.of(), PassiveApplier.offloadWanted(runtime.state(5_000)));
	}

	@Test
	void theWindowWritesHasteAndSpeedUnderTheirOwnIds() {
		List<PassiveEffect.Modifier> pair = OffloadAbility.windowModifiers();

		assertEquals(List.of(ClassRegistry.ATTACK_SPEED, ClassRegistry.MOVEMENT_SPEED),
				pair.stream().map(PassiveEffect.Modifier::attribute).toList());
		assertEquals(List.of("infusedclasses:offload_haste", "infusedclasses:offload_speed"),
				pair.stream().map(PassiveEffect.Modifier::modifierId)
						.map(net.minecraft.resources.Identifier::toString).toList());
		assertEquals(List.of(0.2, 0.01), pair.stream().map(PassiveEffect.Modifier::amount).toList());
	}

	@Test
	void copySpellsThePriceThePairAndTheLane() {
		String copy = OffloadAbility.desc();

		assertTrue(copy.contains("Spend 5 Momentum"));
		assertTrue(copy.contains("+0.2 attack speed"));
		assertTrue(copy.contains("+0.01 speed"));
		assertTrue(copy.contains("(R, 10s)"));
	}
}
