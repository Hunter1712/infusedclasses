package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stacks sync signal: entry building plus codec round-trip, empty list included. */
class PassiveStacksSyncTest {
	@Test
	void entriesCarryTheMillisUntilDecayValue() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(warrior(), ConditionalPassive.ON_HIT_TAKEN, base);

		List<PassiveStacksPayload.Entry> entries = PassiveStacksPayload.entriesFor(warrior(), runtime, base + 2_000);

		assertEquals(1, entries.size());
		PassiveStacksPayload.Entry entry = entries.get(0);
		assertEquals("Bulwark", entry.passiveName());
		assertEquals(1, entry.stacks());
		assertEquals(1.0, entry.perStack());
		assertEquals(5, entry.graceSeconds());
		assertEquals(3_000, entry.millisUntilDecay());
	}

	@Test
	void zeroStackEntriesClearTheRowAndClasslessYieldsEmpty() {
		PlayerRuntime runtime = new PlayerRuntime();
		List<PassiveStacksPayload.Entry> entries = PassiveStacksPayload.entriesFor(warrior(), runtime, 0L);
		assertEquals(1, entries.size());
		assertEquals(0, entries.get(0).stacks());
		assertEquals(0, entries.get(0).millisUntilDecay());

		assertTrue(PassiveStacksPayload.entriesFor(null, runtime, 0L).isEmpty());
		assertTrue(PassiveStacksPayload.entriesFor(warrior(), null, 0L).isEmpty());
	}

	@Test
	void payloadRoundTripsIncludingEmptyClear() {
		PlayerRuntime runtime = new PlayerRuntime();
		long base = 1_000_000L;
		runtime.onTrigger(warrior(), ConditionalPassive.ON_HIT_TAKEN, base);
		runtime.onTrigger(warrior(), ConditionalPassive.ON_HIT_TAKEN, base);
		roundTrip(new PassiveStacksPayload(PassiveStacksPayload.entriesFor(warrior(), runtime, base)));
		roundTrip(new PassiveStacksPayload(List.of()));
	}

	private static void roundTrip(PassiveStacksPayload original) {
		ByteBuf buf = Unpooled.buffer();
		try {
			PassiveStacksPayload.STREAM_CODEC.encode(buf, original);
			assertEquals(original, PassiveStacksPayload.STREAM_CODEC.decode(buf));
		} finally {
			buf.release();
		}
	}

	private static ClassDefinition warrior() {
		return ClassRegistry.byId(ClassRegistry.WARRIOR);
	}
}
