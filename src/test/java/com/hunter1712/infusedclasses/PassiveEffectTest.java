package com.hunter1712.infusedclasses;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The stack-to-attribute mapping, asserted as a decision. Plain JUnit: no player, level,
 * attribute instance or graphics object is constructed to make these assertions.
 */
class PassiveEffectTest {
	private static final ConditionalPassive BULWARK = ClassRegistry.byId(ClassRegistry.WARRIOR).passives().get(0);

	@Test
	void noStacksMeansNoModifier() {
		assertNull(PassiveEffect.forStacks(BULWARK, 0));
	}

	@Test
	void negativeStacksMeanNoModifier() {
		assertNull(PassiveEffect.forStacks(BULWARK, -1));
	}

	@Test
	void missingPassiveMeansNoModifier() {
		assertNull(PassiveEffect.forStacks(null, 3));
	}

	@Test
	void oneStackGrantsThePassivesOwnAttributeModifier() {
		PassiveEffect.Modifier wanted = PassiveEffect.forStacks(BULWARK, 1);

		assertEquals(ClassRegistry.ARMOR, wanted.attribute());
		assertEquals(Identifier.fromNamespaceAndPath("infusedclasses", "bulwark_armor"), wanted.modifierId());
		assertEquals(1.0, wanted.amount());
	}

	@Test
	void stacksScaleLinearlyToTheCap() {
		PassiveEffect.Modifier atCap = PassiveEffect.forStacks(BULWARK, BULWARK.maxStacks());

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
				3,
				2);

		PassiveEffect.Modifier wanted = PassiveEffect.forStacks(swift, 3);

		assertEquals(speed, wanted.attribute());
		assertEquals(Identifier.fromNamespaceAndPath("infusedclasses", "swift_speed"), wanted.modifierId());
		assertEquals(1.5, wanted.amount());
	}
}