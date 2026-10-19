package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
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
		// Every number is distinct, so a lane order or window order drift inside the read
		// cannot hide behind equal neighbours.
		EnumMap<Lane, LaneRead> lanes = new EnumMap<>(Lane.class);
		lanes.put(Lane.OFFLOAD, new LaneRead(1_000, 2_000, true));
		lanes.put(Lane.ONSLAUGHT, new LaneRead(3_000, 4_000, false));
		lanes.put(Lane.ROTATE, new LaneRead(5_000, 6_000, true));
		lanes.put(Lane.OATH, new LaneRead(7_000, 8_000, false));
		lanes.put(Lane.ROULETTE, new LaneRead(9_000, 10_000, true));
		lanes.put(Lane.ALL_IN, new LaneRead(11_000, 12_000, false));
		PlayerStatusPayload populated = new PlayerStatusPayload(
				"infusedclasses:gambler", lanes, RotateAbility.Stance.SCUTUM,
				RouletteAbility.Outcome.JACKPOT, 7, 3_500, 2_500, RotateAbility.Stance.GLADIUS, 2);
		ByteBuf raw = Unpooled.buffer();
		FriendlyByteBuf buf = new FriendlyByteBuf(raw);
		try {
			PlayerStatusPayload.STREAM_CODEC.encode(buf, populated);
			assertEquals(populated, PlayerStatusPayload.STREAM_CODEC.decode(buf));

			buf.clear();
			PlayerStatusPayload bare = PlayerStatusPayload.classless();
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
