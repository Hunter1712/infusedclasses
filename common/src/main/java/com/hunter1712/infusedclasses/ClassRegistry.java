package com.hunter1712.infusedclasses;


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
	public static final String JUGGERNAUT = ModIds.MOD_ID + ":juggernaut";
	public static final String OFFLOAD_ABILITY = ModIds.MOD_ID + ":offload";
	public static final String ONSLAUGHT_ABILITY =
			ModIds.MOD_ID + ":onslaught";
	public static final String LEGIONARY = ModIds.MOD_ID + ":legionary";
	public static final String ROTATE_ABILITY = ModIds.MOD_ID + ":rotate";
	public static final String AEQUILIBRIUM_ABILITY =
			ModIds.MOD_ID + ":aequilibrium";
	public static final String GAMBLER = ModIds.MOD_ID + ":gambler";
	public static final String ROULETTE_ABILITY = ModIds.MOD_ID + ":roulette";
	public static final String ALL_IN_ABILITY = ModIds.MOD_ID + ":all_in";
	/** ponytail: attribute ids rather than vanilla holders, so the passive stays bootstrap-free. */
	public static final AttributeRef ARMOR = new AttributeRef("minecraft:armor");
	public static final AttributeRef ATTACK_DAMAGE = new AttributeRef("minecraft:attack_damage");
	public static final AttributeRef ATTACK_SPEED = new AttributeRef("minecraft:attack_speed");
	public static final AttributeRef MOVEMENT_SPEED = new AttributeRef("minecraft:movement_speed");
	public static final AttributeRef ARMOR_TOUGHNESS = new AttributeRef("minecraft:armor_toughness");
	public static final AttributeRef KNOCKBACK_RESISTANCE = new AttributeRef("minecraft:knockback_resistance");

	private static final Map<String, ClassDefinition> BY_ID = new LinkedHashMap<>();

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

	public static ClassDefinition byId(String id) {
		if (id == null) {
			return null;
		}
		return BY_ID.get(id);
	}
}
