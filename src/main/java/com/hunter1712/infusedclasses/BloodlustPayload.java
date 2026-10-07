package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client intent: "I pressed the Bloodlust key". Server validates class, health gate and cooldown. */
public record BloodlustPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BloodlustPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "bloodlust"));

	public static final StreamCodec<ByteBuf, BloodlustPayload> STREAM_CODEC =
			StreamCodec.unit(new BloodlustPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
