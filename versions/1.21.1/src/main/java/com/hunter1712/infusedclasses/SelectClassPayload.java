package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client intent: "I picked this class". Server validates and applies. */
public record SelectClassPayload(ResourceLocation classId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SelectClassPayload> TYPE =
			new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(InfusedClasses.MOD_ID, "select_class"));

	public static final StreamCodec<ByteBuf, SelectClassPayload> STREAM_CODEC =
			StreamCodec.composite(ResourceLocation.STREAM_CODEC, SelectClassPayload::classId, SelectClassPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

