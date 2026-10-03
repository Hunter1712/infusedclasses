package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassPresentationTest {
	@Test
	void warriorPresentationMatchesPickerIntent() {
		ClassDefinition warrior = ClassRegistry.byId(ClassRegistry.WARRIOR);

		assertEquals(0xFFE74C3C, warrior.accentColor());
		assertEquals("Frontline bruiser", warrior.tagline());
		assertEquals(List.of("War Cry (R)", "Strength burst"), warrior.abilityLines());
	}

	@Test
	void comingSoonHasNoAbilityAndNoPassives() {
		for (ClassDefinition def : ClassRegistry.all()) {
			if (!def.locked()) {
				continue;
			}
			assertNull(def.activeAbility(), def.id() + " locked must carry no ability");
			assertTrue(def.passives().isEmpty(), def.id() + " locked must carry no passives");
		}
	}

	@Test
	void lockedPlaceholdersStillCarryPickerText() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));

		assertEquals("Wilds archer", ranger.tagline());
		assertEquals(0xFF2ECC71, ranger.accentColor());
	}
}
