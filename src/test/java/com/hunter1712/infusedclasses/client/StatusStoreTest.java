package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.PlayerStatusPayload;
import com.hunter1712.infusedclasses.RotateAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The client's one snapshot slot: overwritten whole, cleared on disconnect. */
class StatusStoreTest {
	@BeforeEach
	void startEmpty() {
		StatusStore.clear();
	}

	@Test
	void acceptOverwritesTheWholeSnapshotAndItsReceiptTime() {
		StatusStore.accept(snapshot("infusedclasses:juggernaut"), 1000L);
		PlayerStatusPayload fresh = snapshot("infusedclasses:legionary");

		StatusStore.accept(fresh, 4000L);

		assertEquals(fresh, StatusStore.status());
		assertEquals(0L, StatusStore.elapsed(4000L));
		assertEquals(500L, StatusStore.elapsed(4500L));
	}

	@Test
	void startsEmptyAndDisconnectClearsTheSnapshot() {
		assertNull(StatusStore.status());

		StatusStore.accept(snapshot("infusedclasses:juggernaut"), 1000L);
		StatusStore.clear();

		assertNull(StatusStore.status());
	}

	private static PlayerStatusPayload snapshot(String classId) {
		return new PlayerStatusPayload(
				classId,
				0L, 0L, 0L, 0L, 0L, 0L,
				0L, false, 0L, false, 0L,
				null, false, 0L,
				null, 0L, 0L,
				0, 0L, RotateAbility.Stance.SCUTUM, 0);
	}
}
