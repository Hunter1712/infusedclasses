package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The last-hurt memory after a settled bonus: vanilla compares the next hit against the
 * biggest hurt of the window, so the bonus must fold into what the base hit left without
 * counting a rejected or fully eaten one.
 */
class LastHurtMemoryTest {
	@Test
	void aLandedBonusAddsWhatItTookToTheOldMemory() {
		assertEquals(3.5F, LastHurtMemory.restore(2.0F, 20.0F, 18.5F));
	}

	@Test
	void aFullyEatenBonusLeavesTheMemoryAlone() {
		assertEquals(2.0F, LastHurtMemory.restore(2.0F, 20.0F, 20.0F));
	}

	@Test
	void aRejectedBonusWithNoDeltaLeavesAnEmptyMemoryEmpty() {
		assertEquals(0.0F, LastHurtMemory.restore(0.0F, 20.0F, 20.0F));
	}

	@Test
	void aBonusThatSomehowRaisesHealthGainsNothing() {
		assertEquals(2.0F, LastHurtMemory.restore(2.0F, 20.0F, 21.0F));
	}
}
