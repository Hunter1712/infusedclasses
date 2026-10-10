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
	void juggernautIsKnownAndUnlocked() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		assertNotNull(juggernaut);
		assertFalse(juggernaut.locked());
		assertEquals("Juggernaut", juggernaut.displayName());
	}

	@Test
	void onlyPlayableClassesAreRegistered() {
		long locked = ClassRegistry.all().stream().filter(ClassDefinition::locked).count();

		assertEquals(2, ClassRegistry.all().size());
		assertEquals(0, locked);
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "ranger")));
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "mage")));
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "bard")));
	}

	@Test
	void playableClassesCarryNoHealthBonus() {
		assertEquals(0.0, ClassRegistry.byId(ClassRegistry.JUGGERNAUT).maxHealthBonus());
		assertEquals(0.0, ClassRegistry.byId(ClassRegistry.LEGIONARY).maxHealthBonus());
	}

	@Test
	void juggernautCarriesMomentumConditionalPassive() {
		List<ConditionalPassive> passives = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives();

		assertEquals(1, passives.size());
		ConditionalPassive momentum = passives.get(0);
		assertEquals("Momentum", momentum.name());
		assertEquals("on_hit_dealt", momentum.trigger());
		assertEquals(10, momentum.maxStacks());
		assertFalse(momentum.fullDesc().isBlank());
	}

	@Test
	void juggernautPassiveCopySaysWhatItDoesToday() {
		ConditionalPassive momentum = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives().get(0);

		assertEquals("Landing a hit grants 1 stack (max 10). Each stack adds +1 armor. "
				+ "After 5s without landing a hit, lose all stacks.", momentum.fullDesc());
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
	void hitTakenCopyStillReadsAsAHit() {
		ConditionalPassive guard = new ConditionalPassive("Guard", ConditionalPassive.ON_HIT_TAKEN, 20,
				ClassRegistry.ARMOR, Identifier.fromNamespaceAndPath("infusedclasses", "guard_armor"), 0.5, 5);

		assertEquals("Taking a hit grants 1 stack (max 20). Each stack adds +0.5 armor. "
				+ "After 5s without a hit, lose all stacks.", guard.fullDesc());
	}

	@Test
	void playerClassIdRoundTripsThroughString() {
		PlayerClasses classes = new PlayerClasses();

		assertNull(classes.writeId());
		classes.readId("infusedclasses:juggernaut");
		assertEquals(ClassRegistry.JUGGERNAUT, classes.classId());
		assertEquals("infusedclasses:juggernaut", classes.writeId());

		classes.readId("infusedclasses:berserker");
		assertEquals(ClassRegistry.JUGGERNAUT, classes.classId());
		assertEquals("infusedclasses:juggernaut", classes.writeId());

		classes.readId("infusedclasses:warrior");
		assertEquals(ClassRegistry.JUGGERNAUT, classes.classId());
		assertEquals("infusedclasses:juggernaut", classes.writeId());

		classes.readId("infusedclasses:bard");
		assertNull(classes.classId());
		classes.readId("not an id");
		assertNull(classes.classId());
		classes.readId(null);
		assertNull(classes.classId());
	}

	@Test
	void hasAbilityReadsTheListAndIsNullSafeOnTheId() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		assertTrue(juggernaut.hasAbility(ClassRegistry.RUSH_ABILITY));
		assertTrue(juggernaut.hasAbility(ClassRegistry.REND_ABILITY));
		assertFalse(juggernaut.hasAbility(ClassRegistry.ROTATE_ABILITY));
		assertFalse(juggernaut.hasAbility(null));
	}
}
