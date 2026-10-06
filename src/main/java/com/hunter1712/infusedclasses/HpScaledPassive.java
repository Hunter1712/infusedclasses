package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * HP-scaled passive: missing HP maps to stepped buckets, each bucket a fixed bonus.
 * Plain data like {@link ConditionalPassive}, but with no trigger, stacks, or grace:
 * the bucket is recomputed from live HP every tick, so heals shrink it on their own.
 * The picker copy is generated from these numbers, so it cannot drift from behaviour.
 *
 * @param attribute which attribute the bucket adds to, as a registry key rather than a live holder
 * @param modifierId identifier the passive's modifier is written under
 * @param perStep what one step (10% missing HP) contributes to that attribute
 * @param group the family this reads as on the card and tooltip: passives sharing
 * a group render as one line with their picker copy joined, so a pair never reads as two passives
 */
public record HpScaledPassive(
		String name,
		ResourceKey<Attribute> attribute,
		Identifier modifierId,
		double perStep,
		String group
) {
	/** Missing-HP steps: 10% each, 10 total, zero bonus at full HP, max at 0 HP. */
	public static final int STEPS = 10;

	/**
	 * Bucket 0-10 for current/max HP: floor(missing * 10), clamped. A degenerate max
	 * reads as full HP. The epsilon keeps exact boundaries on their step (90% HP is
	 * step 1): float division lands a hair under whole steps.
	 */
	public static int bucket(double currentHp, double maxHp) {
		if (maxHp <= 0 || currentHp >= maxHp) {
			return 0;
		}
		if (currentHp <= 0) {
			return STEPS;
		}
		double missing = 1.0 - currentHp / maxHp;
		return Math.max(0, Math.min(STEPS, (int) Math.floor(missing * STEPS + 1e-9)));
	}

	/** Full picker copy, generated from the numbers so it cannot drift from behaviour. */
	public String fullDesc() {
		return "Each 10% missing HP grants +" + ConditionalPassive.amount(perStep)
				+ " " + ConditionalPassive.wordFor(attribute)
				+ " (max +" + ConditionalPassive.amount(perStep * STEPS) + " at 0 HP).";
	}
}
