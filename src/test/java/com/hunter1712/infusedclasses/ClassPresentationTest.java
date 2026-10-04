package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

		ConditionalPassive bulwark = warrior.passives().get(0);
		// Tooltip shows passive, then active name + dedicated description
		assertTrue(tooltip.contains("Bulwark: " + bulwark.fullDesc() + " (max " + bulwark.maxStacks() + " stacks)"));
		assertTrue(tooltip.contains("Active: War Cry"));
		assertTrue(tooltip.contains("Gives Strength II for 8s"));
		assertTrue(tooltip.endsWith("Click to choose Warrior."));
	}

	@Test
	void warriorTooltipIncludesMaxStacks() {
		ClassDefinition warrior = ClassRegistry.byId(ClassRegistry.WARRIOR);

		String tooltip = ClassPresentation.tooltipFor(warrior);

		ConditionalPassive bulwark = warrior.passives().get(0);
		assertTrue(tooltip.contains(bulwark.name()));
		assertTrue(tooltip.contains(bulwark.fullDesc()));
		assertTrue(tooltip.contains("max " + bulwark.maxStacks() + " stacks"));
	}

	@Test
	void subtitleIsChoiceShapesAbility() {
		assertEquals("Your choice shapes your ability", ClassPresentation.subtitle());
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

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(warrior);

		// Passives section
		assertEquals("Passives", lines.get(0).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE_HEADER, lines.get(0).kind());
		assertEquals(0xFFFFFFFF, lines.get(0).color());
		assertTrue(lines.get(0).bold());
		assertEquals("  Bulwark", lines.get(1).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(1).kind());
		assertEquals(0xFFBDBDBD, lines.get(1).color());
		assertFalse(lines.get(1).bold());
		// Active section
		assertEquals("Active", lines.get(2).text());
		assertEquals(ClassPresentation.LineKind.ABILITY_HEADER, lines.get(2).kind());
		assertEquals(0xFFFFFFFF, lines.get(2).color());
		assertTrue(lines.get(2).bold());
		assertEquals("  War Cry", lines.get(3).text());
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(3).kind());
		assertEquals(0xFF7ED6FF, lines.get(3).color());
		assertFalse(lines.get(3).bold());
	}

	@Test
	void lockedCardLinesArePlaceholders() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(ranger);

		assertEquals(3, lines.size());
		assertEquals("Passives: ???", lines.get(0).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(0).kind());
		assertEquals(0xFFBDBDBD, lines.get(0).color());
		assertFalse(lines.get(0).bold());
		assertEquals("Active: ???", lines.get(1).text());
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(1).kind());
		assertEquals(0xFF7ED6FF, lines.get(1).color());
		assertFalse(lines.get(1).bold());
		assertEquals("COMING SOON", lines.get(2).text());
		assertEquals(ClassPresentation.LineKind.NOTICE, lines.get(2).kind());
		assertEquals(0xFFE8C15A, lines.get(2).color(), "notice draws in the title gold");
		assertTrue(lines.get(2).bold(), "notice draws bold");
	}

	@Test
	void everyLineKindDecidesItsOwnStyle() {
		assertEquals(new ClassPresentation.LineStyle(0xFFFFFFFF, true),
				ClassPresentation.styleFor(ClassPresentation.LineKind.PASSIVE_HEADER));
		assertEquals(new ClassPresentation.LineStyle(0xFFBDBDBD, false),
				ClassPresentation.styleFor(ClassPresentation.LineKind.PASSIVE));
		assertEquals(new ClassPresentation.LineStyle(0xFFFFFFFF, true),
				ClassPresentation.styleFor(ClassPresentation.LineKind.ABILITY_HEADER));
		assertEquals(new ClassPresentation.LineStyle(0xFF7ED6FF, false),
				ClassPresentation.styleFor(ClassPresentation.LineKind.ABILITY));
		assertEquals(new ClassPresentation.LineStyle(0xFFE8C15A, true),
				ClassPresentation.styleFor(ClassPresentation.LineKind.NOTICE));
	}

	@Test
	void everyCardLineCarriesTheStyleOfItsKind() {
		for (ClassDefinition def : ClassRegistry.all()) {
			for (ClassPresentation.CardLine line : ClassPresentation.cardLinesFor(def)) {
				ClassPresentation.LineStyle style = ClassPresentation.styleFor(line.kind());
				assertEquals(style.color(), line.color(), def.id() + " line \"" + line.text() + "\" colour");
				assertEquals(style.bold(), line.bold(), def.id() + " line \"" + line.text() + "\" bold");
			}
		}
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
	void classWithNoPassivesShowsOnlyActive() {
		Identifier customId = net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "custom");
		ClassDefinition def = new ClassDefinition(
				customId, "Custom", false,
				List.of(),
				ClassRegistry.WARRIOR_ABILITY,
				0xFFFFFF, "tag", List.of("Ability line"), "");

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(def);

		// No passives section at all when empty
		assertTrue(lines.stream().noneMatch(l -> l.text().equals("No passives yet")));
		assertEquals("Active", lines.get(0).text());
		assertEquals(ClassPresentation.LineKind.ABILITY_HEADER, lines.get(0).kind());
		assertEquals(0xFFFFFFFF, lines.get(0).color());
		assertTrue(lines.get(0).bold());
		assertEquals("  Ability line", lines.get(1).text());
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(1).kind());
		assertEquals(0xFF7ED6FF, lines.get(1).color());
		assertFalse(lines.get(1).bold());
	}
}