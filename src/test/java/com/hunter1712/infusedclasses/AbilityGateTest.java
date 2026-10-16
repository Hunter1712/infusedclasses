package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

		assertRejected(RushAbility.fire(null, cold, 0));
		assertEquals(0, cold.rushRemaining(0));
		assertRejected(RendAbility.fire(null, cold, 0).outcome());
		assertEquals(0, cold.rendRemaining(0));
		assertRejected(RotateAbility.fire(null, cold, 0).outcome());
		assertEquals(0, cold.rotateRemaining(0));
		assertRejected(AequilibriumAbility.fire(juggernaut, cold, 0));
		assertEquals(0, cold.oathRemaining(0));
		assertTrue(AequilibriumAbility.fire(legionary, new PlayerRuntime(), 0).fired());
		assertTrue(RotateAbility.fire(legionary, new PlayerRuntime(), 0).outcome().fired());
	}

	/** A wrong-class press: nothing fired, nothing said. */
	private static void assertRejected(AbilityOutcome outcome) {
		assertFalse(outcome.fired());
		assertNull(outcome.announcement());
	}
}
