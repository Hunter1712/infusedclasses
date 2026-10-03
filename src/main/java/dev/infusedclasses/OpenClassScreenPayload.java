package dev.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server tells client to open the picker. Sent once, on first join. */
public record OpenClassScreenPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenClassScreenPayload> TYPE = InfusedClasses.OPEN_CLASS_SCREEN;

	public static final StreamCodec<ByteBuf, OpenClassScreenPayload> STREAM_CODEC =
			StreamCodec.unit(new OpenClassScreenPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
