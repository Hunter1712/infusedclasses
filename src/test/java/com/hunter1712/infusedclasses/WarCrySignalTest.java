package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Edge-triggered clock clear in booleans: adopt, report once, then silence. */
class WarCrySignalTest {
	@Test
	void liveEffectAdoptsWithoutReporting() {
		WarCrySignal signal = new WarCrySignal();

		assertFalse(signal.pollLost(true));
		assertFalse(signal.pollLost(true));
	}

	@Test
	void deadEffectWithBankedSignalReportsOnce() {
		WarCrySignal signal = new WarCrySignal();
		signal.markSent();

		assertTrue(signal.pollLost(false));
		assertFalse(signal.pollLost(false));
	}

	@Test
	void deadEffectWithNothingBankedStaysSilent() {
		WarCrySignal signal = new WarCrySignal();

		assertFalse(signal.pollLost(false));
	}

	@Test
	void relogAdoptsTheLiveEffectSoLaterMilkStillClears() {
		WarCrySignal signal = new WarCrySignal();

		assertFalse(signal.pollLost(true));
		assertTrue(signal.pollLost(false));
	}

	@Test
	void clearDropsABankedSignalWithoutReporting() {
		WarCrySignal signal = new WarCrySignal();
		signal.markSent();
		signal.clear();

		assertFalse(signal.pollLost(false));
	}
}
