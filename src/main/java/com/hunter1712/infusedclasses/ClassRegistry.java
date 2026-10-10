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
 * oath ultimate; its stances are transient auras, not passives.
 */
public final class ClassRegistry {
	public static final Identifier JUGGERNAUT = Identifier.fromNamespaceAndPath("infusedclasses", "juggernaut");
	public static final Identifier RUSH_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "rush");
	public static final Identifier REND_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "rend");
	public static final Identifier LEGIONARY = Identifier.fromNamespaceAndPath("infusedclasses", "legionary");
	public static final Identifier ROTATE_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "rotate");
	public static final Identifier IMPETUS_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "impetus");
	public static final Identifier TESTUDO_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "testudo");
	public static final Identifier AEQUILIBRIUM_ABILITY =
			Identifier.fromNamespaceAndPath("infusedclasses", "aequilibrium");
	/** ponytail: attribute ids rather than vanilla holders, so the passive stays bootstrap-free. */
	public static final ResourceKey<Attribute> ARMOR =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("armor"));
	public static final ResourceKey<Attribute> ATTACK_DAMAGE =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("attack_damage"));
	public static final ResourceKey<Attribute> ATTACK_SPEED =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("attack_speed"));
	public static final ResourceKey<Attribute> MAX_HEALTH =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("max_health"));
	public static final ResourceKey<Attribute> MOVEMENT_SPEED =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("movement_speed"));
	public static final ResourceKey<Attribute> ARMOR_TOUGHNESS =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("armor_toughness"));
	public static final ResourceKey<Attribute> KNOCKBACK_RESISTANCE =
			ResourceKey.create(Registries.ATTRIBUTE, Identifier.withDefaultNamespace("knockback_resistance"));
	public static final Identifier BONUS_HEALTH_ID =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "bonus_health");

	private static final Map<Identifier, ClassDefinition> BY_ID = new LinkedHashMap<>();

	static {
		register(new ClassDefinition(
				JUGGERNAUT,
				"Juggernaut",
				false,
				List.of(new ConditionalPassive(
						"Momentum",
						ConditionalPassive.ON_HIT_DEALT,
						10,
						ARMOR,
						Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "momentum_armor"),
						1.0,
						5)),
				List.of(
						new HpScaledPassive(
								"Vigor Damage",
								ATTACK_DAMAGE,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "vigor_damage"),
								0.3,
								"Vigor"),
						new HpScaledPassive(
								"Vigor Speed",
								ATTACK_SPEED,
								Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "vigor_speed"),
								0.05,
								"Vigor")),
				0.0,
				List.of(RUSH_ABILITY, REND_ABILITY),
				0xFFE74C3C,
				"Frontline bruiser",
				List.of(
						new ClassDefinition.ActiveAbility(
								RushAbility.NAME,
								"Cleanses harmful effects and grants Speed I for "
										+ (RushAbility.SPEED_TICKS / 20) + "s; your next full-swing melee hit grants +"
										+ RushAbility.BONUS_STACKS + " Momentum (R, "
										+ (Cooldowns.RUSH_MS / 1000) + "s). " + RushAbility.CAST_WORD),
						new ClassDefinition.ActiveAbility(
								RendAbility.NAME,
								"Empowers your next melee hit within "
										+ (RendAbility.WINDOW_TICKS / 20) + "s for "
										+ ConditionalPassive.amount(RendAbility.BASE_HP)
										+ " HP + " + ConditionalPassive.amount(RendAbility.HP_PER_STACK)
										+ " HP per Momentum stack, consumes stacks (G, "
										+ (Cooldowns.REND_MS / 1000) + "s). " + RendAbility.CAST_WORD))));

		register(new ClassDefinition(
				LEGIONARY,
				"Legionary",
				false,
				List.of(),
				List.of(),
				0.0,
				List.of(ROTATE_ABILITY, IMPETUS_ABILITY, TESTUDO_ABILITY, AEQUILIBRIUM_ABILITY),
				0xFFB87333,
				"Sword and shield",
				List.of(
						new ClassDefinition.ActiveAbility(
								RotateAbility.NAME,
								"Toggle stance: Gladius (+" + ConditionalPassive.amount(RotateAbility.GLADIUS_DAMAGE)
										+ " damage, +" + ConditionalPassive.amount(RotateAbility.GLADIUS_SPEED)
										+ " speed, take x" + RotateAbility.GLADIUS_TAKEN_MULT
										+ ") or Scutum (+" + ConditionalPassive.amount(RotateAbility.SCUTUM_ARMOR)
										+ " armor, +" + ConditionalPassive.amount(RotateAbility.SCUTUM_TOUGHNESS)
										+ " toughness, slower) (R, "
										+ (Cooldowns.ROTATE_MS / 1000) + "s). Announces Gladius!/Scutum!."
										+ " Flipping to Gladius: next hit deals x" + ImpetusAbility.SWITCH_DEAL_MULT
										+ " (" + (ImpetusAbility.SWITCH_DEAL_MS / 1000) + "s)."
										+ " Flipping to Scutum: take x" + TestudoAbility.SWITCH_TAKE_MULT
										+ " (" + ConditionalPassive.amount(TestudoAbility.SWITCH_TAKE_MS / 1000.0)
										+ "s)."),
						new ClassDefinition.ActiveAbility(
								ImpetusAbility.NAME,
								ImpetusAbility.desc()),
						new ClassDefinition.ActiveAbility(
								TestudoAbility.NAME,
								TestudoAbility.desc()),
						new ClassDefinition.ActiveAbility(
								AequilibriumAbility.NAME,
								AequilibriumAbility.desc()))));
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
