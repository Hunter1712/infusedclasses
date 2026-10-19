package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The class-switch cleanup as a decision: which modifiers a switch takes off, since the new
 * class's tick never visits the old class's kit. The wiring only removes what this answers.
 */
class ClassSwitchCleanupTest {
	private static ClassDefinition juggernaut() {
		return ClassRegistry.byId(ClassRegistry.JUGGERNAUT);
	}

	private static ClassDefinition legionary() {
		return ClassRegistry.byId(ClassRegistry.LEGIONARY);
	}

	private static ClassDefinition gambler() {
		return ClassRegistry.byId(ClassRegistry.GAMBLER);
	}

	private static List<String> ids(ClassDefinition oldDef, ClassDefinition newDef) {
		return PassiveApplier.cleanupFor(oldDef, newDef).stream()
				.map(PassiveEffect.Modifier::modifierId)
				.map(Identifier::toString)
				.map(id -> id.substring(id.indexOf(':') + 1))
				.toList();
	}

	@Test
	void juggernautToLegionaryTakesOffItsOwnKit() {
		// Momentum's armor and weight, its knockback row, Burden's pair, and the
		// Offload window's pair: the new class writes none of them.
		assertEquals(List.of("momentum_armor", "momentum_weight", "momentum_knockback",
				"burden_damage", "burden_swing", "offload_haste", "offload_speed"),
				ids(juggernaut(), legionary()));
	}

	@Test
	void legionaryToGamblerTakesOffTheStanceAurasAndTheOathWindowPair() {
		assertEquals(List.of("gladius_damage", "gladius_alacrity", "scutum_plate", "scutum_toughness",
				"scutum_weight", "aequilibrium_edge", "aequilibrium_plate"),
				ids(legionary(), gambler()));
	}

	@Test
	void gamblerToLegionaryTakesOffTheWheelPair() {
		assertEquals(List.of("roulette_ward", "roulette_haste"), ids(gambler(), legionary()));
	}

	@Test
	void sameClassTakesOffNothing() {
		assertEquals(List.of(), ids(juggernaut(), juggernaut()));
		assertEquals(List.of(), ids(null, null));
	}

	@Test
	void classlessEndsTakeOffWhatTheTickWouldHaveSwept() {
		// null -> class: nothing was written, nothing to remove. This is the guard the
		// old mirror carried by hand.
		assertEquals(List.of(), ids(null, juggernaut()));

		// class -> null: no tick ever runs, so the whole kit comes off here.
		assertEquals(List.of("momentum_armor", "momentum_weight", "momentum_knockback",
				"burden_damage", "burden_swing", "offload_haste", "offload_speed"),
				ids(juggernaut(), null));
		assertEquals(List.of("gladius_damage", "gladius_alacrity", "scutum_plate", "scutum_toughness",
				"scutum_weight", "aequilibrium_edge", "aequilibrium_plate"),
				ids(legionary(), null));
	}

	@Test
	void everyRemovedEntryNamesAnAttributeAndIsListedOnce() {
		for (ClassDefinition oldDef : ClassRegistry.all()) {
			for (ClassDefinition newDef : ClassRegistry.all()) {
				List<PassiveEffect.Modifier> stale = PassiveApplier.cleanupFor(oldDef, newDef);
				for (PassiveEffect.Modifier modifier : stale) {
					org.junit.jupiter.api.Assertions.assertNotNull(modifier.attribute(), modifier.modifierId().toString());
				}
				assertEquals(stale.size(),
						stale.stream().map(PassiveEffect.Modifier::modifierId).distinct().count(),
						oldDef.displayName() + " -> " + newDef.displayName());
			}
		}
	}
}
