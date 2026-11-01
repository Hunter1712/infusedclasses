package com.hunter1712.infusedclasses;


/**
 * Attribute-modifier math, as a decision: the shared {@link Modifier} value every kit
 * writes under, and the HP-scaled bucket mapping. Pure: no player, level or attribute
 * instance is involved, so plain JUnit asserts the mapping and the server entry only has
 * to apply the answer. Conditional passives map through their own module
 * ({@link Momentum#forStacks}), read as the same Modifier shape.
 */
public final class PassiveEffect {
	private PassiveEffect() {
	}

	/** What should be on the player: which attribute, which modifier id, at what value. */
	public record Modifier(AttributeRef attribute, String modifierId, double amount) {
	}

}
