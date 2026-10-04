package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Stack-to-attribute mapping, as a decision. Given a passive and its current stack count it
 * answers which attribute should carry a modifier, under which id, at what value. Pure: no
 * player, level or attribute instance is involved, so plain JUnit asserts the mapping and the
 * server entry only has to apply the answer.
 */
public final class PassiveEffect {
	private PassiveEffect() {
	}

	/** What should be on the player: which attribute, which modifier id, at what value. */
	public record Modifier(ResourceKey<Attribute> attribute, Identifier modifierId, double amount) {
	}

	/**
	 * The modifier the passive's current stacks call for, or null when it should carry none.
	 * Zero or fewer stacks grant nothing. The cap is the stack state's job, not this one's.
	 */
	public static Modifier forStacks(ConditionalPassive passive, int stacks) {
		if (passive == null || stacks <= 0) {
			return null;
		}
		return new Modifier(passive.attribute(), passive.modifierId(), stacks * passive.perStack());
	}
}