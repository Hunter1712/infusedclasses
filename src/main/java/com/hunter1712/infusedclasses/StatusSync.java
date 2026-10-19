package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The status emission adapter: one last-sent snapshot per player, and a send only when
 * the snapshot decision says something a client cannot derive moved. The server tick is
 * the one steady-state change point — a fire, a spend, a stack decay, a switch and a
 * melee settle all rebuild-and-compare there within a tick — so a rejected press and a
 * quiet tick cost no packets. Join and respawn force one so a client always learns its
 * class, classless included. Logout (and before a respawn's forced send) drops the
 * entry: the snapshot is session data.
 */
public final class StatusSync {
	private static final Map<UUID, PlayerStatusPayload> lastSent = new ConcurrentHashMap<>();

	private StatusSync() {
	}

	/** Sends the tick's snapshot only when anything discrete moved since the last send. */
	public static void maybeSend(ServerPlayer player, PlayerStatusPayload snapshot) {
		PlayerStatusPayload prev = lastSent.get(player.getUUID());
		if (!snapshot.movedSince(prev)) {
			return;
		}
		lastSent.put(player.getUUID(), snapshot);
		ServerPlayNetworking.send(player, snapshot);
	}

	/** Join and respawn: one snapshot unconditionally, so the client starts from a known state. */
	public static void sendNow(ServerPlayer player, long now) {
		ClassDefinition def = PlayerClassAccess.of(player).currentDefinition();
		// A classless player materialises no runtime: the empty shape is the truth.
		PlayerRuntime runtime = def == null ? null : InfusedClasses.runtime(player);
		PlayerStatusPayload snapshot = snapshot(def, runtime, runtime == null ? null : runtime.state(now), now);
		lastSent.put(player.getUUID(), snapshot);
		ServerPlayNetworking.send(player, snapshot);
	}

	/** Logout, and the stale half of a respawn: the shared UUID's last-sent snapshot goes. */
	public static void forget(UUID id) {
		lastSent.remove(id);
	}

	/** The snapshot rule in one place: a classless player is the empty shape, no runtime read. */
	private static PlayerStatusPayload snapshot(ClassDefinition def, PlayerRuntime runtime,
			PlayerRuntime.State state, long now) {
		return def == null ? PlayerStatusPayload.classless() : PlayerStatusPayload.of(def, runtime, state, now);
	}
}
