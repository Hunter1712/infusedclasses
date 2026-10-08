package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageSettleTest {
	@Test
	void overOneLandsExtraUnderOneHealsBackAtOneNothing() {
		assertEquals(5.0F, DamageSettle.settleMath(10.0F, 1.5));
		assertEquals(-2.0F, DamageSettle.settleMath(10.0F, 0.8));
		assertEquals(0.0F, DamageSettle.settleMath(10.0F, 1.0));
	}
}
