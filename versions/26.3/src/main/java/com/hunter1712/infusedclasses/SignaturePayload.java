package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Signature slot intent: "I pressed the signature bind". Server validates class and cooldown. */
public record SignaturePayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SignaturePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ModIds.MOD_ID, "signature"));

	public static final StreamCodec<ByteBuf, SignaturePayload> STREAM_CODEC =
			StreamCodec.unit(new SignaturePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
