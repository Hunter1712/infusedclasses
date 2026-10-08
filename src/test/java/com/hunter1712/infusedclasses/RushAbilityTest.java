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
		assertEquals(80, RushAbility.WINDOW_TICKS);
		assertEquals(80, RushAbility.SPEED_TICKS);
		assertEquals(0, RushAbility.SPEED_AMPLIFIER);
		assertEquals(3, RushAbility.BONUS_STACKS);
		assertEquals("minecraft:speed", RushAbility.SPEED_EFFECT.toString());
		assertEquals("minecraft:entity.wind_charge.wind_burst", RushAbility.CAST_SOUND.toString());
		assertEquals("Whoosh!", RushAbility.CAST_WORD);
	}

	@Test
	void firesWithZeroStacksAtFullHp() {
		assertEquals(RushAbility.Kind.FIRED,
				RushAbility.trigger(juggernaut(), 0).kind());
	}

	@Test
	void fireStampsWindowAndArmsOnlyItsOwnLane() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.Result result = RushAbility.fire(juggernaut(), runtime, 0);

		assertEquals(RushAbility.Kind.FIRED, result.kind());
		assertEquals("Rush!", result.announcement().text());
		assertEquals(Cooldowns.RUSH_MS, runtime.rushRemaining(0));
		assertEquals(0, runtime.rendRemaining(0));
		assertTrue(runtime.isRushLive(0));
		assertTrue(runtime.isRushLive(3_999));
		assertFalse(runtime.isRushLive(4_000));
	}

	@Test
	void onCooldownReportsRemainingAndArmsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, 1_000);
		RushAbility.Result second = RushAbility.fire(juggernaut(), runtime, 11_000);

		assertEquals(RushAbility.Kind.ON_COOLDOWN, second.kind());
		assertEquals(15_000, second.remainingMs());
		assertEquals("Rush: 15s", second.announcement().text());
		assertEquals(15_000, runtime.rushRemaining(11_000));
		assertEquals(0, runtime.rendRemaining(11_000));
		assertFalse(runtime.isRushLive(11_000));
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.Result result = RushAbility.fire(null, runtime, 0);

		assertEquals(RushAbility.Kind.WRONG_CLASS, result.kind());
		assertNull(result.announcement());
		assertEquals(0, runtime.rushRemaining(0));
		assertFalse(runtime.isRushLive(0));
	}

	@Test
	void bonusConsumedExactlyOnce() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, 0);

		assertTrue(runtime.consumeRushBonus(0));
		assertFalse(runtime.consumeRushBonus(0));
		assertFalse(runtime.consumeRushBonus(5_000));
	}

	@Test
	void juggernautCarriesBothAbilityIdsInOrder() {
		assertEquals(
				java.util.List.of(ClassRegistry.RUSH_ABILITY, ClassRegistry.REND_ABILITY),
				juggernaut().activeAbilities());
		assertTrue(juggernaut().actives().stream().map(ClassDefinition.ActiveAbility::name).toList()
				.equals(java.util.List.of("Rush", "Rend")));
	}

	@Test
	void openerGatesOnItsOwnIdNotRends() {
		ClassDefinition rendOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				java.util.List.of(), java.util.List.of(), 0.0, java.util.List.of(ClassRegistry.REND_ABILITY), 0, "", java.util.List.of());

		assertEquals(RushAbility.Kind.WRONG_CLASS,
				RushAbility.trigger(rendOnly, 0).kind());
	}

	@Test
	void resetReadiesBothLanesAndClearsTheWindow() {
		PlayerRuntime runtime = new PlayerRuntime();
		RushAbility.fire(juggernaut(), runtime, 0);
		runtime.tryUseRend(0, 5);
		runtime.resetCooldowns();

		assertEquals(0, runtime.rushRemaining(0));
		assertEquals(0, runtime.rendRemaining(0));
		assertFalse(runtime.isRushLive(0));
		assertFalse(runtime.consumeRushBonus(0));
	}
}
