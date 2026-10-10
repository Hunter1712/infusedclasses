package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassPresentationTest {
	@Test
	void juggernautPresentationMatchesPickerIntent() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		assertEquals(0xFFE74C3C, juggernaut.accentColor());
		assertEquals("Frontline bruiser", juggernaut.tagline());
		assertEquals(List.of("Rush", "Rend"),
				juggernaut.actives().stream().map(ClassDefinition.ActiveAbility::name).toList());
	}

	@Test
	void everyRegisteredClassIsPlayableWithActives() {
		for (ClassDefinition def : ClassRegistry.all()) {
			assertFalse(def.locked(), def.id() + " must be playable");
			assertFalse(def.activeAbilities().isEmpty(), def.id() + " must carry an ability");
			assertFalse(def.actives().isEmpty(), def.id() + " must carry an active");
		}
	}

	@Test
	void juggernautCardLinesCarryNamePlusDescription() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
		ConditionalPassive momentum = juggernaut.passives().get(0);

		List<String> texts = ClassPresentation.cardLinesFor(juggernaut).stream()
				.map(ClassPresentation.CardLine::text).toList();

		assertTrue(texts.contains("Momentum \u2014 " + momentum.fullDesc()));
		assertTrue(texts.stream().anyMatch(t -> t.contains("(max " + momentum.maxStacks() + ")")));
		assertTrue(texts.stream().anyMatch(t -> t.startsWith("Rush \u2014 ")
				&& t.contains("(R, " + (Cooldowns.RUSH_MS / 1000) + "s)")));
		assertTrue(texts.stream().anyMatch(t -> t.startsWith("Rend \u2014 ")
				&& t.contains("per Momentum")));
		assertFalse(texts.stream().anyMatch(t -> t.contains("Roots")));
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
		assertTrue(lines.get(2).text().startsWith("Vigor \u2014 "));
		assertEquals(ClassPresentation.LineKind.PASSIVE, lines.get(2).kind());
		// Active section
		assertEquals("Active", lines.get(3).text());
		assertEquals(ClassPresentation.LineKind.ABILITY_HEADER, lines.get(3).kind());
		assertTrue(lines.get(4).text().startsWith("Rush \u2014 "));
		assertEquals(ClassPresentation.LineKind.ABILITY, lines.get(4).kind());
		assertEquals(0xFF7ED6FF, lines.get(4).style().color());
		assertTrue(lines.get(5).text().startsWith("Rend \u2014 "));
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
	}

	@Test
	void buttonIsAlwaysActiveSelect() {
		for (ClassDefinition def : ClassRegistry.all()) {
			assertEquals(new ClassPresentation.ButtonData("Select", true),
					ClassPresentation.cardFor(def, false).text().button());
		}
	}

	@Test
	void classWithNoPassivesShowsOnlyActive() {
		Identifier customId = net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "custom");
		ClassDefinition def = new ClassDefinition(
				customId, "Custom", false,
				List.of(),
				List.of(),
				0.0,
				List.of(ClassRegistry.REND_ABILITY),
				0xFFFFFF, "tag",
				List.of(new ClassDefinition.ActiveAbility("Ability line", "Does a thing.")),
				List.of());

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
