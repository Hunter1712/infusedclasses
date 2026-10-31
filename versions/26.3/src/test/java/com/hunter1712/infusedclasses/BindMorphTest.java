package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Bind morph decisions: what a reused slot bind means, without players or packets.
 * The walk picks roster rows by their own slot; a further class reusing either slot adds
 * actives, not branches.
 */
class BindMorphTest {
	private static ClassDefinition defWith(ClassDefinition.ActiveAbility... actives) {
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				List.of(), List.of(), 0, "", List.of(actives), List.of(), CritRule.universal());
	}

	private static ClassDefinition.ActiveAbility active(Identifier ability, String name) {
		return new ClassDefinition.ActiveAbility(ability, name, "test copy");
	}

	private static AbilityRoster.Row signature(ClassDefinition def) {
		return BindMorph.slotFor(def, AbilityRoster.Slot.SIGNATURE);
	}

	private static AbilityRoster.Row ultimate(ClassDefinition def) {
		return BindMorph.slotFor(def, AbilityRoster.Slot.ULTIMATE);
	}

	@Test
	void signatureSlotIsWheelForGamblersRotateForLegionariesOffloadForEveryoneElse() {
		assertEquals(AbilityRoster.ROULETTE, signature(ClassRegistry.byId(ClassRegistry.GAMBLER)));
		assertEquals(AbilityRoster.ROTATE, signature(ClassRegistry.byId(ClassRegistry.LEGIONARY)));
		assertEquals(AbilityRoster.OFFLOAD, signature(ClassRegistry.byId(ClassRegistry.JUGGERNAUT)));
		assertEquals(AbilityRoster.OFFLOAD, signature(null));
	}

	@Test
	void ultimateSlotIsAllInForGamblersAequilibriumForLegionariesOnslaughtForEveryoneElse() {
		assertEquals(AbilityRoster.ALL_IN, ultimate(ClassRegistry.byId(ClassRegistry.GAMBLER)));
		assertEquals(AbilityRoster.AEQUILIBRIUM, ultimate(ClassRegistry.byId(ClassRegistry.LEGIONARY)));
		assertEquals(AbilityRoster.ONSLAUGHT, ultimate(ClassRegistry.byId(ClassRegistry.JUGGERNAUT)));
		assertEquals(AbilityRoster.ONSLAUGHT, ultimate(null));
	}

	@Test
	void morphKeysOnTheRowsSlotNotOnClassNames() {
		ClassDefinition rotateOnly = defWith(active(ClassRegistry.ROTATE_ABILITY, "Rotate"));
		ClassDefinition otherWithoutRotate = defWith(active(ClassRegistry.AEQUILIBRIUM_ABILITY, "Aequilibrium"));
		ClassDefinition allInOnly = defWith(active(ClassRegistry.ALL_IN_ABILITY, "All In"));

		assertEquals(AbilityRoster.ROTATE, signature(rotateOnly));
		assertEquals(AbilityRoster.OFFLOAD, signature(otherWithoutRotate));
		// The ultimate is picked by the class's own ultimate ability, never lent from the
		// signature row: Aequilibrium without Rotate now fires Aequilibrium, and Rotate
		// alone only falls back to Onslaught — which its definition cannot fire (ADR-0026).
		assertEquals(AbilityRoster.ONSLAUGHT, ultimate(rotateOnly));
		assertEquals(AbilityRoster.AEQUILIBRIUM, ultimate(otherWithoutRotate));
		assertEquals(AbilityRoster.ALL_IN, ultimate(allInOnly));
		assertEquals(AbilityRoster.OFFLOAD, signature(allInOnly));
		// The drawn rows follow the same walk: what the class carries is what it draws.
		assertEquals(AbilityRoster.ROTATE, BindMorph.shape(rotateOnly).signatureRow());
		assertNull(BindMorph.shape(rotateOnly).ultimateRow());
		assertEquals(AbilityRoster.AEQUILIBRIUM, BindMorph.shape(otherWithoutRotate).ultimateRow());
		assertNull(BindMorph.shape(otherWithoutRotate).signatureRow());
	}

	/** Two slot-shaped actives: the class's own active order decides, the same answer the bind fires. */
	@Test
	void theFirstActiveNamingASlotWins() {
		ClassDefinition offloadThenRotate = defWith(
				active(ClassRegistry.OFFLOAD_ABILITY, "Offload"),
				active(ClassRegistry.ROTATE_ABILITY, "Rotate"));
		ClassDefinition rotateThenOffload = defWith(
				active(ClassRegistry.ROTATE_ABILITY, "Rotate"),
				active(ClassRegistry.OFFLOAD_ABILITY, "Offload"));
		ClassDefinition allInThenOnslaught = defWith(
				active(ClassRegistry.ALL_IN_ABILITY, "All In"),
				active(ClassRegistry.ONSLAUGHT_ABILITY, "Onslaught"));

		assertEquals(AbilityRoster.OFFLOAD, signature(offloadThenRotate));
		assertEquals(AbilityRoster.OFFLOAD, BindMorph.shape(offloadThenRotate).signatureRow());
		// Order is the class's own actives, not the roster's.
		assertEquals(AbilityRoster.ROTATE, signature(rotateThenOffload));
		assertEquals(AbilityRoster.ROTATE, BindMorph.shape(rotateThenOffload).signatureRow());
		assertEquals(AbilityRoster.ALL_IN, ultimate(allInThenOnslaught));
		assertEquals(AbilityRoster.ALL_IN, BindMorph.shape(allInThenOnslaught).ultimateRow());
	}
}
