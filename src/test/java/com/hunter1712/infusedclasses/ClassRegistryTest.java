package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void warriorCarriesBulwarkConditionalPassive() {
		List<ConditionalPassive> passives = ClassRegistry.byId(ClassRegistry.WARRIOR).passives();

		assertEquals(1, passives.size());
		ConditionalPassive bulwark = passives.get(0);
		assertEquals("Bulwark", bulwark.name());
		assertEquals("on_hit_taken", bulwark.trigger());
		assertEquals(10, bulwark.maxStacks());
		assertFalse(bulwark.shortDesc().isBlank());
		assertFalse(bulwark.fullDesc().isBlank());
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

