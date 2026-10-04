package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** Per-player class state. One instance per player, supplied by the mixin. */
public class PlayerClasses {
	public static final String NBT_KEY = "InfusedClass";

	private Identifier classId;
	// ponytail: cooldowns reset on relog, persist to NBT if the reset becomes an exploit.
	private final Cooldowns cooldowns = new Cooldowns();

	public Identifier classId() {
		return classId;
	}

	public boolean hasClass() {
		return classId != null;
	}

	public Cooldowns cooldowns() {
		return cooldowns;
	}

	/** Mixin-backed accessor. Cast is safe: mixin implements Owner on every Player. */
	public static PlayerClasses of(Player player) {
		return ((Owner) player).classes();
	}

	public String writeId() {
		return classId == null ? null : classId.toString();
	}

	public void readId(String id) {
		Identifier parsed = id == null ? null : Identifier.tryParse(id);
		ClassDefinition def = parsed == null ? null : ClassRegistry.byId(parsed);
		classId = def == null ? null : def.id();
	}

	/** Pure current definition, null when no class. No live player needed. */
	public ClassDefinition currentDefinition() {
		return classId == null ? null : ClassRegistry.byId(classId);
	}

	/** Pure validation: unknown, missing, and locked ids yield null. No live player needed. */
	public ClassDefinition trySetClass(Identifier id) {
		ClassDefinition def = id == null ? null : ClassRegistry.byId(id);
		if (def == null || def.locked()) {
			return null;
		}
		classId = def.id();
		return def;
	}

	/** Copies persisted choice to a fresh instance (respawn). */
	public void copyFrom(PlayerClasses other) {
		readId(other == null ? null : other.writeId());
	}

	public interface Owner {
		PlayerClasses classes();
	}
}
