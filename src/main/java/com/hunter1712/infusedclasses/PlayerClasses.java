package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Per-player class state. One instance per player, supplied by the mixin. */
public class PlayerClasses {
	public static final String NBT_KEY = "InfusedClass";
	public static final String TREE_KEY = "InfusedTree";
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerClasses.class);

	private Identifier classId;
	// ponytail: cooldowns reset on relog, persist to NBT if the reset becomes an exploit.
	private final Cooldowns cooldowns = new Cooldowns();
	private final Map<String, SkillTrees.Progress> progressByClass = new LinkedHashMap<>();
	// ponytail: stacks are in-memory only; per-tick full clear past the timeout, per-stack decay only if combat feel demands it.
	private final SkillTrees.ThickSkin thickSkin = new SkillTrees.ThickSkin();

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

	/** Copies persisted choice plus tree progress to a fresh instance (respawn). Stacks never copy. */
	public void copyFrom(PlayerClasses other) {
		readId(other == null ? null : other.writeId());
		readTree(other == null ? null : other.writeTree());
		thickSkin.clear();
	}

	/** Progress for the current class, creating the per-class default on first use. Null when classless. */
	public SkillTrees.Progress progress() {
		if (classId == null) {
			return null;
		}
		SkillTrees.Progress p = progressByClass.get(classId.toString());
		if (p == null) {
			p = new SkillTrees.Progress();
			progressByClass.put(classId.toString(), p);
		}
		return p;
	}

	public int level() {
		SkillTrees.Progress p = progress();
		return p == null ? 1 : p.level;
	}

	public int unspentPoints() {
		SkillTrees.Progress p = progress();
		return p == null ? 0 : p.unspent;
	}

	public Set<String> unlockedNodes() {
		SkillTrees.Progress p = progress();
		return p == null ? Set.of() : p.unlocked;
	}

	public SkillTrees.ThickSkin thickSkin() {
		return thickSkin;
	}

	public String writeTree() {
		return SkillTrees.encodeMap(progressByClass);
	}

	public void readTree(String s) {
		progressByClass.clear();
		progressByClass.putAll(SkillTrees.decodeMap(s));
	}

	/** Adds XP, banking one point per level-up. Returns levels gained for toast/sound. Pure. */
	public int addKillXp(boolean meleeAffinity) {
		SkillTrees.Progress p = progress();
		if (p == null) {
			return 0;
		}
		p.xp += SkillTrees.xpForKill(meleeAffinity);
		int gained = 0;
		while (p.level < SkillTrees.MAX_LEVEL && p.xp >= SkillTrees.xpForNextLevel(p.level)) {
			p.xp -= SkillTrees.xpForNextLevel(p.level);
			p.level++;
			p.unspent++;
			gained++;
		}
		return gained;
	}

	/** Spends points on a node after class/gate/double-spend checks. Caller re-applies modifiers. Pure. */
	public boolean tryUnlock(String nodeId) {
		SkillTrees.Progress p = progress();
		if (p == null || !ClassRegistry.WARRIOR.equals(classId)) {
			return false;
		}
		SkillTrees.Node node = SkillTrees.byId(nodeId);
		if (node == null || !SkillTrees.canUnlock(p.unlocked, p.unspent, nodeId)) {
			return false;
		}
		p.unspent -= node.cost();
		p.unlocked.add(nodeId);
		return true;
	}

	/** Refunds all spent points. Caller re-applies base passives. Pure. */
	public int respec() {
		SkillTrees.Progress p = progress();
		if (p == null) {
			return 0;
		}
		int refund = SkillTrees.spentPoints(p.unlocked);
		p.unlocked.clear();
		p.unspent += refund;
		return refund;
	}

	public List<Passive> treePassives() {
		return SkillTrees.bonusesFor(progress() == null ? null : progress().unlocked);
	}

	/** Strips old modifiers first so switches never stack. Returns def, null rejects unknown and locked ids. */
	public ClassDefinition setClass(ServerPlayer player, Identifier id) {
		ClassDefinition def = trySetClass(id);
		if (def == null) {
			return null;
		}
		removeModifiers(player);
		thickSkin.clear();
		progress();
		applyModifiers(player, def, treePassives());
		return def;
	}

	/** Re-apply after join/respawn, where vanilla resets modifiers. */
	public void refresh(ServerPlayer player) {
		ClassDefinition def = currentDefinition();
		if (def != null) {
			removeModifiers(player);
			applyModifiers(player, def, treePassives());
		}
	}

	/** Live Thick Skin armor follows current stacks. */
	public void refreshThickSkin(ServerPlayer player) {
		AttributeInstance inst = instance(player, SkillTrees.THICK_SKIN_ATTRIBUTE);
		if (inst == null) {
			return;
		}
		inst.removeModifier(SkillTrees.THICK_SKIN_MODIFIER);
		int stacks = thickSkin.count();
		if (stacks > 0) {
			inst.addPermanentModifier(new AttributeModifier(SkillTrees.THICK_SKIN_MODIFIER,
					stacks * SkillTrees.THICK_SKIN_ARMOR_PER_STACK, AttributeModifier.Operation.ADD_VALUE));
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
		for (SkillTrees.Node n : SkillTrees.NODES) {
			if (n.bonus() == null) {
				continue;
			}
			AttributeInstance inst = instance(player, n.bonus().attribute());
			if (inst != null) {
				inst.removeModifier(n.bonus().modifier());
			}
		}
		AttributeInstance thick = instance(player, SkillTrees.THICK_SKIN_ATTRIBUTE);
		if (thick != null) {
			thick.removeModifier(SkillTrees.THICK_SKIN_MODIFIER);
		}
	}

	private static void applyModifiers(ServerPlayer player, ClassDefinition def, List<Passive> tree) {
		for (Passive p : def.passives()) {
			applyOne(player, def.id(), p);
		}
		if (tree != null) {
			for (Passive p : tree) {
				applyOne(player, def.id(), p);
			}
		}
	}

	private static void applyOne(ServerPlayer player, Identifier classId, Passive p) {
		AttributeInstance inst = instance(player, p.attribute());
		if (inst == null) {
			LOGGER.warn("Unknown attribute {} for class {}, skipping passive", p.attribute(), classId);
			return;
		}
		inst.removeModifier(p.modifier());
		inst.addPermanentModifier(new AttributeModifier(p.modifier(), p.value(), p.operation()));
	}

	private static AttributeInstance instance(ServerPlayer player, Identifier attribute) {
		Holder<Attribute> holder = ClassAttributes.resolve(attribute);
		return holder == null ? null : player.getAttribute(holder);
	}

	public interface Owner {
		PlayerClasses classes();
	}
}

