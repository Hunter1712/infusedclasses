package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Per-player class state. One instance per player, supplied by the mixin. */
public class PlayerClasses {
	public static final String NBT_KEY = "InfusedClass";
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerClasses.class);

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

	/** Strips old modifiers first so switches never stack. Returns def, null rejects unknown and locked ids. */
	public ClassDefinition setClass(ServerPlayer player, Identifier id) {
		ClassDefinition def = trySetClass(id);
		if (def == null) {
			return null;
		}
		removeModifiers(player);
		applyModifiers(player, def);
		return def;
	}

	/** Re-apply after join/respawn, where vanilla resets modifiers. */
	public void refresh(ServerPlayer player) {
		ClassDefinition def = currentDefinition();
		if (def != null) {
			removeModifiers(player);
			applyModifiers(player, def);
		}
	}

	/** Removes only pairs this mod ever adds. Known list beats sweeping the whole attribute map. */
	private static void removeModifiers(ServerPlayer player) {
		for (ClassDefinition def : ClassRegistry.all()) {
			for (Passive p : def.passives()) {
				AttributeInstance inst = instance(player, p.attribute());
				if (inst != null) {
					inst.removeModifier(p.modifier());
				}
			}
		}
	}

	private static void applyModifiers(ServerPlayer player, ClassDefinition def) {
		for (Passive p : def.passives()) {
			AttributeInstance inst = instance(player, p.attribute());
			if (inst == null) {
				LOGGER.warn("Unknown attribute {} for class {}, skipping passive", p.attribute(), def.id());
				continue;
			}
			inst.removeModifier(p.modifier());
			inst.addPermanentModifier(new AttributeModifier(p.modifier(), p.value(), p.operation()));
		}
	}

	private static AttributeInstance instance(ServerPlayer player, Identifier attribute) {
		Holder<Attribute> holder = ClassAttributes.resolve(attribute);
		return holder == null ? null : player.getAttribute(holder);
	}

	public interface Owner {
		PlayerClasses classes();
	}
}

