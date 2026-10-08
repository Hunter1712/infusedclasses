package com.hunter1712.infusedclasses;

import com.hunter1712.infusedclasses.client.ClassCardRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardDeepeningTest {
	@Test
	void cardForAgreesWithItsParts() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		ClassPresentation.CardModel plain = ClassPresentation.cardFor(juggernaut, false);

		assertEquals(ClassPresentation.cardLinesFor(juggernaut).size(), plain.text().lines().size());
		assertEquals(ClassPresentation.cardStyleFor(juggernaut, false), plain.style());
		assertEquals(ClassPresentation.accentFor(juggernaut), plain.text().accent());
		assertEquals(ClassPresentation.buttonFor(juggernaut), plain.text().button());
		assertEquals(ClassPresentation.tooltipFor(juggernaut), plain.text().tooltip());
		assertEquals(juggernaut.displayName(), plain.text().displayName());
		assertEquals(juggernaut.tagline(), plain.text().tagline());
	}

	@Test
	void lockedCardCarriesLockedLookInOnePlace() {
		ClassDefinition ranger = ClassRegistry.byId(net.minecraft.resources.Identifier
				.fromNamespaceAndPath("infusedclasses", "ranger"));

		ClassPresentation.CardModel card = ClassPresentation.cardFor(ranger, false);

		assertEquals("Locked", card.text().button().label());
		assertEquals("Coming soon", card.text().tooltip());
		assertEquals(0xFF555555, card.text().accent());
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
