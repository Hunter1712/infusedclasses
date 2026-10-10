package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server tells client to open the picker, carrying the player's current class id
 * (empty when none) so the picker opens on the current class page when reopened.
 * Sent once on first join when the player has no class, and on demand by the
 * select command.
 */
public record OpenClassScreenPayload(String currentClassId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenClassScreenPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "open_class_screen"));

	public static final StreamCodec<ByteBuf, OpenClassScreenPayload> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.STRING_UTF8, OpenClassScreenPayload::currentClassId,
					OpenClassScreenPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
