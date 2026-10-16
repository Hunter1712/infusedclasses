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

		assertEquals(3, ClassRegistry.all().size());
		assertEquals(0, locked);
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "ranger")));
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "mage")));
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "bard")));
	}

	@Test
	void gamblerIsKnownAndCarriesItsDice() {
		ClassDefinition gambler = ClassRegistry.byId(ClassRegistry.GAMBLER);

		assertNotNull(gambler);
		assertFalse(gambler.locked());
		assertEquals("Gambler", gambler.displayName());
		assertEquals("Double or nothing", gambler.tagline());
		assertEquals(0xFFD4AF37, gambler.accentColor());
		assertTrue(gambler.hasAbility(ClassRegistry.ROULETTE_ABILITY));
		assertTrue(gambler.hasAbility(ClassRegistry.ALL_IN_ABILITY));
		assertTrue(gambler.critRule().hasDice());
	}

	@Test
	void wholeRosterCarriesACritRule() {
		for (ClassDefinition def : ClassRegistry.all()) {
			assertNotNull(def.critRule(), def.id() + " must carry a crit rule");
		}
	}

	@Test
	void juggernautCarriesMomentumConditionalPassive() {
		List<ConditionalPassive> passives = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives();

		assertEquals(1, passives.size());
		ConditionalPassive momentum = passives.get(0);
		assertEquals("Momentum", momentum.name());
		assertEquals("on_hit_dealt", momentum.trigger());
		assertEquals(20, momentum.maxStacks());
		assertFalse(momentum.fullDesc().isBlank());
	}

	@Test
	void juggernautPassiveCopySaysWhatItDoesToday() {
		ConditionalPassive momentum = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives().get(0);

		assertEquals("Land a hit: +0.4 armor (max 20). 5s no hit: lose all.", momentum.fullDesc());
	}

	@Test
	void passiveCopyIsGeneratedFromItsOwnNumbers() {
		ConditionalPassive swift = new ConditionalPassive("Swift", "on_land", 4,
				ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("movement_speed")),
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_speed"),
				0.5, 3);

		// An on_land passive is not described as a hit.
		assertEquals("Trigger: +0.5 movement speed (max 4). 3s no trigger: lose all.", swift.fullDesc());
		assertFalse(swift.fullDesc().contains("hit"));
	}

	@Test
	void hitTakenCopyStillReadsAsAHit() {
		ConditionalPassive guard = new ConditionalPassive("Guard", ConditionalPassive.ON_HIT_TAKEN, 20,
				ClassRegistry.ARMOR, Identifier.fromNamespaceAndPath("infusedclasses", "guard_armor"), 0.5, 5);

		assertEquals("Take a hit: +0.5 armor (max 20). 5s no hit: lose all.", guard.fullDesc());
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
	void everyActiveDescStaysShort() {
		for (ClassDefinition def : ClassRegistry.all()) {
			for (ClassDefinition.ActiveAbility a : def.actives()) {
				assertTrue(a.desc().length() <= 200,
						def.id() + " active \"" + a.name() + "\" must stay short (" + a.desc().length() + " chars)");
			}
			for (ClassDefinition.ActiveAbility f : def.flipBuffs()) {
				assertTrue(f.desc().length() <= 200,
						def.id() + " flip \"" + f.name() + "\" must stay short (" + f.desc().length() + " chars)");
			}
		}
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
