package com.hunter1712.infusedclasses;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
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
	void warriorCarriesNoHealthBonusWhileLockedCarriesNone() {
		assertEquals(0.0, ClassRegistry.byId(ClassRegistry.WARRIOR).maxHealthBonus());

		for (ClassDefinition def : ClassRegistry.all()) {
			if (def.locked()) {
				assertEquals(0.0, def.maxHealthBonus(), def.id() + " locked must carry no health bonus");
			}
		}
	}

	@Test
	void warriorCarriesBulwarkConditionalPassive() {
		List<ConditionalPassive> passives = ClassRegistry.byId(ClassRegistry.WARRIOR).passives();

		assertEquals(1, passives.size());
		ConditionalPassive bulwark = passives.get(0);
		assertEquals("Bulwark", bulwark.name());
		assertEquals("on_hit_taken", bulwark.trigger());
		assertEquals(20, bulwark.maxStacks());
		assertFalse(bulwark.fullDesc().isBlank());
	}

	@Test
	void warriorPassiveCopySaysWhatItDoesToday() {
		ConditionalPassive bulwark = ClassRegistry.byId(ClassRegistry.WARRIOR).passives().get(0);

		assertEquals("Taking a hit grants 1 stack (max 20). Each stack adds +0.5 armor. "
				+ "After 5s without a hit, lose all stacks.", bulwark.fullDesc());
	}

	@Test
	void passiveCopyIsGeneratedFromItsOwnNumbers() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4,
				ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("movement_speed")),
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_speed"),
				0.5, 3);

		// An on_land passive is not described as a hit, and the drop reads as instant.
		assertEquals("Each trigger grants 1 stack (max 4). Each stack adds +0.5 movement speed. "
				+ "After 3s without a trigger, lose all stacks.", swift.fullDesc());
		assertFalse(swift.fullDesc().contains("hit"));
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

