package com.hunter1712.infusedclasses;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Class-pick protocol hub. One place for the four channels and their direction:
 * server sends open on first join, client sends pick and war-cry intents,
 * server sends confirm as the verdict that alone closes the picker.
 */
public final class ClassPackets {
	public static final String MOD_ID = "infusedclasses";

	public static final CustomPacketPayload.Type<SelectClassPayload> SELECT_CLASS =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "select_class"));
	public static final CustomPacketPayload.Type<OpenClassScreenPayload> OPEN_CLASS_SCREEN =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "open_class_screen"));
	public static final CustomPacketPayload.Type<WarCryPayload> WAR_CRY =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "war_cry"));
	public static final CustomPacketPayload.Type<ClassConfirmedPayload> CLASS_CONFIRMED =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "class_confirmed"));

	private ClassPackets() {
	}
}
