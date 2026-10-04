package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassConfirmedPayload;
import com.hunter1712.infusedclasses.OpenClassScreenPayload;
import com.hunter1712.infusedclasses.SelectClassPayload;
import com.hunter1712.infusedclasses.WarCryPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** Client: opens picker on server signal, sends class pick and War Cry key as intents. */
public class InfusedClassesClient implements ClientModInitializer {
	private static KeyMapping warCryKey;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(OpenClassScreenPayload.TYPE,
				(OpenClassScreenPayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> context.client().setScreen(new ClassSelectScreen())));

		// Picker closes only on server confirm, so a rejected pick stays open for retry.
		ClientPlayNetworking.registerGlobalReceiver(ClassConfirmedPayload.TYPE,
				(ClassConfirmedPayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> {
							if (Minecraft.getInstance().screen instanceof ClassSelectScreen) {
								Minecraft.getInstance().setScreen(null);
							}
						}));

		warCryKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.infusedclasses.war_cry", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyMapping.Category.GAMEPLAY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || warCryKey == null) {
				return;
			}
			while (warCryKey.consumeClick()) {
				ClientPlayNetworking.send(new WarCryPayload());
			}
		});
	}

	public static void pickClass(net.minecraft.resources.Identifier id) {
		ClientPlayNetworking.send(new SelectClassPayload(id));
	}
}
