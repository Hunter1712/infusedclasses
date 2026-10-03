package dev.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

	/** Strips old modifiers first so switches never stack. Returns def, null rejects unknown and locked ids. */
	public ClassDefinition setClass(ServerPlayer player, Identifier id) {
		ClassDefinition def = id == null ? null : ClassRegistry.byId(id);
		if (def == null || def.locked()) {
			return null;
		}
		removeModifiers(player);
		classId = id;
		applyModifiers(player, def);
		return def;
	}

	/** Re-apply after join/respawn, where vanilla resets modifiers. */
	public void refresh(ServerPlayer player) {
		if (classId == null) {
			return;
		}
		ClassDefinition def = ClassRegistry.byId(classId);
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
		Holder<Attribute> holder = holder(attribute);
		return holder == null ? null : player.getAttribute(holder);
	}

	/** Vanilla attribute lookup without touching the registry at test time. Null for unknown. */
	static Holder<Attribute> holder(Identifier id) {
		if (!"minecraft".equals(id.getNamespace())) {
			return null;
		}
		return switch (id.getPath()) {
			case "max_health" -> Attributes.MAX_HEALTH;
			case "armor" -> Attributes.ARMOR;
			case "armor_toughness" -> Attributes.ARMOR_TOUGHNESS;
			case "movement_speed" -> Attributes.MOVEMENT_SPEED;
			default -> null;
		};
	}

	public interface Owner {
		PlayerClasses classes();
	}
}
