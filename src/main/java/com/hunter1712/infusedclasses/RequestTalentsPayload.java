package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client intent: "send me my talent state". Server replies with TalentStatePayload. */
public record RequestTalentsPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RequestTalentsPayload> TYPE = ClassPackets.REQUEST_TALENTS;

	public static final StreamCodec<ByteBuf, RequestTalentsPayload> STREAM_CODEC =
			StreamCodec.unit(new RequestTalentsPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
