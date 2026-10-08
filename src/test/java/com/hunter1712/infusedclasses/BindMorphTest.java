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
	void gMorphsOnStanceForLegionariesRendForEveryoneElse() {
		ClassDefinition legionary = ClassRegistry.byId(ClassRegistry.LEGIONARY);

		assertEquals(BindMorph.OathTarget.IMPETUS,
				BindMorph.oathFor(legionary, RotateAbility.Stance.GLADIUS));
		assertEquals(BindMorph.OathTarget.TESTUDO,
				BindMorph.oathFor(legionary, RotateAbility.Stance.SCUTUM));
		assertEquals(BindMorph.OathTarget.REND,
				BindMorph.oathFor(ClassRegistry.byId(ClassRegistry.JUGGERNAUT), RotateAbility.Stance.GLADIUS));
		assertEquals(BindMorph.OathTarget.REND, BindMorph.oathFor(null, RotateAbility.Stance.SCUTUM));
	}

	@Test
	void oathMorphKeysOnRotateNotOnOathIds() {
		ClassDefinition rotateOnly = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				java.util.List.of(), java.util.List.of(), 0.0, java.util.List.of(ClassRegistry.ROTATE_ABILITY), 0, "", java.util.List.of());
		ClassDefinition impetusWithoutRotate = new ClassDefinition(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("infusedclasses", "test"), "Test", false,
				java.util.List.of(), java.util.List.of(), 0.0, java.util.List.of(ClassRegistry.IMPETUS_ABILITY), 0, "", java.util.List.of());

		assertEquals(BindMorph.OathTarget.IMPETUS,
				BindMorph.oathFor(rotateOnly, RotateAbility.Stance.GLADIUS));
		assertEquals(BindMorph.OathTarget.REND,
				BindMorph.oathFor(impetusWithoutRotate, RotateAbility.Stance.GLADIUS));
	}
}
