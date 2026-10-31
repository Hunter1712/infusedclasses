package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

/** Signature slot intent: "I pressed the signature bind". Server validates class and cooldown. */
public record RushPayload() implements FabricPacket {
	public static final PacketType<RushPayload> TYPE =
			PacketType.create(new ResourceLocation(InfusedClasses.MOD_ID, "rush"),
					buf -> new RushPayload());

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
