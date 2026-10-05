package com.hunter1712.infusedclasses;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The transient half's lifetime, in one module. The server entry holds one of these instead
 * of a bare map, so create/drop/respawn ordering lives behind an interface tests can reach
 * without a player: one per UUID, created on first use, empty again after a respawn, dropped
 * on logout. Nothing here is persisted.
 */
public final class PlayerRuntimes {
	private final ConcurrentMap<UUID, PlayerRuntime> backing = new ConcurrentHashMap<>();

	public PlayerRuntime runtime(UUID id) {
		return backing.computeIfAbsent(id, key -> new PlayerRuntime());
	}

	public void remove(UUID id) {
		backing.remove(id);
	}

	/**
	 * Respawn: both bodies share one UUID, so the old entry goes before the new one is
	 * created. The other order would hand the respawned player the stacks their old body
	 * earned. Starts empty, which is what a respawn has always done to transient state.
	 */
	public void respawn(UUID id) {
		backing.remove(id);
		runtime(id);
	}

	int size() {
		return backing.size();
	}
}
