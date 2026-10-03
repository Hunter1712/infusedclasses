package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassAttributesTest {
	@Test
	void warriorAttributesAreKnown() {
		assertTrue(ClassAttributes.isKnown(Identifier.fromNamespaceAndPath("minecraft", "max_health")));
		assertTrue(ClassAttributes.isKnown(Identifier.fromNamespaceAndPath("minecraft", "armor")));
		assertTrue(ClassAttributes.isKnown(Identifier.fromNamespaceAndPath("minecraft", "armor_toughness")));
		assertTrue(ClassAttributes.isKnown(Identifier.fromNamespaceAndPath("minecraft", "movement_speed")));
	}

	@Test
	void unknownAttributesAreRejected() {
		assertFalse(ClassAttributes.isKnown(Identifier.fromNamespaceAndPath("minecraft", "luck")));
		assertFalse(ClassAttributes.isKnown(Identifier.fromNamespaceAndPath("infusedclasses", "max_health")));
		assertFalse(ClassAttributes.isKnown(null));
	}

	@ParameterizedTest
	@CsvSource({
			"max_health, Max Health",
			"armor, Armor",
			"armor_toughness, Toughness",
			"movement_speed, Move Speed",
	})
	void displayNamesMatchPicker(String path, String expected) {
		assertEquals(expected,
				ClassAttributes.displayName(Identifier.fromNamespaceAndPath("minecraft", path)));
	}

	@Test
	void flatPassiveFormatsWithSign() {
		Passive p = new Passive(
				Identifier.fromNamespaceAndPath("minecraft", "max_health"),
				Identifier.fromNamespaceAndPath("infusedclasses", "warrior_health"),
				8.0, AttributeModifier.Operation.ADD_VALUE);

		assertEquals("+8 Max Health", ClassAttributes.display(p));
	}

	@Test
	void percentPassiveFormatsAsPercent() {
		Passive p = new Passive(
				Identifier.fromNamespaceAndPath("minecraft", "movement_speed"),
				Identifier.fromNamespaceAndPath("infusedclasses", "warrior_speed"),
				-0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

		assertEquals("-10% Move Speed", ClassAttributes.display(p));
	}
}
