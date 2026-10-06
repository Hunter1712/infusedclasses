package com.hunter1712.infusedclasses;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All classes. Adding a class = one definition here. Warrior carries War Cry plus Second Wind
 * on separate cooldown lanes and one conditional passive (Bulwark); War Cry stays the damage
 * tool, no flat melee bonus.
 */
public final class ClassRegistry {
	public static final Identifier WARRIOR = Identifier.fromNamespaceAndPath("infusedclasses", "warrior");
	public static final Identifier WARRIOR_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "war_cry");
	/** ponytail: attribute ids rather than vanilla holders, so the passive stays bootstrap-free. */
	public static final ResourceKey<Attribute> ARMOR =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("armor"));

	private static final Map<Identifier, ClassDefinition> BY_ID = new LinkedHashMap<>();

	static {
		register(new ClassDefinition(
				WARRIOR,
				"Warrior",
				false,
				List.of(new ConditionalPassive(
						"Bulwark",
						ConditionalPassive.ON_HIT_TAKEN,
						10,
						ARMOR,
						Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "bulwark_armor"),
						1.0,
						5)),
				WARRIOR_ABILITY,
				0xFFE74C3C,
				"Frontline bruiser",
				"War Cry + Second Wind",
				"Gives Strength II for " + (WarCryAbility.EFFECT_TICKS / 20) + "s (R, "
						+ (Cooldowns.WAR_CRY_MS / 1000) + "s). Charged hits extend +"
						+ (WarCryExtension.EXTEND_TICKS / 20) + "s at "
						+ (int) (WarCryExtension.PROC_RATE * 100) + "% per victim, no cap. Second Wind (G, "
						+ (Cooldowns.SECOND_WIND_MS / 1000) + "s): heals " + SecondWindAbility.HP_PER_STACK
						+ " HP per Bulwark stack, consumes stacks."));

		register(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "ranger"),
				"Ranger",
				true,
				List.of(),
				null,
				0xFF2ECC71,
				"Wilds archer",
				null,
				null));

		register(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "mage"),
				"Mage",
				true,
				List.of(),
				null,
				0xFF9B59B6,
				"Arcane burst",
				null,
				null));
	}

	private ClassRegistry() {
	}

	private static void register(ClassDefinition d) {
		if (d.locked()) {
			if (d.activeAbility() != null || !d.passives().isEmpty()) {
				throw new IllegalArgumentException("Locked class " + d.id() + " must carry no ability and no passives");
			}
			if (d.abilityName() != null || d.abilityDesc() != null) {
				throw new IllegalArgumentException("Locked class " + d.id() + " must carry no ability name or description");
			}
		} else if (d.activeAbility() == null) {
			throw new IllegalArgumentException("Playable class " + d.id() + " must carry an ability");
		} else if (d.abilityName() == null || d.abilityName().isBlank()) {
			throw new IllegalArgumentException("Playable class " + d.id() + " must carry an ability name");
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
