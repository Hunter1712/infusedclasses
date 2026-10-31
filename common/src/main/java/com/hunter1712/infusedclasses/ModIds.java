package com.hunter1712.infusedclasses;

/**
 * The mod's own names, shared by the decisions and the adapters so neither spells them
 * twice: the namespace every id builds on, and the one rejection line a class choice
 * says. Plain strings, so a decision can name an id without a client or a registry.
 */
public final class ModIds {
	/** Mod namespace, owned here: the one field every id, payload type and modifier id builds on. */
	public static final String MOD_ID = "infusedclasses";

	/** Unknown or locked class rejection. */
	public static final String UNKNOWN_OR_LOCKED = "Unknown or locked class";

	private ModIds() {
	}
}
