package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All classes. Adding a class = one definition here. Warrior is active-ability-only plus
 * one conditional passive; War Cry is the damage tool, no flat melee bonus.
 */
public final class ClassRegistry {
	public static final Identifier WARRIOR = Identifier.fromNamespaceAndPath("infusedclasses", "warrior");
	public static final Identifier WARRIOR_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "war_cry");

	private static final Map<Identifier, ClassDefinition> BY_ID = new LinkedHashMap<>();

	static {
		register(new ClassDefinition(
				WARRIOR,
				"Warrior",
				false,
				List.of(new ConditionalPassive(
						"Bulwark",
						"on_hit_taken",
						"+1 Armor per stack, up to 10",
						"Taking a hit grants 1 stack (max 10). Each stack adds +1 armor. After 5s without a hit, lose 1 stack per second.",
						10)),
				WARRIOR_ABILITY,
				0xFFE74C3C,
				"Frontline bruiser",
				List.of("War Cry (R)", "Strength burst"),
				"Gives Strength II for 8s"));

		register(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "ranger"),
				"Ranger",
				true,
				List.of(),
				null,
				0xFF2ECC71,
				"Wilds archer",
				List.of(),
				""));

		register(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "mage"),
				"Mage",
				true,
				List.of(),
				null,
				0xFF9B59B6,
				"Arcane burst",
				List.of(),
				""));
	}

	private ClassRegistry() {
	}

	private static void register(ClassDefinition d) {
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
