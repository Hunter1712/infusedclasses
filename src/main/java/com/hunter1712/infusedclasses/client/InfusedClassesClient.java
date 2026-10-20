package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassConfirmedPayload;
import com.hunter1712.infusedclasses.OpenClassScreenPayload;
import com.hunter1712.infusedclasses.PlayerStatusPayload;
import com.hunter1712.infusedclasses.RendPayload;
import com.hunter1712.infusedclasses.SelectClassPayload;
import com.hunter1712.infusedclasses.RushPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** Client: opens picker on server signal, sends class pick and ability keys as intents. */
public class InfusedClassesClient implements ClientModInitializer {
	private static KeyMapping signatureKey;
	private static KeyMapping ultimateKey;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(OpenClassScreenPayload.TYPE,
				(OpenClassScreenPayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> context.client().setScreen(new ClassSelectScreen(payload.currentClassId()))));

		// Picker closes only on server confirm, so a rejected pick stays open for retry.
		ClientPlayNetworking.registerGlobalReceiver(ClassConfirmedPayload.TYPE,
				(ClassConfirmedPayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> {
							var mc = Minecraft.getInstance();
							if (mc.screen instanceof ClassSelectScreen) {
								mc.setScreen(null);
							}
						}));

		ClientPlayNetworking.registerGlobalReceiver(PlayerStatusPayload.TYPE,
				(PlayerStatusPayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> StatusStore.accept(payload, now())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> StatusStore.clear());

		// The stack on screen: one element under its own id; what draws and what hides lives inside it.
		HudElementRegistry.addLast(StatusHudElement.ID, new StatusHudElement());

		signatureKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.infusedclasses.signature", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyMapping.Category.GAMEPLAY));
		ultimateKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.infusedclasses.ultimate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KeyMapping.Category.GAMEPLAY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}
			while (signatureKey.consumeClick()) {
				ClientPlayNetworking.send(new RushPayload());
			}
			while (ultimateKey.consumeClick()) {
				ClientPlayNetworking.send(new RendPayload());
			}
		});
	}

	public static void pickClass(Identifier id) {
		ClientPlayNetworking.send(new SelectClassPayload(id));
	}

	/** Monotonic millis for the HUD's local interpolation anchor; no clock crosses the wire. */
	static long now() {
		return System.nanoTime() / 1_000_000L;
	}
}
