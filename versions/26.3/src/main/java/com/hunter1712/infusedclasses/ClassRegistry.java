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
 * All classes. Adding a class = one definition here. Juggernaut carries Burden (the
 * always-on heavy chassis) plus one Momentum bar, and the Offload/Onslaught pair that
 * spends it. Legionary carries Rotate plus the Impetus/Testudo flip buffs and the
 * Aequilibrium oath ultimate; its stances are transient auras, not passives. Gambler
 * carries the free Roulette wheel and All In, with Crooked Dice and Streak generated
 * from its crit rule and rendering as its passives.
 */
public final class ClassRegistry {
	public static final Identifier JUGGERNAUT = Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "juggernaut");
	public static final Identifier OFFLOAD_ABILITY = Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "offload");
	public static final Identifier ONSLAUGHT_ABILITY =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "onslaught");
	public static final Identifier LEGIONARY = Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "legionary");
	public static final Identifier ROTATE_ABILITY = Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "rotate");
	public static final Identifier AEQUILIBRIUM_ABILITY =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "aequilibrium");
	public static final Identifier GAMBLER = Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "gambler");
	public static final Identifier ROULETTE_ABILITY = Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "roulette");
	public static final Identifier ALL_IN_ABILITY = Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "all_in");
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
	public static final ResourceKey<Attribute> KNOCKBACK_RESISTANCE =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("knockback_resistance"));

	private static final Map<Identifier, ClassDefinition> BY_ID = new LinkedHashMap<>();

	static {
		register(new ClassDefinition(
				JUGGERNAUT,
				"Juggernaut",
				List.of(Momentum.PASSIVE),
				List.of(),
				List.of(Burden.chassis()),
				0xFFE74C3C,
				"Frontline bruiser",
				List.of(
						new ClassDefinition.ActiveAbility(
								OFFLOAD_ABILITY, OffloadAbility.NAME, OffloadAbility.desc()),
						new ClassDefinition.ActiveAbility(
								ONSLAUGHT_ABILITY, OnslaughtAbility.NAME, OnslaughtAbility.desc())),
				List.of(),
				CritRule.universal()));

		register(new ClassDefinition(
				LEGIONARY,
				"Legionary",
				List.of(),
				List.of(),
				0xFFB87333,
				"Sword and shield",
				List.of(
						new ClassDefinition.ActiveAbility(ROTATE_ABILITY, RotateAbility.NAME, RotateAbility.desc()),
						new ClassDefinition.ActiveAbility(
								AEQUILIBRIUM_ABILITY, AequilibriumAbility.NAME, AequilibriumAbility.desc())),
				List.of(
						new ClassDefinition.FlipBuff(RotateAbility.IMPETUS_NAME, RotateAbility.impetusDesc()),
						new ClassDefinition.FlipBuff(RotateAbility.TESTUDO_NAME, RotateAbility.testudoDesc())),
				CritRule.universal()));

		register(new ClassDefinition(
				GAMBLER,
				"Gambler",
				List.of(),
				List.of(),
				0xFFD4AF37,
				"Double or nothing",
				List.of(
						new ClassDefinition.ActiveAbility(
								ROULETTE_ABILITY, RouletteAbility.NAME, RouletteAbility.desc()),
						new ClassDefinition.ActiveAbility(ALL_IN_ABILITY, AllInAbility.NAME, AllInAbility.desc())),
				List.of(),
				CritRule.crookedDice()));
	}

	private ClassRegistry() {
	}

	private static void register(ClassDefinition d) {
		if (d.actives().isEmpty()) {
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
