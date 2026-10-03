package dev.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClassRegistryTest {
	@Test
	void warriorIsKnownAndUnlocked() {
		ClassDefinition warrior = ClassRegistry.byId(ClassRegistry.WARRIOR);

		assertNotNull(warrior);
		assertFalse(warrior.locked());
		assertEquals("Warrior", warrior.displayName());
	}

	@Test
	void plannedClassesArePresentAndLocked() {
		long locked = ClassRegistry.all().stream().filter(ClassDefinition::locked).count();

		assertEquals(3, ClassRegistry.all().size());
		assertEquals(2, locked);
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "bard")));
	}

	@ParameterizedTest
	@CsvSource({
			"max_health, 8.0, ADD_VALUE",
			"armor, 4.0, ADD_VALUE",
			"armor_toughness, 2.0, ADD_VALUE",
			"movement_speed, -0.1, ADD_MULTIPLIED_TOTAL",
	})
	void warriorPassiveMathMatchesSpec(String attribute, double value, AttributeModifier.Operation operation) {
		Identifier id = Identifier.fromNamespaceAndPath("minecraft", attribute);
		Passive match = ClassRegistry.byId(ClassRegistry.WARRIOR).passives().stream()
				.filter(p -> p.attribute().equals(id))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no passive for " + id));

		assertEquals(value, match.value());
		assertEquals(operation, match.operation());
		assertEquals("infusedclasses", match.modifier().getNamespace());
	}

	@Test
	void warriorHasNoFlatDamageBonus() {
		Identifier damage = Identifier.fromNamespaceAndPath("minecraft", "attack_damage");

		assertFalse(ClassRegistry.byId(ClassRegistry.WARRIOR).passives().stream()
				.anyMatch(p -> p.attribute().equals(damage)));
	}

	@Test
	void playerClassIdRoundTripsThroughString() {
		PlayerClasses classes = new PlayerClasses();

		assertNull(classes.writeId());
		classes.readId("infusedclasses:warrior");
		assertEquals(ClassRegistry.WARRIOR, classes.classId());
		assertEquals("infusedclasses:warrior", classes.writeId());

		classes.readId("infusedclasses:bard");
		assertNull(classes.classId());
		classes.readId("not an id");
		assertNull(classes.classId());
		classes.readId(null);
		assertNull(classes.classId());
	}
}
