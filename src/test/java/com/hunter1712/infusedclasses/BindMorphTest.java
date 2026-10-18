package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Bind morph decisions: what a reused slot bind means, without players or packets.
 * The entry wires, this decides; a further class reusing either slot adds rows, not branches.
 */
class BindMorphTest {
	private static ClassDefinition defWith(ClassDefinition.ActiveAbility... actives) {
		return new ClassDefinition(Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				List.of(), List.of(), 0, "", List.of(actives), List.of(), CritRule.universal());
	}

	private static ClassDefinition.ActiveAbility active(Identifier ability, String name) {
		return new ClassDefinition.ActiveAbility(ability, name, "test copy");
	}

	@Test
	void signatureIsWheelForGamblersRotateForLegionariesRushForEveryoneElse() {
		assertEquals(BindMorph.SignatureTarget.ROULETTE,
				BindMorph.signatureFor(ClassRegistry.byId(ClassRegistry.GAMBLER)));
		assertEquals(BindMorph.SignatureTarget.ROTATE,
				BindMorph.signatureFor(ClassRegistry.byId(ClassRegistry.LEGIONARY)));
		assertEquals(BindMorph.SignatureTarget.RUSH,
				BindMorph.signatureFor(ClassRegistry.byId(ClassRegistry.JUGGERNAUT)));
		assertEquals(BindMorph.SignatureTarget.RUSH, BindMorph.signatureFor(null));
	}

	@Test
	void ultimateIsAllInForGamblersAequilibriumForLegionariesRendForEveryoneElse() {
		assertEquals(BindMorph.UltimateTarget.ALL_IN,
				BindMorph.ultimateFor(ClassRegistry.byId(ClassRegistry.GAMBLER)));
		assertEquals(BindMorph.UltimateTarget.AEQUILIBRIUM,
				BindMorph.ultimateFor(ClassRegistry.byId(ClassRegistry.LEGIONARY)));
		assertEquals(BindMorph.UltimateTarget.REND,
				BindMorph.ultimateFor(ClassRegistry.byId(ClassRegistry.JUGGERNAUT)));
		assertEquals(BindMorph.UltimateTarget.REND, BindMorph.ultimateFor(null));
	}

	@Test
	void morphKeysOnAbilityIdsNotOnClassNames() {
		ClassDefinition rotateOnly = defWith(active(ClassRegistry.ROTATE_ABILITY, "Rotate"));
		ClassDefinition otherWithoutRotate = defWith(active(ClassRegistry.AEQUILIBRIUM_ABILITY, "Aequilibrium"));
		ClassDefinition allInOnly = defWith(active(ClassRegistry.ALL_IN_ABILITY, "All In"));

		assertEquals(BindMorph.SignatureTarget.ROTATE, BindMorph.signatureFor(rotateOnly));
		assertEquals(BindMorph.SignatureTarget.RUSH, BindMorph.signatureFor(otherWithoutRotate));
		// The ultimate is picked by the class's own ultimate ability, never lent from the
		// signature row: Aequilibrium without Rotate now fires Aequilibrium, and Rotate
		// alone only falls back to Rend — which its definition cannot fire (ADR-0026).
		assertEquals(BindMorph.UltimateTarget.REND, BindMorph.ultimateFor(rotateOnly));
		assertEquals(BindMorph.UltimateTarget.AEQUILIBRIUM, BindMorph.ultimateFor(otherWithoutRotate));
		assertEquals(BindMorph.UltimateTarget.ALL_IN, BindMorph.ultimateFor(allInOnly));
		assertEquals(BindMorph.SignatureTarget.RUSH, BindMorph.signatureFor(allInOnly));
		// The drawn rows follow the same walk: what the class carries is what it draws.
		assertEquals(AbilityRoster.ROTATE, BindMorph.shape(rotateOnly).signatureRow());
		assertNull(BindMorph.shape(rotateOnly).ultimateRow());
		assertEquals(AbilityRoster.AEQUILIBRIUM, BindMorph.shape(otherWithoutRotate).ultimateRow());
		assertNull(BindMorph.shape(otherWithoutRotate).signatureRow());
	}

	/** Two slot-shaped actives: the class's own active order decides, the same answer the bind fires. */
	@Test
	void theFirstActiveNamingASlotWins() {
		ClassDefinition rushThenRotate = defWith(
				active(ClassRegistry.RUSH_ABILITY, "Rush"),
				active(ClassRegistry.ROTATE_ABILITY, "Rotate"));
		ClassDefinition rotateThenRush = defWith(
				active(ClassRegistry.ROTATE_ABILITY, "Rotate"),
				active(ClassRegistry.RUSH_ABILITY, "Rush"));
		ClassDefinition allInThenRend = defWith(
				active(ClassRegistry.ALL_IN_ABILITY, "All In"),
				active(ClassRegistry.REND_ABILITY, "Rend"));

		assertEquals(BindMorph.SignatureTarget.RUSH, BindMorph.signatureFor(rushThenRotate));
		assertEquals(AbilityRoster.RUSH, BindMorph.shape(rushThenRotate).signatureRow());
		// Order is the class's own actives, not the roster's and not the enum's.
		assertEquals(BindMorph.SignatureTarget.ROTATE, BindMorph.signatureFor(rotateThenRush));
		assertEquals(AbilityRoster.ROTATE, BindMorph.shape(rotateThenRush).signatureRow());
		assertEquals(BindMorph.UltimateTarget.ALL_IN, BindMorph.ultimateFor(allInThenRend));
		assertEquals(AbilityRoster.ALL_IN, BindMorph.shape(allInThenRend).ultimateRow());
	}
}
