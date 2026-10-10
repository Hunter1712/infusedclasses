package com.hunter1712.infusedclasses;

import com.hunter1712.infusedclasses.client.CardLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardDeepeningTest {
	@Test
	void cardForResolvesTheWholeCard() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		ClassPresentation.CardModel plain = ClassPresentation.cardFor(juggernaut, false);

		assertEquals(ClassPresentation.cardLinesFor(juggernaut).size(), plain.text().lines().size());
		assertEquals(new ClassPresentation.CardStyle(0xFF3A3A3A, null, 0xFFFFFFFF), plain.style());
		assertEquals(juggernaut.accentColor(), plain.text().accent());
		assertEquals(new ClassPresentation.ButtonData("Select", true), plain.text().button());
		assertEquals(juggernaut.displayName(), plain.text().displayName());
		assertEquals(juggernaut.tagline(), plain.text().tagline());
	}

	@Test
	void selectRectStaysInsideTheFocusedCard() {
		CardLayout.CardRect card = CardLayout.focusedCard(854, 480);
		CardLayout.ButtonRect bounds = CardLayout.selectRect(card);

		assertEquals(card.x() + CardLayout.PAD, bounds.x());
		assertEquals(card.x() + card.w() - CardLayout.PAD, bounds.x() + bounds.w());
		assertEquals(card.y() + card.h() - CardLayout.PAD, bounds.y() + bounds.h());
		assertEquals(CardLayout.BUTTON_H, bounds.h());
	}
}
