package com.hunter1712.infusedclasses;

import java.util.List;

/**
 * Conditional passive, the one live shape: a bar fed by earning events and their amounts,
 * a cap, three-checkpoint tiers of attributes, and a grace before the bar bleeds one stack
 * per second. Plain data, no game bootstrap needed, so tests assert on it directly. The
 * picker copy is generated from these numbers, so it can say what the passive does but
 * never an earning event its earns do not carry.
 *
 * @param name the passive's word, as the stack and the card draw it
 * @param earns the events that feed the bar, each with its own amount and its own copy
 * @param maxStacks the bar's ceiling: headroom above the top tier is spend reserve
 * @param tiers the checkpoints, ascending: the bar reads the last one it has reached
 * @param graceSeconds quiet seconds after a feed before the bar starts bleeding
 */
public record ConditionalPassive(
		String name,
		List<Earn> earns,
		int maxStacks,
		List<Tier> tiers,
		int graceSeconds
) {
	/** One earning event: the trigger plus what it pays, so a double-fed bar stays one mechanism. */
	public record Earn(Trigger trigger, int stacks) {
	}

	/**
	 * One checkpoint: the bar's floor for this row, its word, and what the row is worth
	 * in armor, move-speed weight (an absolute delta; negative is slower) and knockback
	 * resistance.
	 */
	public record Tier(int threshold, String name, double armor, double weight, double knockback) {
	}

	/** Whether any earning event names this trigger. */
	public boolean earnsOn(Trigger trigger) {
		return amountFor(trigger) > 0;
	}

	/** What one feed of this trigger pays, 0 when nothing earns on it. */
	public int amountFor(Trigger trigger) {
		for (Earn earn : earns) {
			if (earn.trigger().equals(trigger)) {
				return earn.stacks();
			}
		}
		return 0;
	}

	/** The row the bar has reached, null below the first checkpoint. The last row holds above its floor. */
	public Tier tierFor(int stacks) {
		Tier reached = null;
		for (Tier tier : tiers) {
			if (stacks >= tier.threshold()) {
				reached = tier;
			}
		}
		return reached;
	}

	/** The reached row's word, empty below the first checkpoint. */
	public String tierNameFor(int stacks) {
		Tier tier = tierFor(stacks);
		return tier == null ? "" : tier.name();
	}

	/** Short picker copy. Generated from the numbers so it cannot drift from behaviour. */
	public String fullDesc() {
		StringBuilder copy = new StringBuilder();
		for (Earn earn : earns) {
			copy.append(earn.trigger().earn()).append(": +").append(earn.stacks()).append(". ");
		}
		copy.append("Max ").append(maxStacks).append(". ");
		copy.append(graceSeconds).append("s no feed: -1/s. ");
		for (Tier tier : tiers) {
			copy.append(tier.threshold()).append(' ').append(tier.name()).append(": +")
					.append(amount(tier.armor())).append(" armor, ")
					.append(amount(tier.weight())).append(" speed, +")
					.append(amount(tier.knockback())).append(" knockback resistance. ");
		}
		return copy.toString().trim();
	}

	/** Whole numbers read as "1", not "1.0", so the copy matches the game. */
	static String amount(double value) {
		return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
	}
}
