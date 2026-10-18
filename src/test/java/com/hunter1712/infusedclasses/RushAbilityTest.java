package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rush decisions: the engage, asserted without a player or level.
 * The adapters own the speed, the cleanse and the wind burst; this pins what they are told.
 */
class RushAbilityTest {
	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	@Test
	void windowAndBonusConstants() {
		assertEquals(100, RushAbility.WINDOW_TICKS);
		assertEquals(0.02, RushAbility.SPEED_BONUS);
		assertEquals("infusedclasses:rush_speed", RushAbility.SPEED_ID.toString());
		assertEquals(5, RushAbility.BONUS_STACKS);
		assertEquals("minecraft:entity.wind_charge.wind_burst", RushAbility.CAST_SOUND.toString());
	}

	@Test
	void firesWithZeroStacksAtFullHp() {
		assertTrue(RushAbility.trigger(juggernaut(), 0).fired());
	}

	@Test
	void fireStampsWindowAndArmsOnlyItsOwnLane() {
		PlayerRuntime runtime = new PlayerRuntime();
		AbilityOutcome result = RushAbility.fire(juggernaut(), runtime, 0);

		assertTrue(result.fired());
		assertEquals("Rush!", result.announcement().text());
		assertEquals(Lane.RUSH.cooldownMs(), runtime.state(0).laneRemaining(Lane.RUSH));
		assertEquals(0, runtime.state(0).laneRemaining(Lane.REND));
		assertTrue(runtime.state(0).lane(Lane.RUSH).windowPending());
		assertTrue(runtime.state(4_999).lane(Lane.RUSH).windowPending());
		assertFalse(runtime.state(5_000).lane(Lane.RUSH).windowPending());
	}

	@Test
	void aPressWhileOnCooldownIsSilentAndArmsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, 1_000);
		AbilityOutcome second = RushAbility.fire(juggernaut(), runtime, 11_000);

		assertFalse(second.fired());
		assertNull(second.announcement());
		assertEquals(15_000, runtime.state(11_000).laneRemaining(Lane.RUSH));
		assertEquals(0, runtime.state(11_000).laneRemaining(Lane.REND));
		assertFalse(runtime.state(11_000).lane(Lane.RUSH).windowPending());
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();
		AbilityOutcome result = RushAbility.fire(null, runtime, 0);

		assertFalse(result.fired());
		assertNull(result.announcement());
		assertEquals(0, runtime.state(0).laneRemaining(Lane.RUSH));
		assertFalse(runtime.state(0).lane(Lane.RUSH).windowPending());
	}

	@Test
	void bonusConsumedExactlyOnce() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, 0);

		assertTrue(runtime.consumeBonus(Lane.RUSH, 0));
		assertFalse(runtime.consumeBonus(Lane.RUSH, 0));
		assertFalse(runtime.consumeBonus(Lane.RUSH, 5_000));
	}

	@Test
	void juggernautCarriesBothAbilityIdsInOrder() {
		assertEquals(
				java.util.List.of(ClassRegistry.RUSH_ABILITY, ClassRegistry.REND_ABILITY),
				juggernaut().actives().stream().map(ClassDefinition.ActiveAbility::id).toList());
		assertTrue(juggernaut().actives().stream().map(ClassDefinition.ActiveAbility::name).toList()
				.equals(java.util.List.of("Rush", "Rend")));
	}

	@Test
	void openerGatesOnItsOwnIdNotRends() {
		ClassDefinition rendOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				java.util.List.of(), java.util.List.of(), 0, "",
				java.util.List.of(new ClassDefinition.ActiveAbility(ClassRegistry.REND_ABILITY, "Rend", "test copy")),
				java.util.List.of(),
				CritRule.universal());

		assertFalse(RushAbility.trigger(rendOnly, 0).fired());
	}

	@Test
	void resetReadiesBothLanesAndClearsTheWindow() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, 0);
		runtime.arm(Lane.REND, 0, RendAbility.WINDOW_TICKS * 50L, 5);
		runtime.resetAll();

		assertEquals(0, runtime.state(0).laneRemaining(Lane.RUSH));
		assertEquals(0, runtime.state(0).laneRemaining(Lane.REND));
		assertFalse(runtime.state(0).lane(Lane.RUSH).windowPending());
		assertFalse(runtime.consumeBonus(Lane.RUSH, 0));
	}
}
