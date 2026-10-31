package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocolTest {
	@Test
	void fourDistinctNamespacedChannels() {
		Set<ResourceLocation> ids = new HashSet<>(Set.of(
				SelectClassPayload.TYPE.getId(),
				OpenClassScreenPayload.TYPE.getId(),
				RendPayload.TYPE.getId(),
				ClassConfirmedPayload.TYPE.getId(),
				PlayerStatusPayload.TYPE.getId()));

		assertEquals(5, ids.size());
		for (ResourceLocation id : ids) {
			assertEquals(InfusedClasses.MOD_ID, id.getNamespace());
		}
	}

	@Test
	void rushPayloadHasItsOwnChannel() {
		assertEquals("infusedclasses:rush", RushPayload.TYPE.getId().toString());
		assertEquals(InfusedClasses.MOD_ID, RushPayload.TYPE.getId().getNamespace());
	}

	@Test
	void selectClassCodecRoundTrips() {
		String id = "infusedclasses:juggernaut";
		SelectClassPayload original = new SelectClassPayload(id);
		ByteBuf raw = Unpooled.buffer();
		FriendlyByteBuf buf = new FriendlyByteBuf(raw);
		try {
			original.write(buf);
			SelectClassPayload decoded = SelectClassPayload.TYPE.read(buf);
			assertEquals(id, decoded.classId());
		} finally {
			raw.release();
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
				"infusedclasses:gambler",
				new PlayerRuntime.State(lanes, RotateAbility.Stance.SCUTUM,
						RouletteAbility.Outcome.JACKPOT, RotateAbility.Stance.GLADIUS, 2),
				new PlayerRuntime.BarRead(7, 3_500, 2_500));
		ByteBuf raw = Unpooled.buffer();
		FriendlyByteBuf buf = new FriendlyByteBuf(raw);
		try {
			populated.write(buf);
			assertEquals(populated, PlayerStatusPayload.TYPE.read(buf));

			buf.clear();
			PlayerStatusPayload bare = PlayerStatusPayload.classless();
			bare.write(buf);
			assertEquals(bare, PlayerStatusPayload.TYPE.read(buf));
		} finally {
			raw.release();
		}
	}

	@Test
	void unitPayloadsCodecRoundTrips() {
		ByteBuf raw = Unpooled.buffer();
		FriendlyByteBuf buf = new FriendlyByteBuf(raw);
		try {
			OpenClassScreenPayload open = new OpenClassScreenPayload("infusedclasses:juggernaut");
			open.write(buf);
			assertEquals(open, OpenClassScreenPayload.TYPE.read(buf));

			buf.clear();
			OpenClassScreenPayload empty = new OpenClassScreenPayload("");
			empty.write(buf);
			assertEquals(empty, OpenClassScreenPayload.TYPE.read(buf));

			buf.clear();
			ClassConfirmedPayload confirmed = new ClassConfirmedPayload();
			confirmed.write(buf);
			assertEquals(confirmed, ClassConfirmedPayload.TYPE.read(buf));

			buf.clear();
			RendPayload rend = new RendPayload();
			rend.write(buf);
			assertEquals(rend, RendPayload.TYPE.read(buf));

			buf.clear();
			RushPayload rush = new RushPayload();
			rush.write(buf);
			assertEquals(rush, RushPayload.TYPE.read(buf));
		} finally {
			raw.release();
		}
	}
}
