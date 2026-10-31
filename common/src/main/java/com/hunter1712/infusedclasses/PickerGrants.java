package com.hunter1712.infusedclasses;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The transient permission to send one class pick. The server grants it when it opens
 * the picker; the receiver clears it only after a switch lands. Not saved and not part
 * of a player's runtime: a relog or respawn must not carry a stale pick.
 */
final class PickerGrants {
	private static final Set<UUID> GRANTED = new HashSet<>();

	private PickerGrants() {
	}

	static void grant(UUID id) {
		GRANTED.add(id);
	}

	static boolean isGranted(UUID id) {
		return GRANTED.contains(id);
	}

	static void clear(UUID id) {
		GRANTED.remove(id);
	}
}
