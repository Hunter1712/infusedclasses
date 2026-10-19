package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The Juggernaut's one mechanic, as a decision: a 60-stack Momentum bar fed by hits
 * dealt and taken and worth a three-checkpoint weight the world can see. Pure: the
 * bar's numbers, its rows and the modifiers each row writes live here, so plain JUnit
 * asserts the mapping and the server entry only has to apply it. The row words and the
 * picker copy ride the {@link ConditionalPassive} row itself; this module owns the ids
 * and the attributes those rows land on, and the fixed amount each event pays.
 *
 * <p>The bar is read, never remembered: a spend or a bleed changes the count and the
 * next tick reconciles to the row the bar names then. Above the last checkpoint the bar
 * stays Rooted, with the headroom up to {@link #MAX_STACKS} as spend reserve.
 */
public final class Momentum {
	public static final String NAME = "Momentum";
	/** The bar's ceiling: the top checkpoint is 45, so 60 leaves a 15-stack reserve above Rooted. */
	public static final int MAX_STACKS = 60;
	/** Quiet seconds after a feed before the bar bleeds, then one stack per second. */
	public static final int GRACE_SECONDS = 5;
	/** Stacks one landed hit pays, dealt and taken alike. */
	public static final int PER_HIT_DEALT = 1;
	public static final int PER_HIT_TAKEN = 1;

	/** The three checkpoints: armor up, weight up, a heavier stance against knockback each row. */
	public static final List<ConditionalPassive.Tier> TIERS = List.of(
			new ConditionalPassive.Tier(15, "Steady", 1.0, -0.005, 0.25),
			new ConditionalPassive.Tier(30, "Braced", 3.0, -0.01, 0.5),
			new ConditionalPassive.Tier(45, "Rooted", 5.0, -0.015, 1.0));

	/** Armor modifier id, resolved by the applier. */
	public static final Identifier ARMOR_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "momentum_armor");
	/** Weight modifier id: the move-speed cost of the current row. */
	public static final Identifier WEIGHT_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "momentum_weight");
	/** Knockback-resistance modifier id: on every row, per the heavy-wall identity. */
	public static final Identifier KNOCKBACK_ID =
			Identifier.fromNamespaceAndPath(InfusedClasses.MOD_ID, "momentum_knockback");

	/** The one passive: fed by hits dealt and taken, tiered 15/30/45, quiet 5s then -1/s. Its rows are its own. */
	public static final ConditionalPassive PASSIVE = new ConditionalPassive(
			NAME,
			List.of(
					new ConditionalPassive.Earn(Trigger.ON_HIT_DEALT, PER_HIT_DEALT),
					new ConditionalPassive.Earn(Trigger.ON_HIT_TAKEN, PER_HIT_TAKEN)),
			MAX_STACKS,
			TIERS,
			GRACE_SECONDS,
			Momentum::forStacks);

	private Momentum() {
	}

	/**
	 * The modifiers the current bar calls for: the reached row's armor, weight and
	 * knockback entries, empty below the first checkpoint. One list for the whole row,
	 * so the applier's write and remove sides read the same answer.
	 */
	public static List<PassiveEffect.Modifier> forStacks(int stacks) {
		ConditionalPassive.Tier row = PASSIVE.tierFor(stacks);
		if (row == null) {
			return List.of();
		}
		return List.of(
				new PassiveEffect.Modifier(ClassRegistry.ARMOR, ARMOR_ID, row.armor()),
				new PassiveEffect.Modifier(ClassRegistry.MOVEMENT_SPEED, WEIGHT_ID, row.weight()),
				new PassiveEffect.Modifier(ClassRegistry.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, row.knockback()));
	}

	/**
	 * Every id the bar can ever write, one entry per id: the sweep set a tick removes
	 * what the current row does not name, and the set a class switch takes off. The
	 * amounts are the top row's; only the ids are this list's point.
	 */
	public static List<PassiveEffect.Modifier> allModifiers() {
		return forStacks(TIERS.get(TIERS.size() - 1).threshold());
	}
}
