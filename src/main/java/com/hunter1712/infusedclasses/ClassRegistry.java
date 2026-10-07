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
 * All classes. Adding a class = one definition here. Berserker carries Bloodlust plus
 * Second Wind, one conditional passive (Bulwark) and the Fury HP-scaled pair.
 */
public final class ClassRegistry {
	public static final Identifier BERSERKER = Identifier.fromNamespaceAndPath("infusedclasses", "berserker");
	public static final Identifier BLOODLUST_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "bloodlust");
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
				BERSERKER,
				"Berserker",
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
								"Fury Damage",
								ATTACK_DAMAGE,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "fury_damage"),
								0.3,
								"Fury"),
						new HpScaledPassive(
								"Fury Speed",
								ATTACK_SPEED,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "fury_speed"),
								0.05,
								"Fury")),
				0.0,
				List.of(BLOODLUST_ABILITY, SECOND_WIND_ABILITY),
				0xFFE74C3C,
				"Frontline bruiser",
				List.of(
						new ClassDefinition.ActiveAbility(
								BloodlustAbility.NAME,
								"Opens a " + (BloodlustAbility.WINDOW_TICKS / 20) + "s lifesteal window healing "
										+ "bucket x " + (int) (BloodlustAbility.LIFESTEAL_PER_BUCKET * 100)
										+ "% of melee damage per hit (max "
										+ ConditionalPassive.amount(BloodlustAbility.MAX_PER_HIT_HP)
										+ " HP), fires under "
										+ (20 * (HpScaledPassive.STEPS - BloodlustAbility.GATE_BUCKET)
												/ HpScaledPassive.STEPS / 2)
										+ " hearts and grants "
										+ "bucket Bulwark (R, "
										+ (Cooldowns.BLOODLUST_MS / 1000) + "s). Roots you for "
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
