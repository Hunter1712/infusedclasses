package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gate-and-arm rule: rejects pass through, fires arm, failed arms cool down. */
class AbilityGateTest {
	@Test
	void rejectPassesThroughWithoutArming() {
		AtomicBoolean armed = new AtomicBoolean();

		String out = AbilityGate.gate("reject", false, () -> {
			armed.set(true);
			return true;
		}, () -> 99L, r -> "cooldown:" + r);

		assertEquals("reject", out);
		assertFalse(armed.get());
	}

	@Test
	void firedArmsAndPassesThrough() {
		String out = AbilityGate.gate("fired", true, () -> true, () -> 99L, r -> "cooldown:" + r);

		assertEquals("fired", out);
	}

	@Test
	void failedArmReportsCooldown() {
		String out = AbilityGate.gate("fired", true, () -> false, () -> 15_000L, r -> "cooldown:" + r);

		assertEquals("cooldown:15000", out);
	}

	@Test
	void everyAbilityArmsOnlyOnFire() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
		ClassDefinition legionary = ClassRegistry.byId(ClassRegistry.LEGIONARY);
		PlayerRuntime cold = new PlayerRuntime();

		assertTrue(RushAbility.fire(null, cold, 0).kind() == RushAbility.Kind.WRONG_CLASS
				&& cold.rushRemaining(0) == 0);
		assertTrue(RendAbility.fire(null, cold, 0).kind() == RendAbility.Kind.WRONG_CLASS
				&& cold.rendRemaining(0) == 0);
		assertTrue(RotateAbility.fire(null, cold, 0).kind() == RotateAbility.Kind.WRONG_CLASS
				&& cold.rotateRemaining(0) == 0);
		assertTrue(ImpetusAbility.fire(juggernaut, cold, 0).kind() == ImpetusAbility.Kind.WRONG_CLASS
				&& cold.oathRemaining(0) == 0);
		assertTrue(TestudoAbility.fire(juggernaut, cold, 0).kind() == TestudoAbility.Kind.WRONG_CLASS
				&& cold.oathRemaining(0) == 0);
		assertEquals(RotateAbility.Kind.FIRED, RotateAbility.fire(legionary, new PlayerRuntime(), 0).kind());
	}
}
