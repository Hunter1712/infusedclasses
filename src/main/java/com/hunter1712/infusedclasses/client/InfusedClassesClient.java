package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassConfirmedPayload;
import com.hunter1712.infusedclasses.OpenClassScreenPayload;
import com.hunter1712.infusedclasses.PassiveStacksPayload;
import com.hunter1712.infusedclasses.SelectClassPayload;
import com.hunter1712.infusedclasses.WarCryPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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
		// The confirm never touches the stacks store, so mid-fight rows survive it.
		ClientPlayNetworking.registerGlobalReceiver(ClassConfirmedPayload.TYPE,
				(ClassConfirmedPayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> {
							var mc = Minecraft.getInstance();
							if (mc.screen instanceof ClassSelectScreen) {
								mc.setScreen(null);
							}
						}));

		ClientPlayNetworking.registerGlobalReceiver(PassiveStacksPayload.TYPE,
				(PassiveStacksPayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> ClientPassiveStacks.setAll(payload.entries(), System.currentTimeMillis())));

		HudElementRegistry.attachElementAfter(VanillaHudElements.STATUS_EFFECTS,
				Identifier.fromNamespaceAndPath("infusedclasses", "passive_hud"),
				PassiveHudOverlay::render);

		warCryKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.infusedclasses.war_cry", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyMapping.Category.GAMEPLAY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}
			while (warCryKey.consumeClick()) {
				ClientPlayNetworking.send(new WarCryPayload());
			}
		});
	}

	public static void pickClass(Identifier id) {
		ClientPlayNetworking.send(new SelectClassPayload(id));
	}
}
