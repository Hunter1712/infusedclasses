package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Fire-then-arm collapsed: production path gates and arms together. */
class WarCryFireTest {
	private static PlayerClasses warrior() {
		PlayerClasses classes = new PlayerClasses();
		classes.readId("infusedclasses:warrior");
		return classes;
	}

	@Test
	void fireArmsTheCooldown() {
		PlayerRuntime runtime = new PlayerRuntime();
		WarCryAbility.Result result = WarCryAbility.fire(warrior().currentDefinition(), runtime, 0);

		assertEquals(WarCryAbility.Kind.FIRED, result.kind());
		assertEquals(Cooldowns.WAR_CRY_MS, runtime.warCryRemaining(0));
	}

	@Test
	void secondFireInsideCooldownReportsRemainingWithoutRearming() {
		PlayerRuntime runtime = new PlayerRuntime();
		WarCryAbility.fire(warrior().currentDefinition(), runtime, 1_000);

		WarCryAbility.Result second = WarCryAbility.fire(warrior().currentDefinition(), runtime, 16_000);

		assertEquals(WarCryAbility.Kind.ON_COOLDOWN, second.kind());
		assertEquals(15_000, second.remainingMs());
		assertEquals(15_000, runtime.warCryRemaining(16_000));
	}

	@Test
	void wrongClassLeavesCooldownUntouched() {
		PlayerRuntime runtime = new PlayerRuntime();
		WarCryAbility.Result result = WarCryAbility.fire(null, runtime, 0);

		assertEquals(WarCryAbility.Kind.WRONG_CLASS, result.kind());
		assertEquals(0, runtime.warCryRemaining(0));
	}
}
