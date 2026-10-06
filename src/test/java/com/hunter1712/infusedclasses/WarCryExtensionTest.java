package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WarCryExtensionTest {
	private static WarCryExtension.Hit hit(boolean warrior, boolean charged, boolean warCryLive,
			boolean victimEligible, int remaining, double roll) {
		return new WarCryExtension.Hit(warrior, charged, warCryLive, victimEligible, remaining, roll);
	}

	@Test
	void chargedHitUnderRateExtends() {
		assertEquals(200, WarCryExtension.extendRoll(hit(true, true, true, true, 160, 0.0)));
		assertEquals(200, WarCryExtension.extendRoll(hit(true, true, true, true, 160, 0.249)));
	}

	@Test
	void everyGateRejects() {
		assertEquals(0, WarCryExtension.extendRoll(hit(false, true, true, true, 160, 0.0)));
		assertEquals(0, WarCryExtension.extendRoll(hit(true, false, true, true, 160, 0.0)));
		assertEquals(0, WarCryExtension.extendRoll(hit(true, true, false, true, 0, 0.0)));
		assertEquals(0, WarCryExtension.extendRoll(hit(true, true, true, false, 160, 0.0)));
		assertEquals(0, WarCryExtension.extendRoll(hit(true, true, true, true, 160, 0.25)));
		assertEquals(0, WarCryExtension.extendRoll(hit(true, true, true, true, 160, 0.99)));
	}

	@Test
	void noCapNoClamp() {
		assertEquals(1_040, WarCryExtension.extendRoll(hit(true, true, true, true, 1_000, 0.1)));
	}

	@Test
	void multiVictimIsNIndependentCalls() {
		double[] rolls = {0.1, 0.5, 0.2};
		int procs = 0;
		for (double roll : rolls) {
			if (WarCryExtension.extendRoll(hit(true, true, true, true, 160, roll)) > 0) {
				procs++;
			}
		}
		assertEquals(2, procs);
	}
}
