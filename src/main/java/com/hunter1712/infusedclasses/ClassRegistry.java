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
 * All classes. Adding a class = one definition here. Juggernaut carries Rush plus
 * Rend, one conditional passive (Momentum) and the Vigor HP-scaled pair.
 * Legionary carries Rotate plus the Impetus/Testudo flip buffs and the Aequilibrium
 * oath ultimate; its stances are transient auras, not passives. Gambler carries the
 * free Roulette wheel and All In, with Crooked Dice and Streak generated from its
 * crit rule and rendering as its passives.
 */
public final class ClassRegistry {
	public static final Identifier JUGGERNAUT = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "juggernaut");
	public static final Identifier RUSH_ABILITY = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "rush");
	public static final Identifier REND_ABILITY = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "rend");
	public static final Identifier LEGIONARY = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "legionary");
	public static final Identifier ROTATE_ABILITY = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "rotate");
	public static final Identifier AEQUILIBRIUM_ABILITY =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "aequilibrium");
	public static final Identifier GAMBLER = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "gambler");
	public static final Identifier ROULETTE_ABILITY = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "roulette");
	public static final Identifier ALL_IN_ABILITY = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "all_in");
	/** ponytail: attribute ids rather than vanilla holders, so the passive stays bootstrap-free. */
	public static final ResourceKey<Attribute> ARMOR =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("armor"));
	public static final ResourceKey<Attribute> ATTACK_DAMAGE =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("attack_damage"));
	public static final ResourceKey<Attribute> ATTACK_SPEED =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("attack_speed"));
	public static final ResourceKey<Attribute> MOVEMENT_SPEED =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("movement_speed"));
	public static final ResourceKey<Attribute> ARMOR_TOUGHNESS =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("armor_toughness"));

	private static final Map<Identifier, ClassDefinition> BY_ID = new LinkedHashMap<>();

	static {
		register(new ClassDefinition(
				JUGGERNAUT,
				"Juggernaut",
				List.of(new ConditionalPassive(
						"Momentum",
						ConditionalPassive.ON_HIT_DEALT,
						20,
						ARMOR,
						Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "momentum_armor"),
						0.4,
						5)),
				List.of(
						new HpScaledPassive(
								ATTACK_DAMAGE,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "vigor_damage"),
								0.3,
								"Vigor",
								-1.0),
						new HpScaledPassive(
								ATTACK_SPEED,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "vigor_speed"),
								0.075,
								"Vigor",
								-0.25)),
				List.of(RUSH_ABILITY, REND_ABILITY),
				0xFFE74C3C,
				"Frontline bruiser",
				List.of(
						new ClassDefinition.ActiveAbility(
								RushAbility.NAME,
								"Cleanses harmful effects and quickens stride for "
										+ (RushAbility.WINDOW_TICKS / 20) + "s. Next full-swing hit: +"
										+ RushAbility.BONUS_STACKS + " Momentum (R, "
										+ (Cooldowns.RUSH_MS / 1000) + "s)."),
						new ClassDefinition.ActiveAbility(
								RendAbility.NAME,
								RendAbility.desc())),
				List.of(),
				CritRule.universal()));

		register(new ClassDefinition(
				LEGIONARY,
				"Legionary",
				List.of(),
				List.of(),
				List.of(ROTATE_ABILITY, AEQUILIBRIUM_ABILITY),
				0xFFB87333,
				"Sword and shield",
				List.of(
						new ClassDefinition.ActiveAbility(
								RotateAbility.NAME,
								"Gladius (+" + ConditionalPassive.amount(RotateAbility.GLADIUS_DAMAGE)
										+ " damage, +" + ConditionalPassive.amount(RotateAbility.GLADIUS_SPEED)
										+ " speed, take x" + RotateAbility.GLADIUS_TAKEN_MULT
										+ ") or Scutum (+" + ConditionalPassive.amount(RotateAbility.SCUTUM_ARMOR)
										+ " armor, +" + ConditionalPassive.amount(RotateAbility.SCUTUM_TOUGHNESS)
										+ " toughness, slower) (R, "
										+ (Cooldowns.ROTATE_MS / 1000) + "s)."),
						new ClassDefinition.ActiveAbility(
								AequilibriumAbility.NAME,
								AequilibriumAbility.desc())),
				List.of(
						new ClassDefinition.ActiveAbility(
								RotateAbility.IMPETUS_NAME,
								RotateAbility.impetusDesc()),
						new ClassDefinition.ActiveAbility(
								RotateAbility.TESTUDO_NAME,
								RotateAbility.testudoDesc())),
				CritRule.universal()));

		register(new ClassDefinition(
				GAMBLER,
				"Gambler",
				List.of(),
				List.of(),
				List.of(ROULETTE_ABILITY, ALL_IN_ABILITY),
				0xFFD4AF37,
				"Double or nothing",
				List.of(
						new ClassDefinition.ActiveAbility(
								RouletteAbility.NAME,
								RouletteAbility.desc()),
						new ClassDefinition.ActiveAbility(
								AllInAbility.NAME,
								AllInAbility.desc())),
				List.of(),
				CritRule.crookedDice()));
	}

	private ClassRegistry() {
	}

	private static void register(ClassDefinition d) {
		if (d.activeAbilities().isEmpty()) {
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
