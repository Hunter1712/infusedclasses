package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Ultimate slot intent: "I pressed the ultimate bind". Server validates class, stacks and cooldown. */
public record UltimatePayload() implements FabricPacket {
	public static final PacketType<UltimatePayload> TYPE =
			PacketType.create(new ResourceLocation(ModIds.MOD_ID, "ultimate"),
					buf -> new UltimatePayload());

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
