package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry-side signals: the one place a stacks or War Cry clock packet goes out. The server entry
 * decides through pure modules and applies through adapters; sending the resulting signal stays
 * here, next to the entry, so receivers and tick flows never build packets inline.
 */
final class ServerSignals {
	private ServerSignals() {
	}

	/** Switch clear: zero-stack entries for both sides, so no old row sticks around. */
	static void sendStacksForSwitch(ServerPlayer player, ClassDefinition oldDef, ClassDefinition newDef,
			PlayerRuntime runtime) {
		long now = System.currentTimeMillis();
		List<PassiveStacksPayload.Entry> out = new ArrayList<>();
		if (oldDef != null) {
			out.addAll(PassiveStacksPayload.entriesFor(oldDef, runtime, now));
		}
		if (newDef != null) {
			out.addAll(PassiveStacksPayload.entriesFor(newDef, runtime, now));
		}
		if (!out.isEmpty()) {
			ServerPlayNetworking.send(player, new PassiveStacksPayload(out));
		}
	}

	/** One stacks signal, only for players holding a class. Classless players cost no packet. */
	static void sendStacks(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		if (def == null) {
			return;
		}
		ServerPlayNetworking.send(player, new PassiveStacksPayload(PassiveStacksPayload.entriesFor(def, runtime, now)));
	}

	/** One War Cry clock signal. Fire and every proc feed it; switch and respawn zero it. */
	static void sendWarCry(ServerPlayer player, int expiryTicks) {
		ServerPlayNetworking.send(player, new WarCryExpiryPayload(expiryTicks));
	}
}
