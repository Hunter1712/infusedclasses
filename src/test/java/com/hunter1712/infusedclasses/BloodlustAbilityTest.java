package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bloodlust decisions: the lifesteal opener, asserted without a player or level.
 * The adapters own the root, the roar and the heals; this pins what they are told.
 */
class BloodlustAbilityTest {
	private static ClassDefinition berserker() {
		return ClassRegistry.byId(ClassRegistry.BERSERKER);
	}

	@Test
	void lifestealRateAndCap() {
		assertEquals(200, BloodlustAbility.WINDOW_TICKS);
		assertEquals(0.02, BloodlustAbility.LIFESTEAL_PER_BUCKET);
		assertEquals(2.0F, BloodlustAbility.MAX_PER_HIT_HP);
		assertEquals(5, BloodlustAbility.GATE_BUCKET);
	}

	@Test
	void lifestealAtBucketsZeroFiveTen() {
		assertEquals(0, BloodlustAbility.lifestealFor(0, 7), 1e-6);
		assertEquals(0, BloodlustAbility.lifestealFor(0, 30), 1e-6);
		assertEquals(0.7F, BloodlustAbility.lifestealFor(5, 7), 1e-6);
		assertEquals(1.4F, BloodlustAbility.lifestealFor(10, 7), 1e-6);
		assertEquals(2.0F, BloodlustAbility.lifestealFor(5, 30), 1e-6);
		assertEquals(2.0F, BloodlustAbility.lifestealFor(10, 30), 1e-6);
	}

	@Test
	void gateBlocksBucketFourAndFiresBucketFive() {
		assertEquals(BloodlustAbility.Kind.TOO_HEALTHY,
				BloodlustAbility.trigger(berserker(), new Cooldowns(), 4, 0).kind());
		assertEquals(BloodlustAbility.Kind.FIRED,
				BloodlustAbility.trigger(berserker(), new Cooldowns(), 5, 0).kind());
	}

	@Test
	void tooHealthyFizzleSaysItsLine() {
		BloodlustAbility.Result result = BloodlustAbility.trigger(berserker(), new Cooldowns(), 0, 0);

		assertEquals(BloodlustAbility.Kind.TOO_HEALTHY, result.kind());
		assertEquals("Too healthy", result.announcement().text());
	}

	@Test
	void fireOpensWindowAndArmsOnlyItsOwnLane() {
		PlayerRuntime runtime = new PlayerRuntime();
		BloodlustAbility.Result result = BloodlustAbility.fire(berserker(), runtime, 5, 0);

		assertEquals(BloodlustAbility.Kind.FIRED, result.kind());
		assertEquals("Bloodlust!", result.announcement().text());
		assertEquals(Cooldowns.BLOODLUST_MS, runtime.bloodlustRemaining(0));
		assertEquals(0, runtime.secondWindRemaining(0));
		assertTrue(runtime.isBloodlustLive(0));
		assertTrue(runtime.isBloodlustLive(9_999));
		assertFalse(runtime.isBloodlustLive(10_000));
	}

	@Test
	void onCooldownReportsRemainingAndArmsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		BloodlustAbility.fire(berserker(), runtime, 5, 1_000);
		BloodlustAbility.Result second = BloodlustAbility.fire(berserker(), runtime, 5, 16_000);

		assertEquals(BloodlustAbility.Kind.ON_COOLDOWN, second.kind());
		assertEquals(15_000, second.remainingMs());
		assertEquals("Bloodlust: 15s", second.announcement().text());
		assertEquals(15_000, runtime.bloodlustRemaining(16_000));
		assertEquals(0, runtime.secondWindRemaining(16_000));
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();
		BloodlustAbility.Result result = BloodlustAbility.fire(null, runtime, 10, 0);

		assertEquals(BloodlustAbility.Kind.WRONG_CLASS, result.kind());
		assertNull(result.announcement());
		assertEquals(0, runtime.bloodlustRemaining(0));
		assertFalse(runtime.isBloodlustLive(0));
	}

	@Test
	void tooHealthyArmsNeitherLaneNorWindow() {
		PlayerRuntime runtime = new PlayerRuntime();
		BloodlustAbility.Result result = BloodlustAbility.fire(berserker(), runtime, 4, 0);

		assertEquals(BloodlustAbility.Kind.TOO_HEALTHY, result.kind());
		assertEquals(0, runtime.bloodlustRemaining(0));
		assertEquals(0, runtime.secondWindRemaining(0));
		assertFalse(runtime.isBloodlustLive(0));
	}

	@Test
	void berserkerCarriesBothAbilityIdsInOrder() {
		assertEquals(
				List.of(ClassRegistry.BLOODLUST_ABILITY, ClassRegistry.SECOND_WIND_ABILITY),
				berserker().activeAbilities());
		assertTrue(berserker().actives().stream().map(ClassDefinition.ActiveAbility::name).toList()
				.equals(List.of("Bloodlust", "Second Wind")));
	}

	@Test
	void openerGatesOnItsOwnIdNotSecondWinds() {
		ClassDefinition secondWindOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(), List.of(), 0.0, List.of(ClassRegistry.SECOND_WIND_ABILITY), 0, "", List.of());

		assertEquals(BloodlustAbility.Kind.WRONG_CLASS,
				BloodlustAbility.trigger(secondWindOnly, new Cooldowns(), 10, 0).kind());
	}

	@Test
	void builderGrantCreditsTheFiredBucketAndRestampsGrace() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = berserker().passives().get(0);

		BloodlustAbility.Result result = BloodlustAbility.fire(berserker(), runtime, 5, 1_000_000L);
		PassiveApplier.grantBloodlustStacks(runtime, berserker(), result, 1_000_000L);

		assertEquals(5, runtime.stacks(bulwark));
		assertTrue(runtime.tickDecayMoved(berserker(), 1_000_000L + 4_000).isEmpty());
		assertEquals(5, runtime.stacks(bulwark));
	}

	@Test
	void builderGrantAtBucketZeroCreditsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = berserker().passives().get(0);

		PassiveApplier.grantBloodlustStacks(runtime, berserker(), BloodlustAbility.Result.fired(0), 0);

		assertEquals(0, runtime.stacks(bulwark));
	}

	@Test
	void builderGrantAtFullBucketCreditsTen() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = berserker().passives().get(0);

		BloodlustAbility.Result result = BloodlustAbility.fire(berserker(), runtime, 10, 0);
		PassiveApplier.grantBloodlustStacks(runtime, berserker(), result, 0);

		assertEquals(10, runtime.stacks(bulwark));
	}

	@Test
	void builderGrantCapsAtTwenty() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = berserker().passives().get(0);
		for (int i = 0; i < 18; i++) {
			runtime.onTrigger(berserker(), ConditionalPassive.ON_HIT_TAKEN, 0);
		}

		BloodlustAbility.Result result = BloodlustAbility.fire(berserker(), runtime, 10, 0);
		PassiveApplier.grantBloodlustStacks(runtime, berserker(), result, 0);

		assertEquals(20, runtime.stacks(bulwark));
	}

	@Test
	void builderGrantWithoutPricedPassiveIsInert() {
		ClassDefinition noBulwark = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(), List.of(), 0.0, List.of(ClassRegistry.BLOODLUST_ABILITY), 0, "", List.of());

		PassiveApplier.grantBloodlustStacks(new PlayerRuntime(), noBulwark, BloodlustAbility.Result.fired(5), 0);
		PassiveApplier.grantBloodlustStacks(new PlayerRuntime(), null, BloodlustAbility.Result.fired(5), 0);
	}

	@Test
	void builderGrantOnRejectsGrantsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = berserker().passives().get(0);
		BloodlustAbility.fire(berserker(), runtime, 5, 1_000);
		BloodlustAbility.Result rejected = BloodlustAbility.fire(berserker(), runtime, 5, 16_000);

		PassiveApplier.grantBloodlustStacks(runtime, berserker(), rejected, 16_000);

		assertEquals(0, runtime.stacks(bulwark));

		PlayerRuntime healthy = new PlayerRuntime();
		PassiveApplier.grantBloodlustStacks(healthy, berserker(),
				BloodlustAbility.fire(berserker(), healthy, 4, 0), 0);

		assertEquals(0, healthy.stacks(bulwark));
	}

	@Test
	void builderGrantWrongClassGrantsNothing() {
		PlayerRuntime runtime = new PlayerRuntime();
		ConditionalPassive bulwark = berserker().passives().get(0);
		BloodlustAbility.Result rejected = BloodlustAbility.fire(null, runtime, 10, 0);

		PassiveApplier.grantBloodlustStacks(runtime, berserker(), rejected, 0);

		assertEquals(0, runtime.stacks(bulwark));
	}

	@Test
	void resetReadiesBothLanesAndClearsTheWindow() {
		PlayerRuntime runtime = new PlayerRuntime();
		BloodlustAbility.fire(berserker(), runtime, 10, 0);
		runtime.tryUseSecondWind(0);
		runtime.resetCooldowns();

		assertEquals(0, runtime.bloodlustRemaining(0));
		assertEquals(0, runtime.secondWindRemaining(0));
		assertFalse(runtime.isBloodlustLive(0));
	}
}
