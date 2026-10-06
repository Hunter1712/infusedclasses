package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocolTest {
	@Test
	void sevenDistinctNamespacedChannels() {
		Set<Identifier> ids = new HashSet<>(Set.of(
				SelectClassPayload.TYPE.id(),
				OpenClassScreenPayload.TYPE.id(),
				WarCryPayload.TYPE.id(),
				SecondWindPayload.TYPE.id(),
				ClassConfirmedPayload.TYPE.id(),
				PassiveStacksPayload.TYPE.id(),
				WarCryExpiryPayload.TYPE.id()));

		assertEquals(7, ids.size());
		for (Identifier id : ids) {
			assertEquals(ClassPackets.MOD_ID, id.getNamespace());
		}
	}

	@Test
	void selectClassCodecRoundTrips() {
		Identifier id = Identifier.fromNamespaceAndPath("infusedclasses", "warrior");
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
			WarCryPayload.STREAM_CODEC.encode(buf, new WarCryPayload());
			WarCryPayload decoded = WarCryPayload.STREAM_CODEC.decode(buf);
			assertEquals(new WarCryPayload(), decoded);

			buf.clear();
			OpenClassScreenPayload.STREAM_CODEC.encode(buf, new OpenClassScreenPayload());
			OpenClassScreenPayload decoded2 = OpenClassScreenPayload.STREAM_CODEC.decode(buf);
			assertEquals(new OpenClassScreenPayload(), decoded2);

			buf.clear();
			ClassConfirmedPayload.STREAM_CODEC.encode(buf, new ClassConfirmedPayload());
			ClassConfirmedPayload decoded3 = ClassConfirmedPayload.STREAM_CODEC.decode(buf);
			assertEquals(new ClassConfirmedPayload(), decoded3);

			buf.clear();
			SecondWindPayload.STREAM_CODEC.encode(buf, new SecondWindPayload());
			SecondWindPayload decoded4 = SecondWindPayload.STREAM_CODEC.decode(buf);
			assertEquals(new SecondWindPayload(), decoded4);
		} finally {
			buf.release();
		}
	}

	@Test
	void warCryExpiryCodecRoundTrips() {
		ByteBuf buf = Unpooled.buffer();
		try {
			WarCryExpiryPayload original = new WarCryExpiryPayload(200);
			WarCryExpiryPayload.STREAM_CODEC.encode(buf, original);
			assertEquals(original, WarCryExpiryPayload.STREAM_CODEC.decode(buf));
		} finally {
			buf.release();
		}
	}
}
