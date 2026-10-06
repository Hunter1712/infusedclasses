package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * War Cry decisions: the debuff shout, asserted without a player, level, or effect.
 * The adapter owns the scan and the writes; this pins what it is told.
 */
class WarCryAbilityTest {
	private static ClassDefinition warrior() {
		return ClassRegistry.byId(ClassRegistry.WARRIOR);
	}

	@Test
	void shoutCarriesBothDebuffsForTheWindow() {
		assertEquals(
				List.of(WarCryAbility.WEAKNESS, WarCryAbility.SLOWNESS),
				WarCryAbility.DEBUFF_EFFECTS);
		assertEquals(160, WarCryAbility.EFFECT_TICKS);
		assertEquals(0, WarCryAbility.AMPLIFIER);
		assertEquals(16.0, WarCryAbility.RADIUS_BLOCKS);
	}

	@Test
	void fireShoutsAndArmsOnlyItsOwnLane() {
		PlayerRuntime runtime = new PlayerRuntime();
		WarCryAbility.Result result = WarCryAbility.fire(warrior(), runtime, 0);

		assertEquals(WarCryAbility.Kind.FIRED, result.kind());
		assertEquals("War Cry!", result.announcement().text());
		assertEquals(Cooldowns.WAR_CRY_MS, runtime.warCryRemaining(0));
		assertEquals(0, runtime.secondWindRemaining(0));
	}

	@Test
	void onCooldownReportsRemainingAndArmsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		WarCryAbility.fire(warrior(), runtime, 1_000);
		WarCryAbility.Result second = WarCryAbility.fire(warrior(), runtime, 16_000);

		assertEquals(WarCryAbility.Kind.ON_COOLDOWN, second.kind());
		assertEquals(45_000, second.remainingMs());
		assertEquals(45_000, runtime.warCryRemaining(16_000));
		assertEquals(0, runtime.secondWindRemaining(16_000));
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();
		WarCryAbility.Result result = WarCryAbility.fire(null, runtime, 0);

		assertEquals(WarCryAbility.Kind.WRONG_CLASS, result.kind());
		assertNull(result.announcement());
		assertEquals(0, runtime.warCryRemaining(0));
	}

	@Test
	void warriorCarriesBothAbilityIdsInOrder() {
		assertEquals(
				List.of(ClassRegistry.WAR_CRY_ABILITY, ClassRegistry.SECOND_WIND_ABILITY),
				warrior().activeAbilities());
		assertTrue(warrior().actives().stream().map(ClassDefinition.ActiveAbility::name).toList()
				.equals(List.of("War Cry", "Second Wind")));
	}

	@Test
	void shoutGatesOnItsOwnIdNotSecondWinds() {
		ClassDefinition secondWindOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(), List.of(), 0.0, List.of(ClassRegistry.SECOND_WIND_ABILITY), 0, "", List.of());

		assertEquals(WarCryAbility.Kind.WRONG_CLASS,
				WarCryAbility.trigger(secondWindOnly, new Cooldowns(), 0).kind());
	}
}
