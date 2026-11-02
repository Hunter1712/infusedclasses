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

	/**
	 * The mults are post-mitigation, so their base is what the hit landed, not the
	 * pre-armor amount the damage hook reports. Worked example on iron armor: 6.0 lands
	 * of a 10.0 post-shield hit, a x1.25 crit adds 1.5 (net 7.5, the balance model's
	 * number) and a x0.85 take heals 0.9 back (net 5.1).
	 */
	@Test
	void theMultPricesWhatLandedNotWhatWasTaken() {
		assertEquals(1.5F, DamageSettle.settleMath(6.0F, 1.25));
		assertEquals(-0.9F, DamageSettle.settleMath(6.0F, 0.85));
	}
}
