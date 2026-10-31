package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Burden, the Juggernaut's chassis: always on while classed, never earned. Heavier hits,
 * slower swings — the class reads slow and heavy first, and Momentum never gives that
 * speed back. The heavier hit is a deal mult the melee plan reads; the swing penalty is
 * the one attribute row. Pure data; the applier writes it every tick while classed and the
 * class-switch cleanup takes it off, the same way as any row.
 */
public final class Burden {
	public static final String NAME = "Burden";
	/** What the chassis multiplies every landed hit by: a heavier blow. */
	public static final double DEAL_MULT = 1.15;
	/** What the chassis takes off every swing: half an attack-speed point, always slow. */
	public static final double SWING_PENALTY = -0.4;
	/** Swing modifier id, resolved by the applier. */
	public static final Identifier SWING_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "burden_swing");

	private Burden() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "x" + ConditionalPassive.amount(DEAL_MULT) + " damage, "
				+ ConditionalPassive.amount(SWING_PENALTY) + " attack speed.";
	}

	/** The chassis as the definition's always-on row: the name, the copy, the swing it writes and the mult it deals. */
	public static ClassDefinition.AlwaysOn chassis() {
		return new ClassDefinition.AlwaysOn(NAME, desc(), List.of(
				new PassiveEffect.Modifier(ClassRegistry.ATTACK_SPEED, SWING_ID, SWING_PENALTY)),
				DEAL_MULT);
	}
}
