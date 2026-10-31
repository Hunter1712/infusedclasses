package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Ultimate slot intent: "I pressed the ultimate bind". Server validates class, stacks and cooldown. */
public record RendPayload() implements FabricPacket {
	public static final PacketType<RendPayload> TYPE =
			PacketType.create(new ResourceLocation(InfusedClasses.MOD_ID, "rend"),
					buf -> new RendPayload());

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
