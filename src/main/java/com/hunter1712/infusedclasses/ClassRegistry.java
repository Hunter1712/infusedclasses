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
	public static final Identifier WAR_CRY_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "war_cry");
	public static final Identifier SECOND_WIND_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "second_wind");
	/** ponytail: attribute ids rather than vanilla holders, so the passive stays bootstrap-free. */
	public static final ResourceKey<Attribute> ARMOR =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("armor"));
	public static final ResourceKey<Attribute> ATTACK_DAMAGE =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("attack_damage"));
	public static final ResourceKey<Attribute> ATTACK_SPEED =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("attack_speed"));
	public static final ResourceKey<Attribute> MAX_HEALTH =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("max_health"));
	public static final Identifier BONUS_HEALTH_ID =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "bonus_health");

	private static final Map<Identifier, ClassDefinition> BY_ID = new LinkedHashMap<>();

	static {
		register(new ClassDefinition(
				WARRIOR,
				"Warrior",
				false,
				List.of(new ConditionalPassive(
						"Bulwark",
						ConditionalPassive.ON_HIT_TAKEN,
						20,
						ARMOR,
						Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "bulwark_armor"),
						0.5,
						5)),
				List.of(
						new HpScaledPassive(
								"Berserk Damage",
								ATTACK_DAMAGE,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "berserk_damage"),
								0.3,
								"Berserk"),
						new HpScaledPassive(
								"Berserk Speed",
								ATTACK_SPEED,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "berserk_speed"),
								0.05,
								"Berserk")),
				0.0,
				List.of(WAR_CRY_ABILITY, SECOND_WIND_ABILITY),
				0xFFE74C3C,
				"Frontline bruiser",
				List.of(
						new ClassDefinition.ActiveAbility(
								WarCryAbility.NAME,
								"Weakens and slows hostiles within " + (int) WarCryAbility.RADIUS_BLOCKS
										+ " blocks for " + (WarCryAbility.EFFECT_TICKS / 20) + "s (R, "
										+ (Cooldowns.WAR_CRY_MS / 1000) + "s). Grants "
										+ WarCryAbility.BUILDER_STACKS + " Bulwark. Roots you for "
										+ (Spellcast.SLOW_TICKS / 20) + "s."),
						new ClassDefinition.ActiveAbility(
								SecondWindAbility.NAME,
								"Shields " + SecondWindAbility.HP_PER_STACK
										+ " HP per Bulwark stack for " + (SecondWindAbility.ABSORPTION_TICKS / 20)
										+ "s, consumes stacks (G, "
										+ (Cooldowns.SECOND_WIND_MS / 1000) + "s). Roots you for "
										+ (Spellcast.SLOW_TICKS / 20) + "s."))));

		register(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "ranger"),
				"Ranger",
				true,
				List.of(),
				List.of(),
				0.0,
				List.of(),
				0xFF2ECC71,
				"Wilds archer",
				List.of()));

		register(new ClassDefinition(
				Identifier.fromNamespaceAndPath("infusedclasses", "mage"),
				"Mage",
				true,
				List.of(),
				List.of(),
				0.0,
				List.of(),
				0xFF9B59B6,
				"Arcane burst",
				List.of()));
	}

	private ClassRegistry() {
	}

	private static void register(ClassDefinition d) {
		if (d.locked()) {
			if (!d.activeAbilities().isEmpty() || !d.passives().isEmpty() || !d.hpPassives().isEmpty()
					|| d.maxHealthBonus() != 0) {
				throw new IllegalArgumentException("Locked class " + d.id() + " must carry no abilities, passives, or health bonus");
			}
			if (!d.actives().isEmpty()) {
				throw new IllegalArgumentException("Locked class " + d.id() + " must carry no actives");
			}
		} else if (d.activeAbilities().isEmpty()) {
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
