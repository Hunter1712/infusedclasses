package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client intent: "I pressed the War Cry key". Server validates class and cooldown. */
public record WarCryPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WarCryPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "war_cry"));

	public static final StreamCodec<ByteBuf, WarCryPayload> STREAM_CODEC =
			StreamCodec.unit(new WarCryPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
