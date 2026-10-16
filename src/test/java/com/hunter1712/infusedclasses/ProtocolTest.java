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
	void fiveDistinctNamespacedChannels() {
		Set<Identifier> ids = new HashSet<>(Set.of(
				SelectClassPayload.TYPE.id(),
				OpenClassScreenPayload.TYPE.id(),
				RendPayload.TYPE.id(),
				ClassConfirmedPayload.TYPE.id(),
				PlayerStatusPayload.TYPE.id()));

		assertEquals(5, ids.size());
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
	void playerStatusPayloadHasItsOwnChannel() {
		assertEquals("infusedclasses:player_status", PlayerStatusPayload.TYPE.id().toString());
		assertEquals(ClassPackets.MOD_ID, PlayerStatusPayload.TYPE.id().getNamespace());
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

	@Test
	void playerStatusCodecRoundTripsFullSnapshot() {
		PlayerStatusPayload original = new PlayerStatusPayload(
				"infusedclasses:legionary",
				1200L, -1L, 800L, 900L, 250L, 1750L,
				4500L, true, 600L, true, 300L,
				RotateAbility.Stance.GLADIUS, true, 950L,
				RouletteAbility.Outcome.JACKPOT, 1100L, 700L,
				5, 2100L, RotateAbility.Stance.SCUTUM, 7);

		assertEquals(original, roundTrip(original));
	}

	@Test
	void playerStatusCodecRoundTripsClasslessSnapshot() {
		PlayerStatusPayload original = new PlayerStatusPayload(
				"",
				0L, 0L, 0L, 0L, 0L, 0L,
				0L, false, 0L, false, 0L,
				null, false, 0L,
				null, 0L, 0L,
				0, 0L, RotateAbility.Stance.SCUTUM, 0);

		assertEquals(original, roundTrip(original));
	}

	private static PlayerStatusPayload roundTrip(PlayerStatusPayload original) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		try {
			PlayerStatusPayload.STREAM_CODEC.encode(buf, original);
			return PlayerStatusPayload.STREAM_CODEC.decode(buf);
		} finally {
			buf.release();
		}
	}
}
