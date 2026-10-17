package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The status emission adapter: one last-sent snapshot per player, and a send only when
 * the snapshot decision says something a client cannot derive moved. Every gameplay
 * change point calls in — intent receivers after their branch, the melee settle, the
 * server tick after the decay-and-apply pass, an accepted class switch — so a rejected
 * press, a spam swing or a quiet tick costs no packets. Join and respawn force one so
 * a client always learns its class, classless included. Logout (and before a respawn's
 * forced send) drops the entry: the snapshot is session data.
 */
public final class StatusSync {
	private static final Map<UUID, PlayerStatusPayload> lastSent = new ConcurrentHashMap<>();

	private StatusSync() {
	}

	/** Rebuilds the snapshot and sends it only when anything discrete moved since the last send. */
	public static void maybeSend(ServerPlayer player, long now) {
		PlayerStatusPayload snapshot = snapshot(player, now);
		PlayerStatusPayload prev = lastSent.get(player.getUUID());
		if (!snapshot.movedSince(prev)) {
			return;
		}
		lastSent.put(player.getUUID(), snapshot);
		ServerPlayNetworking.send(player, snapshot);
	}

	/** Join and respawn: one snapshot unconditionally, so the client starts from a known state. */
	public static void sendNow(ServerPlayer player, long now) {
		PlayerStatusPayload snapshot = snapshot(player, now);
		lastSent.put(player.getUUID(), snapshot);
		ServerPlayNetworking.send(player, snapshot);
	}

	/** Logout, and the stale half of a respawn: the shared UUID's last-sent snapshot goes. */
	public static void forget(UUID id) {
		lastSent.remove(id);
	}

	private static PlayerStatusPayload snapshot(ServerPlayer player, long now) {
		ClassDefinition def = PlayerClassAccess.of(player).currentDefinition();
		return PlayerStatusPayload.of(def == null ? null : def.id(), InfusedClasses.runtime(player), now);
	}
}
