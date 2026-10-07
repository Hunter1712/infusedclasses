package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client intent: "I pressed the Rend key". Server validates class, stacks and cooldown. */
public record RendPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RendPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "rend"));

	public static final StreamCodec<ByteBuf, RendPayload> STREAM_CODEC =
			StreamCodec.unit(new RendPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
