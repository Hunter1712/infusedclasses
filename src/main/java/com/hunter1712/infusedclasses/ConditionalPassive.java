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
 * @param trigger the event that earns a stack, its own value plus its own copy
 * @param attribute which attribute the stacks add to, as a registry key rather than a live holder
 * @param modifierId identifier the passive's modifier is written under
 * @param perStack what one stack contributes to that attribute
 * @param graceSeconds seconds after the last trigger before all stacks drop at once
 */
public record ConditionalPassive(
		String name,
		Trigger trigger,
		int maxStacks,
		ResourceKey<Attribute> attribute,
		Identifier modifierId,
		double perStack,
		int graceSeconds
) {
	/** Short picker copy. Generated from the numbers so it cannot drift from behaviour. */
	public String fullDesc() {
		return trigger.earn() + ": +" + amount(perStack)
				+ " " + word() + " (max " + maxStacks + "). "
				+ graceSeconds + "s " + trigger.idle() + ": lose all.";
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