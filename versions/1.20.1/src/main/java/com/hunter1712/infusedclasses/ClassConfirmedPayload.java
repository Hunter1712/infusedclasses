package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

/** Server confirms the pick. Client closes the picker only on this, so rejected picks stay for retry. */
public record ClassConfirmedPayload() implements FabricPacket {
	public static final PacketType<ClassConfirmedPayload> TYPE =
			PacketType.create(new ResourceLocation(InfusedClasses.MOD_ID, "class_confirmed"),
					buf -> new ClassConfirmedPayload());

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
