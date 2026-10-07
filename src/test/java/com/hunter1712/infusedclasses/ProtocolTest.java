package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocolTest {
	@Test
	void fourDistinctNamespacedChannels() {
		Set<Identifier> ids = new HashSet<>(Set.of(
				SelectClassPayload.TYPE.id(),
				OpenClassScreenPayload.TYPE.id(),
				RendPayload.TYPE.id(),
				ClassConfirmedPayload.TYPE.id()));

		assertEquals(4, ids.size());
		for (Identifier id : ids) {
			assertEquals(ClassPackets.MOD_ID, id.getNamespace());
		}
	}

	@Test
	void rushPayloadHasItsOwnChannel() {
		assertEquals("infusedclasses:rush", RushPayload.TYPE.id().toString());
		assertEquals(ClassPackets.MOD_ID, RushPayload.TYPE.id().getNamespace());
	}

	@Test
	void selectClassCodecRoundTrips() {
		Identifier id = Identifier.fromNamespaceAndPath("infusedclasses", "juggernaut");
		SelectClassPayload original = new SelectClassPayload(id);
		ByteBuf buf = Unpooled.buffer();
		try {
			SelectClassPayload.STREAM_CODEC.encode(buf, original);
			SelectClassPayload decoded = SelectClassPayload.STREAM_CODEC.decode(buf);
			assertEquals(id, decoded.classId());
		} finally {
			buf.release();
		}
	}

	@Test
	void unitPayloadsCodecRoundTrips() {
		ByteBuf buf = Unpooled.buffer();
		try {
			buf.clear();
			OpenClassScreenPayload.STREAM_CODEC.encode(buf, new OpenClassScreenPayload());
			OpenClassScreenPayload decoded2 = OpenClassScreenPayload.STREAM_CODEC.decode(buf);
			assertEquals(new OpenClassScreenPayload(), decoded2);

			buf.clear();
			ClassConfirmedPayload.STREAM_CODEC.encode(buf, new ClassConfirmedPayload());
			ClassConfirmedPayload decoded3 = ClassConfirmedPayload.STREAM_CODEC.decode(buf);
			assertEquals(new ClassConfirmedPayload(), decoded3);

			buf.clear();
			RendPayload.STREAM_CODEC.encode(buf, new RendPayload());
			RendPayload decoded4 = RendPayload.STREAM_CODEC.decode(buf);
			assertEquals(new RendPayload(), decoded4);

			buf.clear();
			RushPayload.STREAM_CODEC.encode(buf, new RushPayload());
			RushPayload decoded5 = RushPayload.STREAM_CODEC.decode(buf);
			assertEquals(new RushPayload(), decoded5);
		} finally {
			buf.release();
		}
	}
}
