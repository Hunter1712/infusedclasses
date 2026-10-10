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
		assertEquals(juggernaut.displayName(), plain.text().displayName());
		assertEquals(juggernaut.tagline(), plain.text().tagline());
	}

	@Test
	void selectRectStaysInsideTheFocusedCard() {
		ClassCardRenderer.CardRect card = ClassCardRenderer.focusedCard(854, 480);
		ClassCardRenderer.ButtonRect bounds = ClassCardRenderer.selectRect(card);

		assertEquals(card.x() + ClassCardRenderer.PAD, bounds.x());
		assertEquals(card.x() + card.w() - ClassCardRenderer.PAD, bounds.x() + bounds.w());
		assertEquals(card.y() + card.h() - ClassCardRenderer.PAD, bounds.y() + bounds.h());
		assertEquals(ClassCardRenderer.BUTTON_H, bounds.h());
	}
}
