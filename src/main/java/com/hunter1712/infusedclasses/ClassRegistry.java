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
 * All classes. Adding a class = one definition here. Warrior carries Second Wind
 * plus one conditional passive (Bulwark) and the Berserk HP-scaled pair.
 */
public final class ClassRegistry {
	public static final Identifier WARRIOR = Identifier.fromNamespaceAndPath("infusedclasses", "warrior");
	public static final Identifier SECOND_WIND_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "second_wind");
	/** ponytail: attribute ids rather than vanilla holders, so the passive stays bootstrap-free. */
	public static final ResourceKey<Attribute> ARMOR =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("armor"));
	public static final ResourceKey<Attribute> ATTACK_DAMAGE =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("attack_damage"));
	public static final ResourceKey<Attribute> ATTACK_SPEED =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("attack_speed"));

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
				List.of(
						new HpScaledPassive(
								"Berserk Damage",
								ATTACK_DAMAGE,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "berserk_damage"),
								0.4),
						new HpScaledPassive(
								"Berserk Speed",
								ATTACK_SPEED,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "berserk_speed"),
								0.08)),
				SECOND_WIND_ABILITY,
				0xFFE74C3C,
				"Frontline bruiser",
				List.of(
						new ClassDefinition.ActiveAbility(
								SecondWindAbility.NAME,
								"Heals " + SecondWindAbility.HP_PER_STACK
										+ " HP per Bulwark stack, consumes stacks (G, "
										+ (Cooldowns.SECOND_WIND_MS / 1000) + "s)."))));

		register(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "ranger"),
				"Ranger",
				true,
				List.of(),
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
				List.of(),
				null,
				0xFF9B59B6,
				"Arcane burst",
				List.of()));
	}

	private ClassRegistry() {
	}

	private static void register(ClassDefinition d) {
		if (d.locked()) {
			if (d.activeAbility() != null || !d.passives().isEmpty() || !d.hpPassives().isEmpty()) {
				throw new IllegalArgumentException("Locked class " + d.id() + " must carry no ability and no passives");
			}
			if (!d.actives().isEmpty()) {
				throw new IllegalArgumentException("Locked class " + d.id() + " must carry no actives");
			}
		} else if (d.activeAbility() == null) {
			throw new IllegalArgumentException("Playable class " + d.id() + " must carry an ability");
		} else if (d.actives().isEmpty()) {
			throw new IllegalArgumentException("Playable class " + d.id() + " must carry at least one active");
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
