package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sweep-set invariant every passive bar must keep: every tier writes the same modifier
 * ids as the top row, so the max-stack row can stand in for the whole bar in the switch
 * sweep. A tier that drops an id would otherwise leave that modifier on the player forever.
 */
class PassiveSweepTest {
	private static Set<String> ids(List<PassiveEffect.Modifier> modifiers) {
		Set<String> ids = new HashSet<>();
		for (PassiveEffect.Modifier modifier : modifiers) {
			assertTrue(ids.add(modifier.modifierId()), "duplicate id " + modifier.modifierId());
		}
		return ids;
	}

	@Test
	void everyTierWritesTheSameIdsAsTheTopRow() {
		for (ClassDefinition def : ClassRegistry.all()) {
			for (ConditionalPassive passive : def.passives()) {
				Set<String> top = ids(passive.barRows().apply(passive.maxStacks()));
				for (ConditionalPassive.Tier tier : passive.tiers()) {
					assertEquals(top, ids(passive.barRows().apply(tier.threshold())),
							def.displayName() + " " + passive.name() + " at " + tier.threshold());
				}
			}
		}
	}

	@Test
	void aClassSwitchSweepsEveryIdALowerTierCanWrite() {
		for (ClassDefinition def : ClassRegistry.all()) {
			Set<String> swept = ids(PassiveSweep.sweepFor(def, null));
			for (ConditionalPassive passive : def.passives()) {
				for (ConditionalPassive.Tier tier : passive.tiers()) {
					assertTrue(swept.containsAll(ids(passive.barRows().apply(tier.threshold()))),
							def.displayName() + " " + passive.name() + " at " + tier.threshold());
				}
			}
		}
	}
}
