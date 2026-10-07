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
 */
public final class ClassRegistry {
	public static final Identifier JUGGERNAUT = Identifier.fromNamespaceAndPath("infusedclasses", "juggernaut");
	public static final Identifier RUSH_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "rush");
	public static final Identifier REND_ABILITY = Identifier.fromNamespaceAndPath("infusedclasses", "rend");
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
						4)),
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
										+ (RushAbility.SPEED_TICKS / 20) + "s; your next melee hit grants +"
										+ RushAbility.BONUS_STACKS + " Momentum (R, "
										+ (Cooldowns.RUSH_MS / 1000) + "s). Whoosh!"),
						new ClassDefinition.ActiveAbility(
								RendAbility.NAME,
								"Empowers your next melee hit within "
										+ (RendAbility.WINDOW_TICKS / 20) + "s for "
										+ ConditionalPassive.amount(RendAbility.BASE_HP)
										+ " HP + " + ConditionalPassive.amount(RendAbility.HP_PER_STACK)
										+ " HP per Momentum stack, consumes stacks (G, "
										+ (Cooldowns.REND_MS / 1000) + "s). Smash!"))));

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
