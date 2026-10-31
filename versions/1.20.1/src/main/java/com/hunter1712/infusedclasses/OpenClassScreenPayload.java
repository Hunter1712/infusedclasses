package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server tells client to open the picker, carrying the player's current class id
 * (empty when none) so the picker opens on the current class page when reopened.
 * Sent once on first join when the player has no class, and on demand by the
 * select command.
 */
public record OpenClassScreenPayload(String currentClassId) implements FabricPacket {
	public static final PacketType<OpenClassScreenPayload> TYPE =
			PacketType.create(new ResourceLocation(InfusedClasses.MOD_ID, "open_class_screen"),
					buf -> new OpenClassScreenPayload(buf.readUtf()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(currentClassId);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
