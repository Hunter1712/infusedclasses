package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: current talent state. One progress string ("xp,level,unspent,nodes")
 * so the codec stays a single string and parsing reuses the pure module on both sides.
 */
public record TalentStatePayload(String state) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TalentStatePayload> TYPE = ClassPackets.TALENT_STATE;

	public static final StreamCodec<ByteBuf, TalentStatePayload> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.STRING_UTF8, TalentStatePayload::state, TalentStatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
