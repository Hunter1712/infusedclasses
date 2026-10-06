package com.hunter1712.infusedclasses.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientWarCryTest {
	@BeforeEach
	void clear() {
		ClientWarCry.clear();
	}

	@Test
	void emptyReadsZero() {
		assertEquals(0, ClientWarCry.ticksLeft(1_000_000L));
	}

	@Test
	void countdownTicksDownAtTwentyPerSecond() {
		long base = 1_000_000L;
		ClientWarCry.set(160, base);

		assertEquals(120, ClientWarCry.ticksLeft(base + 2_000));
		assertEquals(0, ClientWarCry.ticksLeft(base + 8_000));
		assertEquals(0, ClientWarCry.ticksLeft(base + 20_000));
	}

	@Test
	void newSignalRestampsAndZeroClears() {
		long base = 1_000_000L;
		ClientWarCry.set(100, base);
		ClientWarCry.set(160, base + 2_000);

		assertEquals(160, ClientWarCry.ticksLeft(base + 2_000));

		ClientWarCry.set(0, base + 3_000);
		assertEquals(0, ClientWarCry.ticksLeft(base + 3_000));
	}
}
