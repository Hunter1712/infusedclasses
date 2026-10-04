package com.hunter1712.infusedclasses;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client intent: "spend a point on this talent". Server validates gates and points. */
public record SpendTalentPayload(String nodeId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpendTalentPayload> TYPE = ClassPackets.SPEND_TALENT;

	public static final StreamCodec<ByteBuf, SpendTalentPayload> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SpendTalentPayload::nodeId, SpendTalentPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
