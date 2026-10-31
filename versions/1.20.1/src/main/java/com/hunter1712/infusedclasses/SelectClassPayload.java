package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

/** Client intent: "I picked this class". Server validates and applies. */
public record SelectClassPayload(String classId) implements FabricPacket {
	public static final PacketType<SelectClassPayload> TYPE =
			PacketType.create(new ResourceLocation(ModIds.MOD_ID, "select_class"),
					buf -> new SelectClassPayload(buf.readUtf()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(classId);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
