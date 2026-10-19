package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
