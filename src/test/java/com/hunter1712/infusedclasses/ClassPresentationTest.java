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
	void berserkerPresentationMatchesPickerIntent() {
		ClassDefinition berserker = ClassRegistry.byId(ClassRegistry.BERSERKER);

		assertEquals(0xFFE74C3C, berserker.accentColor());
		assertEquals("Frontline bruiser", berserker.tagline());
		assertEquals(List.of("Bloodlust", "Second Wind"),
				berserker.actives().stream().map(ClassDefinition.ActiveAbility::name).toList());
	}

	@Test
	void comingSoonHasNoAbilityAndNoPassives() {
		for (ClassDefinition def : ClassRegistry.all()) {
			if (!def.locked()) {
				continue;
			}
			assertTrue(def.activeAbilities().isEmpty(), def.id() + " locked must carry no ability");
			assertTrue(def.passives().isEmpty(), def.id() + " locked must carry no passives");
			assertTrue(def.hpPassives().isEmpty(), def.id() + " locked must carry no HP-scaled passives");
			assertTrue(def.actives().isEmpty(), def.id() + " locked must carry no actives");
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
	void berserkerTooltipJoinsAbilityLines() {
		ClassDefinition berserker = ClassRegistry.byId(ClassRegistry.BERSERKER);

		String tooltip = ClassPresentation.tooltipFor(berserker);

		ConditionalPassive bulwark = berserker.passives().get(0);
		// Tooltip shows passive, then one Active entry per active with honest numbers from the constants
		assertTrue(tooltip.contains("Passive: Bulwark \u2014 " + bulwark.fullDesc()));
		assertTrue(tooltip.contains("Active: Bloodlust \u2014 Opens a"));
		assertTrue(tooltip.contains("(R, " + (Cooldowns.BLOODLUST_MS / 1000) + "s)"));
		assertTrue(tooltip.contains("bucket x 2%"));
		assertTrue(tooltip.contains("max 2 HP"));
		assertTrue(tooltip.contains("Active: Second Wind \u2014 Shields"));
		assertTrue(tooltip.contains("(G, " + (Cooldowns.SECOND_WIND_MS / 1000) + "s)"));
		assertTrue(tooltip.contains(SecondWindAbility.HP_PER_STACK + " HP per Bulwark stack"));
		assertTrue(tooltip.contains("Roots you for " + (Spellcast.SLOW_TICKS / 20) + "s"));
		assertTrue(tooltip.endsWith("Click to choose Berserker."));
	}

	@Test
	void berserkerTooltipIncludesMaxStacks() {
		ClassDefinition berserker = ClassRegistry.byId(ClassRegistry.BERSERKER);

		String tooltip = ClassPresentation.tooltipFor(berserker);

		ConditionalPassive bulwark = berserker.passives().get(0);
		assertTrue(tooltip.contains(bulwark.name()));
		assertTrue(tooltip.contains(bulwark.fullDesc()));
		assertTrue(tooltip.contains("(max " + bulwark.maxStacks() + ")"));
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
	void berserkerCardLinesIncludePassivesAndAbilities() {
		ClassDefinition berserker = ClassRegistry.byId(ClassRegistry.BERSERKER);

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(berserker);

		// Passives section
		assertEquals("Passives", lines.get(0).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE_HEADER, lines.get(0).kind());
		assertEquals(0xFFFFFFFF, lines.get(0).style().color());
		assertTrue(lines.get(0).style().bold());
		assertEquals("Bulwark", lines.get(1).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(1).kind());
		assertEquals(0xFFBDBDBD, lines.get(1).style().color());
		assertFalse(lines.get(1).style().bold());
		assertEquals("Fury", lines.get(2).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(2).kind());
		assertEquals(0xFFBDBDBD, lines.get(2).style().color());
		assertFalse(lines.get(2).style().bold());
		// Active section
		assertEquals("Active", lines.get(3).text());
		assertEquals(ClassPresentation.LineKind.ABILITY_HEADER, lines.get(3).kind());
		assertEquals(0xFFFFFFFF, lines.get(3).style().color());
		assertTrue(lines.get(3).style().bold());
		assertEquals("Bloodlust", lines.get(4).text());
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(4).kind());
		assertEquals(0xFF7ED6FF, lines.get(4).style().color());
		assertFalse(lines.get(4).style().bold());
		assertEquals("Second Wind", lines.get(5).text());
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(5).kind());
		assertEquals(0xFF7ED6FF, lines.get(5).style().color());
		assertFalse(lines.get(5).style().bold());
	}

	@Test
	void lockedCardLinesArePlaceholders() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(ranger);

		assertEquals(3, lines.size());
		assertEquals("Passives: ???", lines.get(0).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(0).kind());
		assertEquals(0xFFBDBDBD, lines.get(0).style().color());
		assertFalse(lines.get(0).style().bold());
		assertEquals("Active: ???", lines.get(1).text());
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(1).kind());
		assertEquals(0xFF7ED6FF, lines.get(1).style().color());
		assertFalse(lines.get(1).style().bold());
		assertEquals("COMING SOON", lines.get(2).text());
		assertEquals(ClassPresentation.LineKind.NOTICE, lines.get(2).kind());
		assertEquals(0xFFE8C15A, lines.get(2).style().color(), "notice draws in the title gold");
		assertTrue(lines.get(2).style().bold(), "notice draws bold");
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
	void lockedCardIsVeiledNeverHighlighted() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));

		for (boolean hovered : new boolean[] { false, true }) {
			ClassPresentation.CardStyle style = ClassPresentation.cardStyleFor(ranger, hovered);

			assertEquals(0xFF3A3A3A, style.borderColor());
			assertNull(style.washColor());
			assertEquals(0x50000000, style.veilColor());
			assertEquals(0xFF8A8A8A, style.nameColor());
		}
	}

	@Test
	void unlockedCardHighlightsOnlyOnHover() {
		ClassDefinition berserker = ClassRegistry.byId(ClassRegistry.BERSERKER);

		ClassPresentation.CardStyle plain = ClassPresentation.cardStyleFor(berserker, false);
		assertEquals(0xFF3A3A3A, plain.borderColor());
		assertNull(plain.washColor());
		assertNull(plain.veilColor());
		assertEquals(0xFFFFFFFF, plain.nameColor());

		ClassPresentation.CardStyle hovered = ClassPresentation.cardStyleFor(berserker, true);
		assertEquals(0xFF8A8A8A, hovered.borderColor());
		assertEquals(0x14FFFFFF, hovered.washColor());
		assertNull(hovered.veilColor());
		assertEquals(0xFFFFFFFF, hovered.nameColor());
	}

	@Test
	void everyCardLineCarriesTheStyleOfItsKind() {
		for (ClassDefinition def : ClassRegistry.all()) {
			for (ClassPresentation.CardLine line : ClassPresentation.cardLinesFor(def)) {
				assertEquals(ClassPresentation.styleFor(line.kind()), line.style(),
						def.id() + " line \"" + line.text() + "\" draws with the style of its kind");
			}
		}
	}

	@Test
	void lockedAccentIsGrey() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));
		ClassDefinition berserker = ClassRegistry.byId(ClassRegistry.BERSERKER);

		assertEquals(0xFF555555, ClassPresentation.accentFor(ranger));
		assertEquals(0xFFE74C3C, ClassPresentation.accentFor(berserker));
	}

	@Test
	void buttonForLockedIsInactive() {
		ClassDefinition ranger = ClassRegistry.byId(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "ranger"));
		ClassDefinition berserker = ClassRegistry.byId(ClassRegistry.BERSERKER);

		assertEquals(new ClassPresentation.ButtonData("Locked", false), ClassPresentation.buttonFor(ranger));
		assertEquals(new ClassPresentation.ButtonData("Select", true), ClassPresentation.buttonFor(berserker));
	}

	@Test
	void classWithNoPassivesShowsOnlyActive() {
		Identifier customId = net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "custom");
		ClassDefinition def = new ClassDefinition(
				customId, "Custom", false,
				List.of(),
				List.of(),
				0.0,
				List.of(ClassRegistry.SECOND_WIND_ABILITY),
				0xFFFFFF, "tag",
				List.of(new ClassDefinition.ActiveAbility("Ability line", "Does a thing.")));

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(def);

		// Empty passives render the placeholder under the header (08)
		assertEquals("Passives", lines.get(0).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE_HEADER, lines.get(0).kind());
		assertEquals("No passives yet", lines.get(1).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(1).kind());
		assertEquals(0xFFBDBDBD, lines.get(1).style().color());
		assertFalse(lines.get(1).style().bold());
		assertEquals("Active", lines.get(2).text());
		assertEquals(ClassPresentation.LineKind.ABILITY_HEADER, lines.get(2).kind());
		assertEquals(0xFFFFFFFF, lines.get(2).style().color());
		assertTrue(lines.get(2).style().bold());
		assertEquals("Ability line", lines.get(3).text());
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(3).kind());
		assertEquals(0xFF7ED6FF, lines.get(3).style().color());
		assertFalse(lines.get(3).style().bold());
	}
}