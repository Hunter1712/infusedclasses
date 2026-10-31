package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Ultimate slot intent: "I pressed the ultimate bind". Server validates class, stacks and cooldown. */
public record UltimatePayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UltimatePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ModIds.MOD_ID, "ultimate"));

	public static final StreamCodec<ByteBuf, UltimatePayload> STREAM_CODEC =
			StreamCodec.unit(new UltimatePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
