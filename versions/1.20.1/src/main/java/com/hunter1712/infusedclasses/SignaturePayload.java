package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Signature slot intent: "I pressed the signature bind". Server validates class and cooldown. */
public record SignaturePayload() implements FabricPacket {
	public static final PacketType<SignaturePayload> TYPE =
			PacketType.create(new ResourceLocation(ModIds.MOD_ID, "signature"),
					buf -> new SignaturePayload());

	@Override
	public void write(FriendlyByteBuf buf) {
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
