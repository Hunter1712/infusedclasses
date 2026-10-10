package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * HP-scaled passive: current HP maps to stepped buckets, each bucket a fixed bonus.
 * Plain data like {@link ConditionalPassive}, but with no trigger, stacks, or grace:
 * the bucket is recomputed from live HP every tick, so damage shrinks it on its own.
 * The picker copy is generated from these numbers, so it cannot drift from behaviour.
 *
 * @param attribute which attribute the bucket adds to, as a registry key rather than a live holder
 * @param modifierId identifier the passive's modifier is written under
 * @param perStep what one step (10% current HP) contributes to that attribute
 * @param group the family this reads as on the card and tooltip: passives sharing
 * a group render as one line with their picker copy joined, so a pair never reads as two passives
 * @param baseBonus the floor at 0 HP: full HP pays this plus every step, so a passive
 * can punish the near-dead as well as reward the topped-off
 */
public record HpScaledPassive(
		String name,
		ResourceKey<Attribute> attribute,
		Identifier modifierId,
		double perStep,
		String group,
		double baseBonus
) {
	/** Current-HP steps: 10% each, 10 total, zero bonus at 0 HP, max at full HP. */
	public static final int STEPS = 10;

	/**
	 * Bucket 0-10 for current/max HP: floor(current * 10 / max), clamped. A degenerate max
	 * reads as zero HP. The epsilon keeps exact boundaries on their step (90% HP is
	 * step 9): float division lands a hair under whole steps.
	 */
	public static int bucketForCurrent(double currentHp, double maxHp) {
		if (maxHp <= 0 || currentHp <= 0) {
			return 0;
		}
		if (currentHp >= maxHp) {
			return STEPS;
		}
		return Math.max(0, Math.min(STEPS, (int) Math.floor(currentHp / maxHp * STEPS + 1e-9)));
	}

	/** Bonus at full HP: the floor plus every step. */
	public double maxBonus() {
		return baseBonus + STEPS * perStep;
	}

	/** Short picker copy, generated from the numbers so it cannot drift from behaviour. */
	public String fullDesc() {
		return "HP-scaled: " + ConditionalPassive.amount(baseBonus)
				+ " at 0 HP to +" + ConditionalPassive.amount(maxBonus()) + " full HP.";
	}
}
