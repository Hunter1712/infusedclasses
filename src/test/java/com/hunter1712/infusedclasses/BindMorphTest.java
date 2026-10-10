package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bind morph decisions: what a reused bind means, without players or packets.
 * The entry wires, this decides; a third class reusing R/G adds rows, not branches.
 */
class BindMorphTest {
	@Test
	void rIsRotateForLegionariesRushForEveryoneElse() {
		assertTrue(BindMorph.usesRotate(ClassRegistry.byId(ClassRegistry.LEGIONARY)));
		assertFalse(BindMorph.usesRotate(ClassRegistry.byId(ClassRegistry.JUGGERNAUT)));
		assertFalse(BindMorph.usesRotate(null));
	}

	@Test
	void gIsUltimateForLegionariesRendForEveryoneElse() {
		ClassDefinition legionary = ClassRegistry.byId(ClassRegistry.LEGIONARY);

		assertEquals(BindMorph.OathTarget.AEQUILIBRIUM,
				BindMorph.oathFor(legionary));
		assertEquals(BindMorph.OathTarget.REND,
				BindMorph.oathFor(ClassRegistry.byId(ClassRegistry.JUGGERNAUT)));
		assertEquals(BindMorph.OathTarget.REND, BindMorph.oathFor(null));
	}

	@Test
	void oathKeysOnRotateNotOnOathIds() {
		ClassDefinition rotateOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				java.util.List.of(), java.util.List.of(), 0.0, java.util.List.of(ClassRegistry.ROTATE_ABILITY), 0, "", java.util.List.of(), java.util.List.of());
		ClassDefinition impetusWithoutRotate = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				java.util.List.of(), java.util.List.of(), 0.0, java.util.List.of(ClassRegistry.IMPETUS_ABILITY), 0, "", java.util.List.of(), java.util.List.of());

		assertEquals(BindMorph.OathTarget.AEQUILIBRIUM,
				BindMorph.oathFor(rotateOnly));
		assertEquals(BindMorph.OathTarget.REND,
				BindMorph.oathFor(impetusWithoutRotate));
	}
}
