package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarCryAbilityTest {
	private static PlayerClasses warrior() {
		PlayerClasses classes = new PlayerClasses();
		classes.readId("infusedclasses:warrior");
		return classes;
	}

	private static PlayerClasses ranger() {
		PlayerClasses classes = new PlayerClasses();
		classes.readId("infusedclasses:ranger");
		return classes;
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("minecraft", path);
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

	@Test
	void firedResultCarriesTheEffectFacts() {
		PlayerClasses classes = warrior();

		WarCryAbility.Outcome outcome = WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0).outcome();

		assertEquals(id("strength"), outcome.effect());
		assertEquals(160, outcome.durationTicks()); // 8s, as the picker claims
		assertEquals(1, outcome.amplifier()); // Strength II
	}

	@Test
	void firedResultCarriesItsPresentation() {
		PlayerClasses classes = warrior();

		WarCryAbility.Result result = WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0);

		assertEquals(id("entity.ravager.roar"), result.outcome().sound());
		assertEquals(id("angry_villager"), result.outcome().particle());
		assertEquals("War Cry!", result.announcement().text());
		assertEquals(ChatFormatting.RED, result.announcement().style());
	}

	@Test
	void cooldownMessageIsDecidedNotFormattedByTheCaller() {
		PlayerClasses classes = warrior();
		WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 1_000);

		WarCryAbility.Result second = WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 16_000);

		assertEquals("War Cry: 15s", second.announcement().text());
	}

	@Test
	void cooldownMessageRoundsToWholeSeconds() {
		PlayerClasses classes = warrior();
		WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0);

		// 1.4s left rounds down to "1s", 1.6s left rounds up to "2s".
		assertEquals("War Cry: 1s",
				WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 28_600).announcement().text());
		assertEquals("War Cry: 2s",
				WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 28_400).announcement().text());
	}

	@Test
	void cooldownIsThirtySeconds() {
		PlayerClasses classes = warrior();
		WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0);

		assertEquals("War Cry: 30s",
				WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0).announcement().text());
	}

	@Test
	void onCooldownHasNoEffectToApply() {
		PlayerClasses classes = warrior();
		WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0);

		WarCryAbility.Result second = WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 1_000);

		assertNull(second.outcome());
	}

	@Test
	void wrongClassHasNothingAtAllToApply() {
		PlayerClasses classes = ranger();

		WarCryAbility.Result result = WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), 0);

		assertNull(result.outcome());
		assertNull(result.announcement());
	}
}
