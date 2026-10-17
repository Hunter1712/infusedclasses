package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
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
				ClassConfirmedPayload.TYPE.id(),
				PlayerStatusPayload.TYPE.id()));

		assertEquals(5, ids.size());
		for (Identifier id : ids) {
			assertEquals(InfusedClasses.MOD_ID, id.getNamespace());
		}
	}

	@Test
	void rushPayloadHasItsOwnChannel() {
		assertEquals("infusedclasses:rush", RushPayload.TYPE.id().toString());
		assertEquals(InfusedClasses.MOD_ID, RushPayload.TYPE.id().getNamespace());
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
	void playerStatusCodecRoundTripsPopulatedAndNulls() {
		PlayerStatusPayload populated = new PlayerStatusPayload(
				"infusedclasses:gambler", 25_000, 60_000, 0, 0, 12_345, 75_000,
				4_000, true, 5_000, false, 1_500, RotateAbility.Stance.SCUTUM, true,
				9_000, RouletteAbility.Outcome.JACKPOT, 6_000, 15_000,
				7, 3_500, 2_500, RotateAbility.Stance.GLADIUS, 2);
		ByteBuf raw = Unpooled.buffer();
		FriendlyByteBuf buf = new FriendlyByteBuf(raw);
		try {
			PlayerStatusPayload.STREAM_CODEC.encode(buf, populated);
			assertEquals(populated, PlayerStatusPayload.STREAM_CODEC.decode(buf));

			buf.clear();
			PlayerStatusPayload bare = new PlayerStatusPayload(
					"", 0, 0, 0, 0, 0, 0, 0, false, 0, false, 0, null, false,
					0, null, 0, 0, 0, 0, 0, RotateAbility.Stance.SCUTUM, 0);
			PlayerStatusPayload.STREAM_CODEC.encode(buf, bare);
			assertEquals(bare, PlayerStatusPayload.STREAM_CODEC.decode(buf));
		} finally {
			raw.release();
		}
	}

	@Test
	void unitPayloadsCodecRoundTrips() {
		ByteBuf buf = Unpooled.buffer();
		try {
			buf.clear();
			OpenClassScreenPayload.STREAM_CODEC.encode(buf, new OpenClassScreenPayload("infusedclasses:juggernaut"));
			assertEquals(new OpenClassScreenPayload("infusedclasses:juggernaut"),
					OpenClassScreenPayload.STREAM_CODEC.decode(buf));

			buf.clear();
			OpenClassScreenPayload.STREAM_CODEC.encode(buf, new OpenClassScreenPayload(""));
			assertEquals(new OpenClassScreenPayload(""), OpenClassScreenPayload.STREAM_CODEC.decode(buf));

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
