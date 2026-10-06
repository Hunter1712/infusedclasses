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
		assertEquals(200, WarCryAbility.EFFECT_TICKS);
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
		assertEquals(5_000, second.remainingMs());
		assertEquals("War Cry: 5s", second.announcement().text());
		assertEquals(5_000, runtime.warCryRemaining(16_000));
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

	@Test
	void builderGrantCreditsFiveAndRestampsGrace() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = warrior().passives().get(0);

		WarCryAbility.Result result = WarCryAbility.fire(warrior(), runtime, 1_000_000L);
		PassiveApplier.grantWarCryStacks(runtime, warrior(), result, 1_000_000L);

		assertEquals(5, runtime.stacks(bulwark));
		assertTrue(runtime.tickDecayMoved(warrior(), 1_000_000L + 4_000).isEmpty());
		assertEquals(5, runtime.stacks(bulwark));
	}

	@Test
	void builderGrantCapsAtTwenty() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = warrior().passives().get(0);
		for (int i = 0; i < 18; i++) {
			runtime.onTrigger(warrior(), ConditionalPassive.ON_HIT_TAKEN, 0);
		}

		WarCryAbility.Result result = WarCryAbility.fire(warrior(), runtime, 0);
		PassiveApplier.grantWarCryStacks(runtime, warrior(), result, 0);

		assertEquals(20, runtime.stacks(bulwark));
	}

	@Test
	void builderGrantWithoutPricedPassiveIsInert() {
		ClassDefinition noBulwark = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(), List.of(), 0.0, List.of(ClassRegistry.WAR_CRY_ABILITY), 0, "", List.of());

		PassiveApplier.grantWarCryStacks(new PlayerRuntime(), noBulwark, WarCryAbility.Result.fired(), 0);
		PassiveApplier.grantWarCryStacks(new PlayerRuntime(), null, WarCryAbility.Result.fired(), 0);
	}

	@Test
	void builderGrantOnCooldownGrantsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = warrior().passives().get(0);
		WarCryAbility.fire(warrior(), runtime, 1_000);
		WarCryAbility.Result rejected = WarCryAbility.fire(warrior(), runtime, 16_000);

		PassiveApplier.grantWarCryStacks(runtime, warrior(), rejected, 16_000);

		assertEquals(0, runtime.stacks(bulwark));
	}

	@Test
	void builderGrantWrongClassGrantsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = warrior().passives().get(0);
		WarCryAbility.Result rejected = WarCryAbility.fire(null, runtime, 0);

		PassiveApplier.grantWarCryStacks(runtime, warrior(), rejected, 0);

		assertEquals(0, runtime.stacks(bulwark));
	}
}
