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
	void juggernautIsKnownAndPlayable() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		assertNotNull(juggernaut);
		assertEquals("Juggernaut", juggernaut.displayName());
	}

	@Test
	void onlyPlayableClassesAreRegistered() {
		assertEquals(3, ClassRegistry.all().size());
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "ranger")));
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "mage")));
		assertNull(ClassRegistry.byId(Identifier.fromNamespaceAndPath("infusedclasses", "bard")));
	}

	@Test
	void gamblerIsKnownAndCarriesItsDice() {
		ClassDefinition gambler = ClassRegistry.byId(ClassRegistry.GAMBLER);

		assertNotNull(gambler);
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
		assertTrue(momentum.earnsOn(Trigger.ON_HIT_DEALT));
		assertTrue(momentum.earnsOn(Trigger.ON_HIT_TAKEN));
		assertEquals(1, momentum.amountFor(Trigger.ON_HIT_DEALT));
		assertEquals(1, momentum.amountFor(Trigger.ON_HIT_TAKEN));
		assertEquals(Momentum.MAX_STACKS, momentum.maxStacks());
		assertEquals(Momentum.GRACE_SECONDS, momentum.graceSeconds());
		assertFalse(momentum.fullDesc().isBlank());
	}

	@Test
	void juggernautCarriesTheBurdenAlwaysOnChassis() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		assertEquals(1, juggernaut.alwaysOn().size());
		assertEquals("Burden", juggernaut.alwaysOn().get(0).name());
		assertEquals(List.of("infusedclasses:burden_damage", "infusedclasses:burden_swing"),
				juggernaut.alwaysOn().get(0).modifiers().stream()
						.map(PassiveEffect.Modifier::modifierId)
						.map(Identifier::toString)
						.toList());
		// Vigor is cut: the class carries no HP-scaled passive.
		assertTrue(juggernaut.hpPassives().isEmpty());
	}

	@Test
	void juggernautPassiveCopySaysWhatItDoesToday() {
		ConditionalPassive momentum = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives().get(0);

		assertEquals("Land a hit: +1. Take a hit: +1. Max 60. 5s no feed: -1/s. "
						+ "15 Steady: +1 armor, -2% speed, +0.25 knockback resistance. "
						+ "30 Braced: +3 armor, -4% speed, +0.5 knockback resistance. "
						+ "45 Rooted: +5 armor, -7% speed, +1 knockback resistance.",
				momentum.fullDesc());
	}

	@Test
	void passiveCopyIsGeneratedFromItsOwnNumbersAndItsTriggersOwnWords() {
		ConditionalPassive swift = new ConditionalPassive("Swift",
				List.of(new ConditionalPassive.Earn(new Trigger("on_land", "Land a swing"), 1)), 4,
				List.of(new ConditionalPassive.Tier(2, "Quick", 0.5, -0.001, 0.25)), 3);

		// The words ride the earns: an on_land passive is described by its own, never as a hit.
		assertEquals("Land a swing: +1. Max 4. 3s no feed: -1/s. "
						+ "2 Quick: +0.5 armor, -1% speed, +0.25 knockback resistance.",
				swift.fullDesc());
		assertFalse(swift.fullDesc().contains("hit"));
	}

	@Test
	void playerClassIdRoundTripsThroughString() {
		PlayerClasses classes = new PlayerClasses();

		assertNull(classes.writeId());
		classes.readId("infusedclasses:juggernaut");
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
			for (ClassDefinition.FlipBuff f : def.flipBuffs()) {
				assertTrue(f.desc().length() <= 200,
						def.id() + " flip \"" + f.name() + "\" must stay short (" + f.desc().length() + " chars)");
			}
		}
	}

	@Test
	void hasAbilityReadsTheRowsAndIsNullSafeOnTheId() {
		ClassDefinition juggernaut = ClassRegistry.byId(ClassRegistry.JUGGERNAUT);

		assertTrue(juggernaut.hasAbility(ClassRegistry.OFFLOAD_ABILITY));
		assertTrue(juggernaut.hasAbility(ClassRegistry.ONSLAUGHT_ABILITY));
		assertFalse(juggernaut.hasAbility(ClassRegistry.ROTATE_ABILITY));
		assertFalse(juggernaut.hasAbility(null));
	}
}
