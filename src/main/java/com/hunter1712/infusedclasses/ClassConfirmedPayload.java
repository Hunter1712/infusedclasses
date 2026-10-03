package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server confirms the pick. Client closes the picker only on this, so rejected picks stay for retry. */
public record ClassConfirmedPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClassConfirmedPayload> TYPE = InfusedClasses.CLASS_CONFIRMED;

	public static final StreamCodec<ByteBuf, ClassConfirmedPayload> STREAM_CODEC =
			StreamCodec.unit(new ClassConfirmedPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

