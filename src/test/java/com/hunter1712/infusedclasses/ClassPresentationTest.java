package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassPresentationTest {
	@Test
	void juggernautPresentationMatchesPickerIntent() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		assertEquals(0xFFE74C3C, juggernaut.accentColor());
		assertEquals("Frontline bruiser", juggernaut.tagline());
		assertEquals(List.of("Offload", "Onslaught"),
				juggernaut.actives().stream().map(ClassDefinition.ActiveAbility::name).toList());
	}

	@Test
	void everyRegisteredClassIsPlayableWithActives() {
		for (ClassDefinition def : ClassRegistry.all()) {
			assertFalse(def.actives().isEmpty(), def.id() + " must carry an ability");
			for (ClassDefinition.ActiveAbility active : def.actives()) {
				assertNotNull(active.id(), def.id() + " active \"" + active.name() + "\" must carry its id");
			}
		}
	}

	@Test
	void juggernautCardLinesCarryNamePlusDescription() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
		ConditionalPassive momentum = juggernaut.passives().get(0);

		List<String> texts = ClassPresentation.cardLinesFor(juggernaut).stream()
				.map(ClassPresentation.CardLine::text).toList();

		assertTrue(texts.contains("Momentum \u2014 " + momentum.fullDesc()));
		assertTrue(texts.stream().anyMatch(t -> t.contains("Max " + momentum.maxStacks() + ".")));
		assertTrue(texts.stream().anyMatch(t -> t.startsWith("Offload \u2014 ")
				&& t.contains("(R, " + (Lane.OFFLOAD.cooldownMs() / 1000) + "s)")));
		assertTrue(texts.stream().anyMatch(t -> t.startsWith("Onslaught \u2014 ")
				&& t.contains("per full hit") && t.contains("spending 2")));
		assertFalse(texts.stream().anyMatch(t -> t.contains("Roots")));
	}

	@Test
	void descStartIsDerivedFromTheJoin() {
		ClassPresentation.CardLine withDesc =
				new ClassPresentation.CardLine("Rush", "Engage.", ClassPresentation.LineKind.ABILITY);

		assertEquals("Rush \u2014 ".length(), withDesc.descStart());
		assertEquals("Engage.", withDesc.text().substring(withDesc.descStart()));

		ClassPresentation.CardLine bare =
				new ClassPresentation.CardLine("Passives", null, ClassPresentation.LineKind.PASSIVE_HEADER);

		// No description: the boundary is the end, so the whole row reads as the name.
		assertEquals(bare.text().length(), bare.descStart());
	}

	@Test
	void legionaryCardLinesGroupFlipsAsPassives() {
		ClassDefinition legionary = ClassRegistry.byId(ClassRegistry.LEGIONARY);

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(legionary);
		List<String> texts = lines.stream().map(ClassPresentation.CardLine::text).toList();

		assertFalse(texts.contains("No passives yet"));
		assertTrue(texts.stream().anyMatch(t -> t.startsWith("Rotate \u2014 ")));
		assertTrue(texts.stream().anyMatch(t -> t.startsWith("Aequilibrium \u2014 ")));
		for (ClassPresentation.CardLine line : lines) {
			if (line.text().startsWith("Impetus \u2014 ") || line.text().startsWith("Testudo \u2014 ")) {
				assertEquals(ClassPresentation.LineKind.PASSIVE, line.kind());
			}
			if (line.text().startsWith("Rotate \u2014 ") || line.text().startsWith("Aequilibrium \u2014 ")) {
				assertEquals(ClassPresentation.LineKind.ABILITY, line.kind());
			}
		}
	}

	@Test
	void gamblerCardShowsGeneratedDiceLinesUnderPassives() {
		ClassDefinition gambler = ClassRegistry.byId(ClassRegistry.GAMBLER);
		CritRule dice = gambler.critRule();

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(gambler);
		List<String> texts = lines.stream().map(ClassPresentation.CardLine::text).toList();

		assertFalse(texts.contains("No passives yet"));
		assertTrue(texts.contains(CritRule.CROOKED_DICE + " \u2014 " + dice.crookedDiceDesc()));
		assertTrue(texts.contains(CritRule.STREAK + " \u2014 " + dice.streakDesc()));
		assertTrue(texts.contains("Roulette \u2014 " + RouletteAbility.desc()));
		assertTrue(texts.contains("All In \u2014 " + AllInAbility.desc()));
		// The dice lines are passives, not actives: they render above the Active header.
		int activeHeader = texts.indexOf("Active");
		assertTrue(texts.indexOf(CritRule.STREAK + " \u2014 " + dice.streakDesc()) < activeHeader);
		for (ClassPresentation.CardLine line : lines) {
			if (line.text().startsWith(CritRule.CROOKED_DICE + " \u2014 ")
					|| line.text().startsWith(CritRule.STREAK + " \u2014 ")) {
				assertEquals(ClassPresentation.LineKind.PASSIVE, line.kind());
			}
		}
	}

	@Test
	void subtitleIsChoiceShapesAbility() {
		assertEquals("Your choice shapes your ability", ClassPresentation.subtitle());
	}

	@Test
	void juggernautCardLinesIncludeHeadersAndDetails() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(juggernaut);

		// Passives section
		assertEquals("Passives", lines.get(0).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE_HEADER, lines.get(0).kind());
		assertEquals(0xFFFFFFFF, lines.get(0).style().color());
		assertTrue(lines.get(0).style().bold());
		assertTrue(lines.get(1).text().startsWith("Momentum \u2014 "));
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(1).kind());
		assertEquals(0xFFBDBDBD, lines.get(1).style().color());
		assertFalse(lines.get(1).style().bold());
		assertTrue(lines.get(2).text().startsWith("Burden \u2014 "));
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(2).kind());
		// Active section
		assertEquals("Active", lines.get(3).text());
		assertEquals(ClassPresentation.LineKind.ABILITY_HEADER, lines.get(3).kind());
		assertTrue(lines.get(4).text().startsWith("Offload \u2014 "));
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(4).kind());
		assertEquals(0xFF7ED6FF, lines.get(4).style().color());
		assertTrue(lines.get(5).text().startsWith("Onslaught \u2014 "));
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
	}

	@Test
	void everyCardHighlightsOnlyOnHover() {
		for (ClassDefinition def : ClassRegistry.all()) {
			ClassPresentation.CardStyle plain = ClassPresentation.cardFor(def, false).style();
			assertEquals(0xFF3A3A3A, plain.borderColor());
			assertEquals(null, plain.washColor());
			assertEquals(0xFFFFFFFF, plain.nameColor());

			ClassPresentation.CardStyle hovered = ClassPresentation.cardFor(def, true).style();
			assertEquals(0xFF8A8A8A, hovered.borderColor());
			assertEquals(0x14FFFFFF, hovered.washColor());
			assertEquals(0xFFFFFFFF, hovered.nameColor());
		}
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
	void accentIsTheClassColor() {
		assertEquals(0xFFE74C3C,
				ClassPresentation.cardFor(ClassRegistry.byId(ClassRegistry.JUGGERNAUT), false).text().accent());
		assertEquals(0xFFB87333,
				ClassPresentation.cardFor(ClassRegistry.byId(ClassRegistry.LEGIONARY), false).text().accent());
		assertEquals(0xFFD4AF37,
				ClassPresentation.cardFor(ClassRegistry.byId(ClassRegistry.GAMBLER), false).text().accent());
	}

	@Test
	void classWithNoPassivesShowsOnlyActive() {
		Identifier customId = net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "custom");
		ClassDefinition def = new ClassDefinition(
				customId, "Custom",
				List.of(),
				List.of(),
				0xFFFFFF, "tag",
				List.of(new ClassDefinition.ActiveAbility(ClassRegistry.ONSLAUGHT_ABILITY, "Ability line", "Does a thing.")),
				List.of(),
				CritRule.universal());

		List<ClassPresentation.CardLine> lines = ClassPresentation.cardLinesFor(def);

		// Empty passives render the placeholder under the header
		assertEquals("Passives", lines.get(0).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE_HEADER, lines.get(0).kind());
		assertEquals("No passives yet", lines.get(1).text());
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(1).kind());
		assertEquals(0xFFBDBDBD, lines.get(1).style().color());
		assertFalse(lines.get(1).style().bold());
		assertEquals("Active", lines.get(2).text());
		assertEquals(ClassPresentation.LineKind.ABILITY_HEADER, lines.get(2).kind());
		assertEquals("Ability line \u2014 Does a thing.", lines.get(3).text());
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(3).kind());
		assertEquals(0xFF7ED6FF, lines.get(3).style().color());
		assertFalse(lines.get(3).style().bold());
	}
}
