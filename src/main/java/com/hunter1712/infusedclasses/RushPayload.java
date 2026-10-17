package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Signature slot intent: "I pressed the signature bind". Server validates class and cooldown. */
public record RushPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RushPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "rush"));

	public static final StreamCodec<ByteBuf, RushPayload> STREAM_CODEC =
			StreamCodec.unit(new RushPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
