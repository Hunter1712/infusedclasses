package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client intent: "I pressed the Rush key". Server validates class and cooldown. */
public record RushPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RushPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "rush"));

	public static final StreamCodec<ByteBuf, RushPayload> STREAM_CODEC =
			StreamCodec.unit(new RushPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
