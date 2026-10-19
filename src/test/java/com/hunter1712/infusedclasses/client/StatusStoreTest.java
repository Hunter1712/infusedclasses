package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.Lane;
import com.hunter1712.infusedclasses.LaneRead;
import com.hunter1712.infusedclasses.PlayerRuntime;
import com.hunter1712.infusedclasses.PlayerStatusPayload;
import com.hunter1712.infusedclasses.RotateAbility;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The client store: one snapshot, its receipt time, and a clean disconnect. */
class StatusStoreTest {
	/** A snapshot with a class id and nothing else: the store only cares that it is one value. */
	private static PlayerStatusPayload snapshot(String classId) {
		EnumMap<Lane, LaneRead> lanes = new EnumMap<>(Lane.class);
		for (Lane lane : Lane.values()) {
			lanes.put(lane, new LaneRead(0, 0, false));
		}
		PlayerRuntime.State state = new PlayerRuntime.State(lanes, null, null,
				RotateAbility.Stance.SCUTUM, 0);
		return new PlayerStatusPayload(classId, state, PlayerRuntime.BarRead.NONE);
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
