package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client intent: "refund my talents". Server validates class and re-sends state. */
public record RespecTalentsPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RespecTalentsPayload> TYPE = ClassPackets.RESPEC_TALENTS;

	public static final StreamCodec<ByteBuf, RespecTalentsPayload> STREAM_CODEC =
			StreamCodec.unit(new RespecTalentsPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
