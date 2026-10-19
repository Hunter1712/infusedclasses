package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Burden, the Juggernaut's chassis: always on while classed, never earned. Heavier hits,
 * slower swings — the class reads slow and heavy first, and Momentum is what moves under
 * that weight, never a speed it forgot it gave up. Pure data; the applier writes it every
 * tick while classed and the class-switch cleanup takes it off, the same way as any row.
 */
public final class Burden {
	public static final String NAME = "Burden";
	/** What the chassis adds to every hit. */
	public static final double DAMAGE_BONUS = 1.0;
	/** What the chassis takes off every swing: half an attack-speed point, always slow. */
	public static final double SWING_PENALTY = -0.5;
	/** Damage modifier id, resolved by the applier. */
	public static final Identifier DAMAGE_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "burden_damage");
	/** Swing modifier id, resolved by the applier. */
	public static final Identifier SWING_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "burden_swing");

	private Burden() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "+" + ConditionalPassive.amount(DAMAGE_BONUS) + " damage, "
				+ ConditionalPassive.amount(SWING_PENALTY) + " attack speed. Weight is the point.";
	}

	/** The chassis as the definition's always-on row: the name, the copy and the pair it writes. */
	public static ClassDefinition.AlwaysOn chassis() {
		return new ClassDefinition.AlwaysOn(NAME, desc(), List.of(
				new PassiveEffect.Modifier(ClassRegistry.ATTACK_DAMAGE, DAMAGE_ID, DAMAGE_BONUS),
				new PassiveEffect.Modifier(ClassRegistry.ATTACK_SPEED, SWING_ID, SWING_PENALTY)));
	}
}
