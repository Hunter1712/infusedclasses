package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RendAbilityTest {
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
			runtime.onTrigger(juggernautDef(), ConditionalPassive.ON_HIT_DEALT, now, 1.0F);
		}
		return runtime;
	}

	@Test
	void empowerMathAtZeroOneTen() {
		assertEquals(0, RendAbility.empowerFor(0));
		assertEquals(7.0F, RendAbility.empowerFor(1));
		assertEquals(16.0F, RendAbility.empowerFor(10));
		assertEquals("minecraft:block.anvil.land", RendAbility.CAST_SOUND.toString());
		assertEquals("Smash!", RendAbility.CAST_WORD);
	}

	@Test
	void noExecuteBonusAtAnyTargetHp() {
		// The empower is the empower: target health never enters the decision.
		assertEquals(16.0F, RendAbility.empowerFor(10));
		assertEquals(RendAbility.BASE_HP + 10 * RendAbility.HP_PER_STACK, RendAbility.empowerFor(10));
	}

	@Test
	void spendsTheHitDealtPassiveWhateverItsPosition() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4, ClassRegistry.ARMOR,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_armor"), 0.5, 2);
		ConditionalPassive momentum = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives().get(0);
		ClassDefinition shuffled = new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				List.of(swift, momentum), List.of(), 0.0, List.of(ClassRegistry.REND_ABILITY), 0, "", List.of());

		assertEquals(momentum, shuffled.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT));
		assertNull(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "empty"), "Empty", false,
				List.of(), List.of(), 0.0, List.of(ClassRegistry.REND_ABILITY), 0, "", List.of())
				.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT));
	}

	@Test
	void fireEmpowersAndArmsOnlyItsOwnLane() {
		PlayerRuntime runtime = withStacks(5, 0);
		RendAbility.Result result = RendAbility.fire(juggernautDef(), runtime, 0);

		assertEquals(RendAbility.Kind.FIRED, result.kind());
		assertEquals(11.0F, result.empowerHp());
		assertTrue(result.consume());
		assertEquals(Cooldowns.REND_MS, runtime.rendRemaining(0));
		assertEquals(120, RendAbility.WINDOW_TICKS);
	}

	@Test
	void fireStampsASingleUseEmpower() {
		PlayerRuntime runtime = withStacks(10, 0);
		RendAbility.fire(juggernautDef(), runtime, 0);

		assertEquals(10, runtime.peekRendEmpowerStacks(0));
		assertEquals(10, runtime.peekRendEmpowerStacks(5_999));
		assertEquals(-1, runtime.peekRendEmpowerStacks(6_000));
		assertEquals(10, runtime.consumeRendEmpower(0));
		assertEquals(-1, runtime.consumeRendEmpower(0));
	}

	@Test
	void unswungWindowExpiresWithoutRefunding() {
		PlayerRuntime runtime = withStacks(10, 0);
		RendAbility.fire(juggernautDef(), runtime, 0);

		assertEquals(-1, runtime.peekRendEmpowerStacks(6_001));
		assertEquals(-1, runtime.consumeRendEmpower(6_001));
		// The lane stays armed: the whiff cost time, and the stacks are already spent.
		assertEquals(Cooldowns.REND_MS - 6_001, runtime.rendRemaining(6_001));
	}

	@Test
	void fizzleBurnsNothing() {
		PlayerRuntime runtime = withStacks(0, 0);
		RendAbility.Result result = RendAbility.fire(juggernautDef(), runtime, 0);

		assertEquals(RendAbility.Kind.NO_MOMENTUM, result.kind());
		assertEquals("No Momentum", result.announcement().text());
		assertEquals(0, result.empowerHp());
		assertFalse(result.consume());
		assertEquals(0, runtime.rendRemaining(0));
		assertEquals(-1, runtime.peekRendEmpowerStacks(0));
	}

	@Test
	void wrongClassSilentAndUntouched() {
		PlayerRuntime runtime = withStacks(5, 0);
		RendAbility.Result result = RendAbility.fire(null, runtime, 0);

		assertEquals(RendAbility.Kind.WRONG_CLASS, result.kind());
		assertNull(result.announcement());
		assertEquals(0, runtime.rendRemaining(0));
		assertEquals(-1, runtime.peekRendEmpowerStacks(0));
	}

	@Test
	void cooldownIndependentAndNeverRearmedByRejects() {
		PlayerRuntime runtime = withStacks(5, 0);
		assertEquals(0, runtime.rendRemaining(0));

		RendAbility.fire(juggernautDef(), runtime, 1_000);
		RendAbility.Result second = RendAbility.fire(juggernautDef(), runtime, 16_000);

		assertEquals(RendAbility.Kind.ON_COOLDOWN, second.kind());
		assertEquals(45_000, second.remainingMs());
		assertEquals(45_000, runtime.rendRemaining(16_000));
	}

	@Test
	void resetReadiesTheLaneAndClearsTheEmpower() {
		PlayerRuntime runtime = withStacks(5, 0);
		RendAbility.fire(juggernautDef(), runtime, 0);
		runtime.resetCooldowns();

		assertEquals(0, runtime.rendRemaining(0));
		assertEquals(-1, runtime.peekRendEmpowerStacks(0));
		assertEquals(-1, runtime.consumeRendEmpower(0));
	}

	@Test
	void fireThenDropSpendsWhatFirePriced() {
		PlayerRuntime runtime = withStacks(4, 0);
		RendAbility.Result result = RendAbility.fire(juggernautDef(), runtime, 0);

		assertEquals(RendAbility.Kind.FIRED, result.kind());
		assertTrue(result.consume());
		// The entry honors the flag through consumeSpent; its pure half is pinned
		// here: stacks drop once, a second drop finds nothing, and the armed
		// snapshot survives fire-time (hit-time spend is a separate event).
		assertTrue(PassiveApplier.dropStacks(runtime, RendAbility.spentPassive(juggernautDef())));
		assertEquals(0, runtime.stacks(juggernautDef().passives().get(0)));
		assertFalse(PassiveApplier.dropStacks(runtime, RendAbility.spentPassive(juggernautDef())));
		assertEquals(4, runtime.peekRendEmpowerStacks(0));
	}
}
