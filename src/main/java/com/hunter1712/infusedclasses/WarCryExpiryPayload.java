package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client War Cry clock: remaining Strength ticks, sent on fire and every extension proc
 * (plus zero on class switch, respawn, and edge-triggered on effect loss). The client interpolates
 * the countdown between messages; the overlay hides the row while no Strength effect is live as a backstop.
 */
public record WarCryExpiryPayload(int expiryTicks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WarCryExpiryPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "war_cry_expiry"));

	public static final StreamCodec<ByteBuf, WarCryExpiryPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WarCryExpiryPayload::expiryTicks,
			WarCryExpiryPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
