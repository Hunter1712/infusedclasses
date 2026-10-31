package com.hunter1712.infusedclasses;


/**
 * The class one player has chosen: validation, and the NBT round-trip that carries it across a
 * relog. Persisted choice and nothing else - no clock reads and no transient state - so this half
 * of per-player state is obviously pure. One instance per player, supplied by the mixin.
 *
 * Transient per-player state (passive stacks, cooldowns) lives in {@link PlayerRuntime}, which
 * the server entry holds per player instead.
 */
public final class PlayerClasses {
	public static final String NBT_KEY = "InfusedClass";

	private String classId;

	public String classId() {
		return classId;
	}

	public boolean hasClass() {
		return classId != null;
	}

	public String writeId() {
		return classId;
	}

	public void readId(String id) {
		ClassDefinition def = id == null ? null : ClassRegistry.byId(id);
		classId = def == null ? null : def.id();
	}

	/** Pure current definition, null when no class. No live player needed. */
	public ClassDefinition currentDefinition() {
		return classId == null ? null : ClassRegistry.byId(classId);
	}

	/** Pure validation: unknown and missing ids yield null. No live player needed. */
	public ClassDefinition trySetClass(String id) {
		ClassDefinition def = id == null ? null : ClassRegistry.byId(id);
		if (def == null) {
			return null;
		}
		classId = def.id();
		return def;
	}

	/** Copies the persisted choice to a fresh instance (respawn). The choice is all it copies. */
	public void copyFrom(PlayerClasses other) {
		classId = other == null ? null : other.classId;
	}
}
