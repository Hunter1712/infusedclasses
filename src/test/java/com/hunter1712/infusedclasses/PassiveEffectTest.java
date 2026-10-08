package com.hunter1712.infusedclasses;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stack-to-attribute mapping, asserted as a decision. Plain JUnit: no player, level,
 * attribute instance or graphics object is constructed to make these assertions.
 */
class PassiveEffectTest {
	private static final ConditionalPassive MOMENTUM = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).passives().get(0);

	@Test
	void noStacksMeansNoModifier() {
		assertNull(PassiveEffect.forStacks(MOMENTUM, 0));
	}

	@Test
	void negativeStacksMeanNoModifier() {
		assertNull(PassiveEffect.forStacks(MOMENTUM, -1));
	}

	@Test
	void missingPassiveMeansNoModifier() {
		assertNull(PassiveEffect.forStacks(null, 3));
	}

	@Test
	void oneStackGrantsThePassivesOwnAttributeModifier() {
		PassiveEffect.Modifier wanted = PassiveEffect.forStacks(MOMENTUM, 1);

		assertEquals(ClassRegistry.ARMOR, wanted.attribute());
		assertEquals(Identifier.fromNamespaceAndPath("infusedclasses", "momentum_armor"), wanted.modifierId());
		assertEquals(1.0, wanted.amount());
	}

	@Test
	void stacksScaleLinearlyToTheCap() {
		PassiveEffect.Modifier atCap = PassiveEffect.forStacks(MOMENTUM, MOMENTUM.maxStacks());

		assertEquals(10, atCap.amount());
	}

	@Test
	void theMappingFollowsThePassiveNotAParticularName() {
		ResourceKey<Attribute> speed = ResourceKey.create(Registries.ATTRIBUTE,
				Identifier.withDefaultNamespace("movement_speed"));
		ConditionalPassive swift = new ConditionalPassive(
				"Something Else",
				"on_land",
				4,
				speed,
				Identifier.fromNamespaceAndPath("infusedclasses", "swift_speed"),
				0.5,
				3);

		PassiveEffect.Modifier wanted = PassiveEffect.forStacks(swift, 3);

		assertEquals(speed, wanted.attribute());
		assertEquals(Identifier.fromNamespaceAndPath("infusedclasses", "swift_speed"), wanted.modifierId());
		assertEquals(1.5, wanted.amount());
	}

	@Test
	void stacksWriteTheirModifier() {
		assertTrue(PassiveEffect.planFor(MOMENTUM, 3, false) instanceof PassiveEffect.Plan.Write write
				&& write.wanted().amount() == 3.0);
	}

	@Test
	void deadStacksRemoveOnlyWhenTheyMoved() {
		assertTrue(PassiveEffect.planFor(MOMENTUM, 0, true) instanceof PassiveEffect.Plan.Remove);
		assertTrue(PassiveEffect.planFor(MOMENTUM, 0, false) instanceof PassiveEffect.Plan.Skip);
		assertTrue(PassiveEffect.planFor(null, 0, true) instanceof PassiveEffect.Plan.Skip);
	}

	@Test
	void bucketsWriteLiveAndRemoveDead() {
		HpScaledPassive vigor = ClassRegistry.byId(ClassRegistry.JUGGERNAUT).hpPassives().get(0);

		assertTrue(PassiveEffect.planForBucket(vigor, 10) instanceof PassiveEffect.Plan.Write write
				&& write.wanted().amount() == 3.0);
		assertTrue(PassiveEffect.planForBucket(vigor, 0)
				instanceof PassiveEffect.Plan.Remove remove
				&& remove.attribute().equals(vigor.attribute()));
		assertTrue(PassiveEffect.planForBucket(null, 0) instanceof PassiveEffect.Plan.Skip);
	}
}
