package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarCryAbilityTest {
	private static PlayerClasses warrior() {
		PlayerClasses classes = new PlayerClasses();
		classes.readId("infusedclasses:warrior");
		return classes;
	}

	@Test
	void warriorFiresWhenReady() {
		PlayerClasses classes = warrior();
		WarCryAbility.Result result = WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0);

		assertEquals(WarCryAbility.Kind.FIRED, result.kind());
		assertEquals(0, result.remainingMs());
	}

	@Test
	void secondUseInsideCooldownReportsRemaining() {
		PlayerClasses classes = warrior();
		WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 1_000);

		WarCryAbility.Result second = WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 16_000);

		assertEquals(WarCryAbility.Kind.ON_COOLDOWN, second.kind());
		assertEquals(15_000, second.remainingMs());
	}

	@Test
	void firesAgainAtCooldownEdge() {
		PlayerClasses classes = warrior();
		WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0);

		assertTrue(WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), Cooldowns.WAR_CRY_MS).kind() == WarCryAbility.Kind.FIRED);
	}

	@Test
	void nonWarriorIsRejectedWithoutTouchingCooldown() {
		PlayerClasses classes = new PlayerClasses();
		classes.readId("infusedclasses:ranger");

		WarCryAbility.Result result = WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0);

		assertEquals(WarCryAbility.Kind.WRONG_CLASS, result.kind());
		assertEquals(0, classes.cooldowns().warCryRemaining(0));
	}

	@Test
	void missingClassIsRejected() {
		WarCryAbility.Result result = WarCryAbility.trigger(null, new Cooldowns(), 0);

		assertEquals(WarCryAbility.Kind.WRONG_CLASS, result.kind());
	}
}
