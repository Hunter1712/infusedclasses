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
	 * What a tick should do about one passive: write the modifier its stacks call for, take off
	 * what dead stacks earned, or nothing. The stacks-moved flag is the tick's own decay answer:
	 * a tick that moved no stacks cannot have changed what should be on the player.
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
	 */
	public static Modifier forBucket(HpScaledPassive passive, int bucket) {
		if (passive == null || bucket <= 0) {
			return null;
		}
		int capped = Math.min(bucket, HpScaledPassive.STEPS);
		return new Modifier(passive.attribute(), passive.modifierId(), capped * passive.perStep());
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

	/**
	 * The whole answer for one passive this tick, so the adapter never reads the passive
	 * behind the decision's back. No stacks plus no movement means nothing to do; no stacks
	 * after movement means taking off what those stacks earned.
	 */
	public static Plan planFor(ConditionalPassive passive, int stacks, boolean stacksMoved) {
		Modifier wanted = forStacks(passive, stacks);
		if (wanted != null) {
			return new Plan.Write(wanted);
		}
		if (stacksMoved && passive != null) {
			return new Plan.Remove(passive.attribute(), passive.modifierId());
		}
		return new Plan.Skip();
	}

	/**
	 * The whole answer for one HP-scaled passive this tick, in the same shape as
	 * {@link #planFor}: write the bucket's modifier, take off what a dead bucket earned,
	 * or nothing. The bucket is recomputed from live HP every tick and the writer's
	 * early-out covers no-op ticks, so unlike conditionals there is no moved-set.
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