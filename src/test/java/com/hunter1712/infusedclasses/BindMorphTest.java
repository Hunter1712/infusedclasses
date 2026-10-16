package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bind morph decisions: what a reused bind means, without players or packets.
 * The entry wires, this decides; a further class reusing R/G adds rows, not branches.
 */
class BindMorphTest {
	@Test
	void rIsWheelForGamblersRotateForLegionariesRushForEveryoneElse() {
		assertEquals(BindMorph.RushTarget.ROULETTE,
				BindMorph.rushFor(ClassRegistry.byId(ClassRegistry.GAMBLER)));
		assertEquals(BindMorph.RushTarget.ROTATE,
				BindMorph.rushFor(ClassRegistry.byId(ClassRegistry.LEGIONARY)));
		assertEquals(BindMorph.RushTarget.RUSH,
				BindMorph.rushFor(ClassRegistry.byId(ClassRegistry.JUGGERNAUT)));
		assertEquals(BindMorph.RushTarget.RUSH, BindMorph.rushFor(null));
	}

	@Test
	void gIsAllInForGamblersUltimateForLegionariesRendForEveryoneElse() {
		assertEquals(BindMorph.OathTarget.ALL_IN,
				BindMorph.oathFor(ClassRegistry.byId(ClassRegistry.GAMBLER)));
		assertEquals(BindMorph.OathTarget.AEQUILIBRIUM,
				BindMorph.oathFor(ClassRegistry.byId(ClassRegistry.LEGIONARY)));
		assertEquals(BindMorph.OathTarget.REND,
				BindMorph.oathFor(ClassRegistry.byId(ClassRegistry.JUGGERNAUT)));
		assertEquals(BindMorph.OathTarget.REND, BindMorph.oathFor(null));
	}

	@Test
	void morphKeysOnAbilityIdsNotOnClassNames() {
		ClassDefinition rotateOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				java.util.List.of(), java.util.List.of(), java.util.List.of(ClassRegistry.ROTATE_ABILITY), 0, "", java.util.List.of(), java.util.List.of(),
				CritRule.universal());
		ClassDefinition otherWithoutRotate = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				java.util.List.of(), java.util.List.of(), java.util.List.of(ClassRegistry.AEQUILIBRIUM_ABILITY), 0, "", java.util.List.of(), java.util.List.of(),
				CritRule.universal());
		ClassDefinition allInOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				java.util.List.of(), java.util.List.of(), java.util.List.of(ClassRegistry.ALL_IN_ABILITY), 0, "", java.util.List.of(), java.util.List.of(),
				CritRule.universal());

		assertEquals(BindMorph.RushTarget.ROTATE, BindMorph.rushFor(rotateOnly));
		assertEquals(BindMorph.RushTarget.RUSH, BindMorph.rushFor(otherWithoutRotate));
		assertEquals(BindMorph.OathTarget.AEQUILIBRIUM, BindMorph.oathFor(rotateOnly));
		assertEquals(BindMorph.OathTarget.REND, BindMorph.oathFor(otherWithoutRotate));
		assertEquals(BindMorph.OathTarget.ALL_IN, BindMorph.oathFor(allInOnly));
		assertEquals(BindMorph.RushTarget.RUSH, BindMorph.rushFor(allInOnly));
	}
}
