package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Per-player class state. One instance per player, supplied by the mixin. */
public class PlayerClasses {
	public static final String NBT_KEY = "InfusedClass";

	private Identifier classId;
	// ponytail: cooldowns reset on relog, persist to NBT if the reset becomes an exploit.
	private final Cooldowns cooldowns = new Cooldowns();
	// Transient conditional-passive stacks: keyed by passive name, never written to NBT.
	private final Map<String, Integer> passiveStacks = new HashMap<>();

	public Identifier classId() {
		return classId;
	}

	public boolean hasClass() {
		return classId != null;
	}

	public Cooldowns cooldowns() {
		return cooldowns;
	}

	/** Current stacks for a passive, 0 when never triggered. */
	public int stacks(String passiveName) {
		return passiveStacks.getOrDefault(passiveName, 0);
	}

	/** String-keyed trigger helper: bumps every passive on the current class whose trigger matches.
	 *  Unknown triggers and trigger-less classes are inert. */
	public void onTrigger(String trigger) {
		ClassDefinition def = currentDefinition();
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			if (passive.trigger().equals(trigger)) {
				passiveStacks.put(passive.name(), Math.min(passive.maxStacks(), stacks(passive.name()) + 1));
			}
		}
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
