package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;

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
	public record Modifier(ResourceKey<Attribute> attribute, Identifier modifierId, double amount) {
	}

	/**
	 * What a tick should do about one HP-scaled passive: write the modifier its bucket
	 * calls for, take off what a dead bucket earned, or nothing.
	 */
	public sealed interface Plan {
		record Write(Modifier wanted) implements Plan {
		}

		record Remove(ResourceKey<Attribute> attribute, Identifier modifierId) implements Plan {
		}

		record Skip() implements Plan {
		}
	}

	/**
	 * The modifier the passive's current bucket calls for, or null when it should carry none.
	 * Bucket 0 grants nothing: an absent modifier, never a zero-valued one. Above-cap
	 * buckets grant the cap: the bucket math already clamps, this holds the promise anyway.
	 * Otherwise the floor plus the steps: a passive can punish the near-dead too.
	 */
	public static Modifier forBucket(HpScaledPassive passive, int bucket) {
		if (passive == null || bucket <= 0) {
			return null;
		}
		int capped = Math.min(bucket, HpScaledPassive.STEPS);
		return new Modifier(passive.attribute(), passive.modifierId(),
				passive.baseBonus() + capped * passive.perStep());
	}

	/**
	 * The whole answer for one HP-scaled passive this tick: write the bucket's modifier,
	 * take off what a dead bucket earned, or nothing. The bucket is recomputed from live
	 * HP every tick and the writer's early-out covers no-op ticks, so unlike the tiered
	 * bar there is no moved-set.
	 */
	public static Plan planForBucket(HpScaledPassive hp, int bucket) {
		Modifier wanted = forBucket(hp, bucket);
		if (wanted != null) {
			return new Plan.Write(wanted);
		}
		if (hp != null) {
			return new Plan.Remove(hp.attribute(), hp.modifierId());
		}
		return new Plan.Skip();
	}
}
