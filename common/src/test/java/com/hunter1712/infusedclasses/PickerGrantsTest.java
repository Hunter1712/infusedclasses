package com.hunter1712.infusedclasses;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The picker grant as a decision: one transient permission per player, consumed on use
 * and cleared without touching anyone else. No player is constructed.
 */
class PickerGrantsTest {
	private static final UUID ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@AfterEach
	void clearGrants() {
		PickerGrants.clear(ONE);
		PickerGrants.clear(TWO);
	}

	@Test
	void aGrantIsPerPlayerAndGoesAwayWhenCleared() {
		PickerGrants.grant(ONE);

		assertTrue(PickerGrants.isGranted(ONE));
		assertFalse(PickerGrants.isGranted(TWO));

		PickerGrants.clear(ONE);

		assertFalse(PickerGrants.isGranted(ONE));
	}

	@Test
	void clearingOnePlayerLeavesAnotherGrantAlone() {
		PickerGrants.grant(ONE);
		PickerGrants.grant(TWO);

		PickerGrants.clear(ONE);

		assertFalse(PickerGrants.isGranted(ONE));
		assertTrue(PickerGrants.isGranted(TWO));
	}
}
