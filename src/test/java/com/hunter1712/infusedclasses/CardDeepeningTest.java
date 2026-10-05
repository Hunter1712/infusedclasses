package com.hunter1712.infusedclasses;

import com.hunter1712.infusedclasses.client.ClassCardRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardDeepeningTest {
	@Test
	void cardForAgreesWithItsParts() {
		ClassDefinition warrior = ClassRegistry.byId(ClassRegistry.WARRIOR);

		ClassPresentation.CardModel plain = ClassPresentation.cardFor(warrior, false);

		assertEquals(ClassPresentation.cardLinesFor(warrior).size(), plain.lines().size());
		assertEquals(ClassPresentation.cardStyleFor(warrior, false), plain.style());
		assertEquals(ClassPresentation.accentFor(warrior), plain.accent());
		assertEquals(ClassPresentation.buttonFor(warrior), plain.button());
		assertEquals(ClassPresentation.tooltipFor(warrior), plain.tooltip());
	}

	@Test
	void lockedCardCarriesLockedLookInOnePlace() {
		ClassDefinition ranger = ClassRegistry.byId(net.minecraft.resources.Identifier
				.fromNamespaceAndPath("infusedclasses", "ranger"));

		ClassPresentation.CardModel card = ClassPresentation.cardFor(ranger, false);

		assertEquals("Locked", card.button().label());
		assertEquals("Coming soon", card.tooltip());
		assertEquals(0xFF555555, card.accent());
	}

	@Test
	void buttonRectStaysInsideTheCard() {
		ClassCardRenderer.ButtonRect bounds = ClassCardRenderer.buttonRect(10, 20);

		assertEquals(10 + ClassCardRenderer.PAD, bounds.x());
		assertEquals(10 + ClassCardRenderer.WIDTH - ClassCardRenderer.PAD, bounds.x() + bounds.w());
		assertEquals(20 + ClassCardRenderer.HEIGHT - ClassCardRenderer.PAD, bounds.y() + bounds.h());
		assertEquals(ClassCardRenderer.BUTTON_H, bounds.h());
	}
}
