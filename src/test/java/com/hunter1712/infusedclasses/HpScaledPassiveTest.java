package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The HP-scaled passive's own facts: bucket math from live HP and copy generated from the
 * numbers. No class registers one today (Vigor is cut); the module stays because the applier
 * still reads the bucket list.
 */
class HpScaledPassiveTest {
	private static final HpScaledPassive SPEED = new HpScaledPassive(ClassRegistry.ATTACK_SPEED,
			Identifier.fromNamespaceAndPath("infusedclasses", "test_speed"), 0.075, "Test", -0.25);

	@Test
	void bucketsFollowCurrentHp() {
		assertEquals(10, HpScaledPassive.bucketForCurrent(20, 20));
		assertEquals(9, HpScaledPassive.bucketForCurrent(18, 20));
		assertEquals(5, HpScaledPassive.bucketForCurrent(10, 20));
		assertEquals(0, HpScaledPassive.bucketForCurrent(1, 20));
		assertEquals(0, HpScaledPassive.bucketForCurrent(0, 20));
	}

	@Test
	void bucketsScaleWithTheBiggerPool() {
		assertEquals(10, HpScaledPassive.bucketForCurrent(40, 40));
		assertEquals(5, HpScaledPassive.bucketForCurrent(20, 40));
		assertEquals(0, HpScaledPassive.bucketForCurrent(0, 40));
	}

	@Test
	void bucketsClampPastTheEnds() {
		assertEquals(10, HpScaledPassive.bucketForCurrent(25, 20));
		assertEquals(0, HpScaledPassive.bucketForCurrent(-3, 20));
		assertEquals(0, HpScaledPassive.bucketForCurrent(10, 0));
	}

	@Test
	void maxBonusIsTheFloorPlusEveryStep() {
		assertEquals(0.5, SPEED.maxBonus());
	}

	@Test
	void copyIsGeneratedFromTheNumbers() {
		assertEquals("HP-scaled: -0.25 at 0 HP to +0.5 full HP.", SPEED.fullDesc());
	}

	@Test
	void aClassCarryingThePassiveSweepsItsModifierAndGroupsItsCardLine() {
		ClassDefinition def = new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "hp_only"),
				"HP Only", List.of(), List.of(SPEED), 0, "",
				List.of(), List.of(), CritRule.universal());

		assertEquals(List.of("test_speed"), PassiveApplier.cleanupFor(def, null).stream()
				.map(PassiveEffect.Modifier::modifierId)
				.map(Identifier::getPath)
				.toList());
		assertTrue(ClassPresentation.cardLinesFor(def).stream()
				.anyMatch(line -> line.kind() == ClassPresentation.LineKind.PASSIVE
						&& line.name().equals(SPEED.group())
						&& line.desc().equals(SPEED.fullDesc())));
	}
}
