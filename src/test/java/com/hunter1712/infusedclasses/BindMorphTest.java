package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bind morph decisions: what a reused slot bind means, without players or packets.
 * The entry wires, this decides; a further class reusing either slot adds rows, not branches.
 */
class BindMorphTest {
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
		ClassDefinition rotateOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				java.util.List.of(), java.util.List.of(), 0, "",
				java.util.List.of(new ClassDefinition.ActiveAbility(ClassRegistry.ROTATE_ABILITY, "Rotate", "test copy")),
				java.util.List.of(),
				CritRule.universal());
		ClassDefinition otherWithoutRotate = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				java.util.List.of(), java.util.List.of(), 0, "",
				java.util.List.of(new ClassDefinition.ActiveAbility(ClassRegistry.AEQUILIBRIUM_ABILITY, "Aequilibrium", "test copy")),
				java.util.List.of(),
				CritRule.universal());
		ClassDefinition allInOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test",
				java.util.List.of(), java.util.List.of(), 0, "",
				java.util.List.of(new ClassDefinition.ActiveAbility(ClassRegistry.ALL_IN_ABILITY, "All In", "test copy")),
				java.util.List.of(),
				CritRule.universal());

		assertEquals(BindMorph.SignatureTarget.ROTATE, BindMorph.signatureFor(rotateOnly));
		assertEquals(BindMorph.SignatureTarget.RUSH, BindMorph.signatureFor(otherWithoutRotate));
		assertEquals(BindMorph.UltimateTarget.AEQUILIBRIUM, BindMorph.ultimateFor(rotateOnly));
		assertEquals(BindMorph.UltimateTarget.REND, BindMorph.ultimateFor(otherWithoutRotate));
		assertEquals(BindMorph.UltimateTarget.ALL_IN, BindMorph.ultimateFor(allInOnly));
		assertEquals(BindMorph.SignatureTarget.RUSH, BindMorph.signatureFor(allInOnly));
	}
}
