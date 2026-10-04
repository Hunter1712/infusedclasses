package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
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

	@Test
	void warriorTooltipJoinsAbilityLines() {
		ClassDefinition warrior = ClassRegistry.byId(ClassRegistry.WARRIOR);

		String tooltip = ClassPresentation.tooltipFor(warrior);

		assertEquals("War Cry (R) — Strength burst. Click to choose Warrior.", tooltip);
	}

	@Test
	void lockedTooltipIsComingSoon() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));

		String tooltip = ClassPresentation.tooltipFor(ranger);

		assertEquals("Coming soon", tooltip);
	}

	@Test
	void warriorCardLinesIncludePassivesAndAbilities() {
		ClassDefinition warrior = ClassRegistry.byId(ClassRegistry.WARRIOR);

		List<String> lines = ClassPresentation.cardLinesFor(warrior);

		assertTrue(lines.stream().anyMatch(l -> l.contains("Max Health")));
		assertTrue(lines.stream().anyMatch(l -> l.contains("Armor")));
		assertTrue(lines.stream().anyMatch(l -> l.contains("Toughness")));
		assertTrue(lines.stream().anyMatch(l -> l.contains("Move Speed")));
		assertTrue(lines.stream().anyMatch(l -> l.equals("War Cry (R)")));
		assertTrue(lines.stream().anyMatch(l -> l.equals("Strength burst")));
	}

	@Test
	void lockedCardLinesArePlaceholders() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));

		List<String> lines = ClassPresentation.cardLinesFor(ranger);

		assertEquals(3, lines.size());
		assertEquals("Passives: ???", lines.get(0));
		assertEquals("Ability: ???", lines.get(1));
		assertEquals("COMING SOON", lines.get(2));
	}

	@Test
	void lockedAccentIsGrey() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));
		ClassDefinition warrior = ClassRegistry.byId(ClassRegistry.WARRIOR);

		assertEquals(0xFF555555, ClassPresentation.accentFor(ranger));
		assertEquals(0xFFE74C3C, ClassPresentation.accentFor(warrior));
	}

	@Test
	void buttonForLockedIsInactive() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));
		ClassDefinition warrior = ClassRegistry.byId(ClassRegistry.WARRIOR);

		assertEquals(new ClassPresentation.ButtonData("Locked", false), ClassPresentation.buttonFor(ranger));
		assertEquals(new ClassPresentation.ButtonData("Select", true), ClassPresentation.buttonFor(warrior));
	}

	@Test
	void classWithNoPassivesShowsPlaceholder() {
		Identifier customId = net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "custom");
		ClassDefinition def = new ClassDefinition(
				customId, "Custom", false,
				List.of(),
				ClassRegistry.WARRIOR_ABILITY,
				0xFFFFFF, "tag", List.of("Ability line"));

		List<String> lines = ClassPresentation.cardLinesFor(def);

		assertTrue(lines.stream().anyMatch(l -> l.equals("No passives yet")));
		assertTrue(lines.stream().anyMatch(l -> l.equals("Ability line")));
	}
}