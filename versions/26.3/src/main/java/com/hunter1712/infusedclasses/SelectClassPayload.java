package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client intent: "I picked this class". Server validates and applies. */
public record SelectClassPayload(String classId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SelectClassPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ModIds.MOD_ID, "select_class"));

	public static final StreamCodec<ByteBuf, SelectClassPayload> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SelectClassPayload::classId, SelectClassPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

