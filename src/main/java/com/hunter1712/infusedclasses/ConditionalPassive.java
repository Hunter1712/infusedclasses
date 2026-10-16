package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Reactive conditional passive: event-triggered stacks, a cap, a grace period, and
 * the attribute those stacks are worth. Plain data, no game bootstrap needed, so tests assert on
 * it directly. The picker copy is generated from these numbers, so it can say what the passive
 * does but never an earning event its trigger does not carry.
 *
 * @param attribute which attribute the stacks add to, as a registry key rather than a live holder
 * @param modifierId identifier the passive's modifier is written under
 * @param perStack what one stack contributes to that attribute
 * @param graceSeconds seconds after the last trigger before all stacks drop at once
 */
public record ConditionalPassive(
		String name,
		String trigger,
		int maxStacks,
		ResourceKey<Attribute> attribute,
		Identifier modifierId,
		double perStack,
		int graceSeconds
) {
	/** A hit-taken trigger: any passive on this trigger earns a stack per hit taken. One constant, so a rename cannot go silently inert. */
	public static final String ON_HIT_TAKEN = "on_hit_taken";
	/** The trigger the Juggernaut feeds by landing a hit. Same guarantee as the hit-taken one. */
	public static final String ON_HIT_DEALT = "on_hit_dealt";
	/** Short picker copy. Generated from the numbers so it cannot drift from behaviour. */
	public String fullDesc() {
		TriggerPhrasing said = phrasing();
		return said.earn() + ": +" + amount(perStack)
				+ " " + word() + " (max " + maxStacks + "). "
				+ graceSeconds + "s " + said.idle() + ": lose all.";
	}

	/**
	 * What a trigger earns and what idleness decays, in one row per trigger: the definition
	 * carries a trigger id, not prose for it, so the copy only describes an earning event it
	 * actually knows. A third trigger adds one row, never two chains to keep in sync.
	 */
	private record TriggerPhrasing(String earn, String idle) {
	}

	private TriggerPhrasing phrasing() {
		return switch (trigger) {
			case ON_HIT_TAKEN -> new TriggerPhrasing("Take a hit", "no hit");
			case ON_HIT_DEALT -> new TriggerPhrasing("Land a hit", "no hit");
			default -> new TriggerPhrasing("Trigger", "no trigger");
		};
	}

	private String word() {
		return wordFor(attribute);
	}

	/** Attribute name as the copy spells it: "minecraft:armor_toughness" reads as "armor toughness". */
	static String wordFor(ResourceKey<Attribute> attribute) {
		return attribute.identifier().getPath().replace('_', ' ');
	}

	/** Whole numbers read as "1", not "1.0", so the copy matches the game. */
	static String amount(double value) {
		return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
	}
}