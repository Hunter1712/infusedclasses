package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassRegistry;
import com.hunter1712.infusedclasses.ConditionalPassive;
import com.hunter1712.infusedclasses.PassiveStacksPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Client stacks store with a fake clock: interpolation, re-stamp, hide and clear. */
class ClientPassiveStacksTest {
	@BeforeEach
	void clear() {
		ClientPassiveStacks.clear();
	}

	@Test
	void countdownTicksDownWithoutNewPackets() {
		long base = 1_000_000L;
		ClientPassiveStacks.setAll(List.of(entry(3, 5_000)), base);

		assertEquals(3_000, ClientPassiveStacks.millisUntilDecay("Bulwark", base + 2_000));
		assertEquals(3, ClientPassiveStacks.stacksFor(bulwark(), base + 2_000));
	}

	@Test
	void newServerEntryRestampsTheCountdown() {
		long base = 1_000_000L;
		ClientPassiveStacks.setAll(List.of(entry(3, 5_000)), base);
		ClientPassiveStacks.setAll(List.of(entry(4, 5_000)), base + 2_000);

		assertEquals(5_000, ClientPassiveStacks.millisUntilDecay("Bulwark", base + 2_000));
		assertEquals(4, ClientPassiveStacks.stacksFor(bulwark(), base + 2_000));
	}

	@Test
	void zeroStackEntryHidesTheRowAndEmptySignalClears() {
		long base = 1_000_000L;
		ClientPassiveStacks.setAll(List.of(entry(0, 0)), base);
		assertEquals(0, ClientPassiveStacks.stacksFor(bulwark(), base));

		ClientPassiveStacks.setAll(List.of(entry(3, 5_000)), base);
		ClientPassiveStacks.setAll(List.of(), base + 1_000);
		assertTrue(ClientPassiveStacks.isEmpty());
	}

	@Test
	void unlistedNamesAreDroppedOnReplace() {
		long base = 1_000_000L;
		ClientPassiveStacks.setAll(List.of(
				entry(3, 5_000),
				new PassiveStacksPayload.Entry("Swift", 2, 0.5, 2, 2_000)), base);
		ClientPassiveStacks.setAll(List.of(entry(3, 5_000)), base + 1_000);

		assertEquals(0, ClientPassiveStacks.millisUntilDecay("Swift", base + 1_000));
		assertEquals(3, ClientPassiveStacks.stacksFor(bulwark(), base + 1_000));
	}

	@Test
	void staleEntriesPastGraceReadAsZero() {
		long base = 1_000_000L;
		ClientPassiveStacks.setAll(List.of(entry(3, 5_000)), base);
		// Past the 5s grace every stack is gone, however many were held.
		assertEquals(0, ClientPassiveStacks.stacksFor(bulwark(), base + 6_000));
		assertEquals(0, ClientPassiveStacks.millisUntilDecay("Bulwark", base + 6_000));
	}

	private static PassiveStacksPayload.Entry entry(int stacks, long until) {
		return new PassiveStacksPayload.Entry("Bulwark", stacks, 1.0, 5, until);
	}

	private static ConditionalPassive bulwark() {
		return ClassRegistry.byId(ClassRegistry.WARRIOR).passives().get(0);
	}
}
