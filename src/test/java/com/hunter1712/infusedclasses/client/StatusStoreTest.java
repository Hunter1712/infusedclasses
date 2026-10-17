package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.PlayerStatusPayload;
import com.hunter1712.infusedclasses.RotateAbility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The client store: one snapshot, its receipt time, and a clean disconnect. */
class StatusStoreTest {
	private static PlayerStatusPayload snapshot(String classId) {
		return new PlayerStatusPayload(classId, 0, 0, 0, 0, 0, 0, 0, false, 0, false, 0, null, false,
				0, null, 0, 0, 0, 0, 0, RotateAbility.Stance.SCUTUM, 0);
	}

	@Test
	void acceptReplacesTheSnapshotAndItsReceiptTime() {
		StatusStore.clear();
		assertNull(StatusStore.status());

		StatusStore.accept(snapshot("infusedclasses:juggernaut"), 1_000);
		assertEquals("infusedclasses:juggernaut", StatusStore.status().classId());
		assertEquals(1_500, StatusStore.elapsed(2_500));

		StatusStore.accept(snapshot("infusedclasses:gambler"), 3_000);
		assertEquals("infusedclasses:gambler", StatusStore.status().classId());
		assertEquals(200, StatusStore.elapsed(3_200));
	}

	@Test
	void clearLeavesNothingBehind() {
		StatusStore.accept(snapshot("infusedclasses:legionary"), 1_000);

		StatusStore.clear();

		assertNull(StatusStore.status());
		assertEquals(0, StatusStore.elapsed(0));
	}
}
