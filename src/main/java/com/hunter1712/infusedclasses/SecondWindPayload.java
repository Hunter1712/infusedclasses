package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client intent: "I pressed the Second Wind key". Server validates class, stacks and cooldown. */
public record SecondWindPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SecondWindPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "second_wind"));

	public static final StreamCodec<ByteBuf, SecondWindPayload> STREAM_CODEC =
			StreamCodec.unit(new SecondWindPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
