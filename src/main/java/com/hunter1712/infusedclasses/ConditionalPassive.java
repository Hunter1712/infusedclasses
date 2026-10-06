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
	/** The only trigger the server entry feeds. One constant, so a rename cannot go silently inert. */
	public static final String ON_HIT_TAKEN = "on_hit_taken";
	/** Full picker copy. Generated from the numbers so it cannot drift from behaviour. */
	public String fullDesc() {
		return earnClause() + " (max " + maxStacks + "). Each stack adds +" + amount(perStack)
				+ " " + word() + ". After " + graceSeconds + "s " + idleClause() + ", lose all stacks.";
	}

	/**
	 * What earns a stack. The definition carries a trigger id, not prose for it, so the copy only
	 * describes an earning event it actually knows: the one trigger the mod emits is described,
	 * and any other trigger is described as the bare trigger rather than guessed at.
	 */
	private String earnClause() {
		return isHitTrigger() ? "Taking a hit grants 1 stack" : "Each trigger grants 1 stack";
	}

	/** The idle half of the same wording: what has to stop happening for the stacks to decay. */
	private String idleClause() {
		return isHitTrigger() ? "without a hit" : "without a trigger";
	}

	private boolean isHitTrigger() {
		return ON_HIT_TAKEN.equals(trigger);
	}

	/** Attribute name as the copy spells it: "minecraft:armor_toughness" reads as "armor toughness". */
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