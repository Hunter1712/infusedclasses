package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondWindAbilityTest {
	private static PlayerClasses berserker() {
		PlayerClasses classes = new PlayerClasses();
		classes.readId("infusedclasses:berserker");
		return classes;
	}

	private static ClassDefinition berserkerDef() {
		return berserker().currentDefinition();
	}

	private static PlayerRuntime withStacks(int stacks, long now) {
		PlayerRuntime runtime = new PlayerRuntime();
		for (int i = 0; i < stacks; i++) {
			runtime.onTrigger(berserkerDef(), ConditionalPassive.ON_HIT_TAKEN, now);
		}
		return runtime;
	}

	@Test
	void shieldMathAtZeroFiveTwenty() {
		assertEquals(0, SecondWindAbility.shieldAmount(0));
		assertEquals(5.0F, SecondWindAbility.shieldAmount(5));
		assertEquals(20.0F, SecondWindAbility.shieldAmount(20));
	}

	@Test
	void shieldWindowAndAmplifierCoverTheStacks() {
		assertEquals(200, SecondWindAbility.ABSORPTION_TICKS);
		assertEquals(0, SecondWindAbility.amplifierFor(1.0F));
		assertEquals(0, SecondWindAbility.amplifierFor(4.0F));
		assertEquals(1, SecondWindAbility.amplifierFor(5.0F));
		assertEquals(4, SecondWindAbility.amplifierFor(20.0F));
	}

	@Test
	void spendsTheHitTakenPassiveWhateverItsPosition() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ConditionalPassive bulwark = ClassRegistry.byId(ClassRegistry.BERSERKER).passives().get(0);
		ClassDefinition shuffled = new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(swift, bulwark), List.of(), 0.0, List.of(ClassRegistry.SECOND_WIND_ABILITY), 0, "", List.of());

		assertEquals(bulwark, SecondWindAbility.bulwarkOf(shuffled));
		assertNull(SecondWindAbility.bulwarkOf(null));
		assertNull(SecondWindAbility.bulwarkOf(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "empty"), "Empty", false,
				List.of(), List.of(), 0.0, List.of(ClassRegistry.SECOND_WIND_ABILITY), 0, "", List.of())));
	}

	@Test
	void fireShieldsAndArmsOnlyItsOwnLane() {
		PlayerRuntime runtime = withStacks(5, 0);
		SecondWindAbility.Result result = SecondWindAbility.fire(berserkerDef(), runtime, 0);

		assertEquals(SecondWindAbility.Kind.FIRED, result.kind());
		assertEquals(5.0F, result.shieldHp());
		assertTrue(result.consume());
		assertEquals(Cooldowns.SECOND_WIND_MS, runtime.secondWindRemaining(0));
	}

	@Test
	void fizzleBurnsNothing() {
		PlayerRuntime runtime = withStacks(0, 0);
		SecondWindAbility.Result result = SecondWindAbility.fire(berserkerDef(), runtime, 0);

		assertEquals(SecondWindAbility.Kind.NO_BULWARK, result.kind());
		assertEquals("No Bulwark", result.announcement().text());
		assertEquals(0, result.shieldHp());
		assertFalse(result.consume());
		assertEquals(0, runtime.secondWindRemaining(0));
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = withStacks(5, 0);
		SecondWindAbility.Result result = SecondWindAbility.fire(null, runtime, 0);

		assertEquals(SecondWindAbility.Kind.WRONG_CLASS, result.kind());
		assertNull(result.announcement());
		assertEquals(0, runtime.secondWindRemaining(0));
	}

	@Test
	void cooldownIndependentAndNeverRearmedByRejects() {
		PlayerRuntime runtime = withStacks(5, 0);
		assertEquals(0, runtime.secondWindRemaining(0));

		SecondWindAbility.fire(berserkerDef(), runtime, 1_000);
		SecondWindAbility.Result second = SecondWindAbility.fire(berserkerDef(), runtime, 16_000);

		assertEquals(SecondWindAbility.Kind.ON_COOLDOWN, second.kind());
		assertEquals(45_000, second.remainingMs());
		assertEquals(45_000, runtime.secondWindRemaining(16_000));
	}

	@Test
	void resetReadiesTheLane() {
		PlayerRuntime runtime = withStacks(5, 0);
		runtime.tryUseSecondWind(0);
		runtime.resetCooldowns();

		assertEquals(0, runtime.secondWindRemaining(0));
	}
}
