package dev.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All classes. Adding a class = one line here. Warrior numbers per spec: +8 health (4 hearts),
 * +4 armor, +2 toughness, -10% speed. War Cry is the damage tool, no flat melee bonus.
 */
public final class ClassRegistry {
	public static final Identifier WARRIOR = Identifier.fromNamespaceAndPath("infusedclasses", "warrior");

	private static final Map<Identifier, ClassDefinition> BY_ID = new LinkedHashMap<>();

	static {
		register(new ClassDefinition(
				WARRIOR,
				"Warrior",
				false,
				List.of(
						new Passive(minecraftId("max_health"), warriorModifier("health"), 8.0, AttributeModifier.Operation.ADD_VALUE),
						new Passive(minecraftId("armor"), warriorModifier("armor"), 4.0, AttributeModifier.Operation.ADD_VALUE),
						new Passive(minecraftId("armor_toughness"), warriorModifier("toughness"), 2.0, AttributeModifier.Operation.ADD_VALUE),
						new Passive(minecraftId("movement_speed"), warriorModifier("speed"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
				Identifier.fromNamespaceAndPath("infusedclasses", "war_cry")));

		for (String name : List.of("ranger", "mage")) {
			Identifier id = Identifier.fromNamespaceAndPath("infusedclasses", name);
			register(new ClassDefinition(id, capitalize(name), true, List.of(),
					Identifier.fromNamespaceAndPath("infusedclasses", name + "_ability")));
		}
	}

	private ClassRegistry() {
	}

	private static Identifier minecraftId(String path) {
		return Identifier.fromNamespaceAndPath("minecraft", path);
	}

	private static Identifier warriorModifier(String suffix) {
		return Identifier.fromNamespaceAndPath("infusedclasses", "warrior_" + suffix);
	}

	private static String capitalize(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	private static void register(ClassDefinition d) {
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
