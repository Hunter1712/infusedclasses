package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

/**
 * Rend decisions: the Juggernaut's Momentum spender on {@code V}. Pure gate-and-arm shape:
 * on fire, the next melee hit landed inside the window deals {@code stacks * HP_PER_STACK}
 * bonus damage, and every Momentum stack is consumed up front, on its own 60s
 * lane. Zero stacks fizzles with a {@code No Momentum} line and arms nothing; a
 * window left unswung simply expires, stacks already spent. The consume plan rides in the
 * result ({@link Result#consume()}): the entry asks the passive applier to consume the
 * trigger-found passive, and the damage hook spends the stamp on the next hit.
 */
public final class RendAbility {
	public static final String NAME = "Rend";
	/** Bonus damage per Momentum stack in HP: half a heart each, 10 HP at full stacks. */
	public static final float HP_PER_STACK = 0.5F;
	/** Rend window in ticks: 6 seconds to land the hit, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 120;
	/** Cast sound id, resolved by the adapter; the anvil smash is the spend. */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("block.anvil.land");

	/**
	 * What firing should do. Consume is true only on fire, telling the adapter to drop
	 * every Momentum stack; a wrong-class press rejects silently, a zero-stack fizzle
	 * says its {@code No Momentum} line without a cast.
	 */
	public record Result(AbilityOutcome outcome, boolean consume) {
		public static Result fired() {
			return new Result(AbilityOutcome.fired(new ChatLine(NAME + "!", ChatFormatting.RED)), true);
		}

		public static Result wrongClass() {
			return new Result(AbilityOutcome.rejected(), false);
		}

		public static Result onCooldown(long remainingMs) {
			// Unstyled on purpose: this line has never carried a colour of its own.
			return new Result(AbilityOutcome.onCooldown(remainingMs), false);
		}

		public static Result noMomentum() {
			return new Result(AbilityOutcome.fizzled(new ChatLine("No Momentum", null)), false);
		}
	}

	private RendAbility() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		String bonus = "+" + ConditionalPassive.amount(HP_PER_STACK) + " per Momentum";
		return "Next hit in " + (WINDOW_TICKS / 20) + "s: " + bonus
				+ ", spends stacks (V, " + (Cooldowns.REND_MS / 1000) + "s).";
	}

	/** Bonus damage for a stack count: half a heart per stack. Nothing at zero or below. */
	public static float bonusFor(int stacks) {
		if (stacks <= 0) {
			return 0;
		}
		return stacks * HP_PER_STACK;
	}

	/** Decides firing for Juggernauts off cooldown holding stacks. Zero stacks fizzles before the cooldown is even read. Pure and read-only: only {@link #fire} arms the cooldown. */
	public static Result trigger(ClassDefinition def, long rendRemaining, int stacks) {
		if (def == null || !def.hasAbility(ClassRegistry.REND_ABILITY)) {
			return Result.wrongClass();
		}
		if (stacks <= 0) {
			return Result.noMomentum();
		}
		if (rendRemaining > 0) {
			return Result.onCooldown(rendRemaining);
		}
		return Result.fired();
	}

	/**
	 * Arms the lane and stamps the window's bonus together on a fired press; rejected
	 * presses leave the lane untouched. The arm reads the same lane and now the trigger just
	 * checked, so it cannot fail.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		ConditionalPassive spent = def == null ? null : def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
		int stacks = spent == null ? 0 : runtime.stacks(spent);
		Result result = trigger(def, runtime.rendRemaining(now), stacks);
		if (result.outcome().fired()) {
			runtime.tryUseRend(now, stacks);
		}
		return result;
	}
}
