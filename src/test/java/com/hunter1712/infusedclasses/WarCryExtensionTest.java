package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WarCryExtensionTest {
	/** The eligible baseline: every gate passes, so only the roll decides. */
	private static WarCryExtension.Hit baseHit(double roll) {
		return new WarCryExtension.Hit(true, true, true, true, 160, roll);
	}

	@Test
	void chargedHitUnderRateExtends() {
		assertEquals(200, WarCryExtension.extendRoll(baseHit(0.0)));
		assertEquals(200, WarCryExtension.extendRoll(baseHit(0.249)));
	}

	@Test
	void everyGateRejects() {
		WarCryExtension.Hit base = baseHit(0.0);
		assertEquals(0, WarCryExtension.extendRoll(new WarCryExtension.Hit(false, base.charged(),
				base.warCryLive(), base.victimEligible(), base.remainingTicks(), base.roll())));
		assertEquals(0, WarCryExtension.extendRoll(new WarCryExtension.Hit(base.warrior(), false,
				base.warCryLive(), base.victimEligible(), base.remainingTicks(), base.roll())));
		assertEquals(0, WarCryExtension.extendRoll(new WarCryExtension.Hit(base.warrior(), base.charged(),
				false, base.victimEligible(), 0, base.roll())));
		assertEquals(0, WarCryExtension.extendRoll(new WarCryExtension.Hit(base.warrior(), base.charged(),
				base.warCryLive(), false, base.remainingTicks(), base.roll())));
		assertEquals(0, WarCryExtension.extendRoll(baseHit(0.25)));
		assertEquals(0, WarCryExtension.extendRoll(baseHit(0.99)));
	}

	@Test
	void noCapNoClamp() {
		assertEquals(1_040, WarCryExtension.extendRoll(
				new WarCryExtension.Hit(true, true, true, true, 1_000, 0.1)));
	}

	@Test
	void multiVictimIsNIndependentCalls() {
		double[] rolls = {0.1, 0.5, 0.2};
		int procs = 0;
		for (double roll : rolls) {
			if (WarCryExtension.extendRoll(baseHit(roll)) > 0) {
				procs++;
			}
		}
		assertEquals(2, procs);
	}
}
