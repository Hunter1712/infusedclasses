package com.hunter1712.infusedclasses.client;

import com.hunter1712.infusedclasses.ClassConfirmedPayload;
import com.hunter1712.infusedclasses.ClassPackets;
import com.hunter1712.infusedclasses.OpenClassScreenPayload;
import com.hunter1712.infusedclasses.RequestTalentsPayload;
import com.hunter1712.infusedclasses.RespecTalentsPayload;
import com.hunter1712.infusedclasses.SelectClassPayload;
import com.hunter1712.infusedclasses.SpendTalentPayload;
import com.hunter1712.infusedclasses.TalentStatePayload;
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
	private static KeyMapping talentsKey;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ClassPackets.OPEN_CLASS_SCREEN,
				(OpenClassScreenPayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> context.client().setScreen(new ClassSelectScreen())));

		// Picker closes only on server confirm, so a rejected pick stays open for retry.
		ClientPlayNetworking.registerGlobalReceiver(ClassPackets.CLASS_CONFIRMED,
				(ClassConfirmedPayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> {
							if (Minecraft.getInstance().screen instanceof ClassSelectScreen) {
								Minecraft.getInstance().setScreen(null);
							}
						}));

		// Talent state is only ever sent when the player asked (key, command, spend),
		// so receipt always (re)opens the panel with fresh server-validated numbers.
		ClientPlayNetworking.registerGlobalReceiver(ClassPackets.TALENT_STATE,
				(TalentStatePayload payload, ClientPlayNetworking.Context context) -> context.client()
						.execute(() -> Minecraft.getInstance().setScreen(new TalentScreen(payload.state()))));

		warCryKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.infusedclasses.war_cry", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyMapping.Category.GAMEPLAY));
		talentsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.infusedclasses.talents", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyMapping.Category.GAMEPLAY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || warCryKey == null) {
				return;
			}
			while (warCryKey.consumeClick()) {
				ClientPlayNetworking.send(new WarCryPayload());
			}
			if (talentsKey != null) {
				while (talentsKey.consumeClick()) {
					ClientPlayNetworking.send(new RequestTalentsPayload());
				}
			}
		});
	}

	public static void pickClass(net.minecraft.resources.Identifier id) {
		ClientPlayNetworking.send(new SelectClassPayload(id));
	}

	public static void spendTalent(String nodeId) {
		ClientPlayNetworking.send(new SpendTalentPayload(nodeId));
	}

	public static void respecTalents() {
		ClientPlayNetworking.send(new RespecTalentsPayload());
	}
}

