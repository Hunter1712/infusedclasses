package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Reactive conditional passive: event-triggered stacks, a cap, decay after a grace period, and
 * the attribute those stacks are worth. Plain data, no game bootstrap needed, so tests assert on
 * it directly.
 *
 * @param attribute which attribute the stacks add to, as a registry key rather than a live holder
 * @param modifierId identifier the passive's modifier is written under
 * @param perStack what one stack contributes to that attribute
 * @param graceSeconds seconds after the last trigger before stacks start decaying
 * @param decayPerSecond stacks lost per second once the grace period is up
 */
public record ConditionalPassive(
		String name,
		String trigger,
		int maxStacks,
		ResourceKey<Attribute> attribute,
		Identifier modifierId,
		double perStack,
		int graceSeconds,
		int decayPerSecond
) {
	/** One-line card summary. Generated from the numbers so it cannot drift from behaviour. */
	public String shortDesc() {
		return "+" + amount(perStack) + " " + capitalized(word()) + " per stack, up to " + maxStacks;
	}

	/** Full picker copy. Generated from the numbers so it cannot drift from behaviour. */
	public String fullDesc() {
		return "Taking a hit grants 1 stack (max " + maxStacks + "). Each stack adds +" + amount(perStack)
				+ " " + word() + ". After " + graceSeconds + "s without a hit, lose " + decayPerSecond
				+ " stack per second.";
	}

	/** Attribute name as the copy spells it: "minecraft:armor_toughness" reads as "armor toughness". */
	private String word() {
		return attribute.identifier().getPath().replace('_', ' ');
	}

	private static String capitalized(String word) {
		return Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}

	/** Whole numbers read as "1", not "1.0", so the copy matches the game. */
	private static String amount(double value) {
		return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
	}
}