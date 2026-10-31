package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Client intent: "I picked this class". Server validates and applies. */
public record SelectClassPayload(ResourceLocation classId) implements FabricPacket {
	public static final PacketType<SelectClassPayload> TYPE =
			PacketType.create(new ResourceLocation(InfusedClasses.MOD_ID, "select_class"),
					buf -> new SelectClassPayload(buf.readResourceLocation()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(classId);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
