package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All classes. Adding a class = one definition here. Warrior numbers per spec: +8 health (4 hearts),
 * +4 armor, +2 toughness, -10% speed. War Cry is the damage tool, no flat melee bonus.
 */
public final class ClassRegistry {
	public static final Identifier WARRIOR = Identifier.fromNamespaceAndPath("infusedclasses", "warrior");
	public static final Identifier WARRIOR_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "war_cry");

	public static final double WARRIOR_HEALTH_BONUS = 8.0;
	public static final double WARRIOR_ARMOR_BONUS = 4.0;
	public static final double WARRIOR_TOUGHNESS_BONUS = 2.0;
	public static final double WARRIOR_SPEED_PENALTY = -0.1;

	private static final Map<Identifier, ClassDefinition> BY_ID = new LinkedHashMap<>();

	static {
		register(new ClassDefinition(
				WARRIOR,
				"Warrior",
				false,
				List.of(
						new Passive(minecraftId("max_health"), warriorModifier("health"), WARRIOR_HEALTH_BONUS, AttributeModifier.Operation.ADD_VALUE),
						new Passive(minecraftId("armor"), warriorModifier("armor"), WARRIOR_ARMOR_BONUS, AttributeModifier.Operation.ADD_VALUE),
						new Passive(minecraftId("armor_toughness"), warriorModifier("toughness"), WARRIOR_TOUGHNESS_BONUS, AttributeModifier.Operation.ADD_VALUE),
						new Passive(minecraftId("movement_speed"), warriorModifier("speed"), WARRIOR_SPEED_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
				WARRIOR_ABILITY,
				0xFFE74C3C,
				"Frontline bruiser",
				List.of("War Cry (R)", "Strength burst")));

		register(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "ranger"),
				"Ranger",
				true,
				List.of(),
				null,
				0xFF2ECC71,
				"Wilds archer",
				List.of()));

		register(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "mage"),
				"Mage",
				true,
				List.of(),
				null,
				0xFF9B59B6,
				"Arcane burst",
				List.of()));
	}

	private ClassRegistry() {
	}

	private static Identifier minecraftId(String path) {
		return Identifier.fromNamespaceAndPath("minecraft", path);
	}

	private static Identifier warriorModifier(String suffix) {
		return Identifier.fromNamespaceAndPath("infusedclasses", "warrior_" + suffix);
	}

	private static void register(ClassDefinition d) {
		for (Passive p : d.passives()) {
			if (!ClassAttributes.isKnown(p.attribute())) {
				throw new IllegalArgumentException("Unknown attribute " + p.attribute() + " for class " + d.id());
			}
		}
		if (d.locked()) {
			if (d.activeAbility() != null || !d.passives().isEmpty()) {
				throw new IllegalArgumentException("Locked class " + d.id() + " must carry no ability and no passives");
			}
		} else if (d.activeAbility() == null) {
			throw new IllegalArgumentException("Playable class " + d.id() + " must carry an ability");
		}
		BY_ID.put(d.id(), d);
	}

	public static Collection<ClassDefinition> all() {
		return Collections.unmodifiableCollection(BY_ID.values());
	}

	public static ClassDefinition byId(Identifier id) {
		if (id == null) {
			return null;
		}
		return BY_ID.get(id);
	}
}
