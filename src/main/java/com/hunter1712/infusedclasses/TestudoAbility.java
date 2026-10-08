package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Testudo decisions: the Legionary's Scutum payoff on {@code G}. Pure gate-and-arm
 * shape on the shared oath lane: only Scutum fires, firing arms the lane for both
 * oath abilities and stamps a 6s window. Armor and knockback immunity ride the
 * passive applier while live; the dealt penalty rides the melee hook.
 */
public final class TestudoAbility {
	public static final String NAME = "Testudo";
	/** Plate granted while live: +8 armor. */
	public static final double TESTUDO_ARMOR = 8.0;
	/** Tradeoff: melee dealt x0.7 while live (x0.56 with the Scutum stance). */
	public static final double TESTUDO_DEAL_MULT = 0.7;
	/** Window factor for outgoing melee while Testudo is live: x0.7 dealt. */
	public static final double TESTUDO_WINDOW_DEAL_FACTOR = 0.7;
	/** Payoff window in ticks: 6 seconds, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 120;
	/** Modifier id for the window plate. */
	public static final Identifier TESTUDO_PLATE_ID =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "testudo_plate");
	/** Modifier id for the window knockback immunity (+1 resistance). */
	public static final Identifier TESTUDO_BRACE_ID =
			Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "testudo_brace");
	/** Cast sound id, resolved by the adapter; fail silent if missing. */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("item.shield.block");
	/** Picker-copy tail evoking the cast, kept next to {@link #CAST_SOUND} so the two change together. */
	public static final String CAST_WORD = "Hold!";

	/**
	 * The window aura as modifiers: what sits on the player while Testudo is live.
	 * Single source for the applier's write and remove paths, so they cannot drift.
	 * Shared instance: the tick iterates it, never mutates.
	 */
	public static List<PassiveEffect.Modifier> windowModifiers() {
		return WINDOW_AURA;
	}

	private static final List<PassiveEffect.Modifier> WINDOW_AURA = List.of(
			new PassiveEffect.Modifier(ClassRegistry.ARMOR, TESTUDO_PLATE_ID, TESTUDO_ARMOR),
			new PassiveEffect.Modifier(ClassRegistry.KNOCKBACK_RESISTANCE, TESTUDO_BRACE_ID, 1.0));

	public enum Kind {
		FIRED,
		WRONG_CLASS,
		ON_COOLDOWN
	}

	public record Result(Kind kind, long remainingMs, ChatLine announcement) {
		public static Result fired() {
			return new Result(Kind.FIRED, 0, new ChatLine(NAME + "!", ChatFormatting.GRAY));
		}

		public static Result wrongClass() {
			return new Result(Kind.WRONG_CLASS, 0, null);
		}

		public static Result onCooldown(long remainingMs) {
			return new Result(Kind.ON_COOLDOWN, remainingMs, ChatLine.cooldown(NAME, remainingMs));
		}
	}

	private TestudoAbility() {
	}

	/** Decides firing for Scutum Legionaries off the shared oath lane. Other stances and classes stay silent. Pure and read-only: only {@link #fire} arms the lane and the window. */
	public static Result trigger(ClassDefinition def, long oathRemaining, RotateAbility.Stance stance) {
		if (def == null || !def.hasAbility(ClassRegistry.TESTUDO_ABILITY)
				|| stance != RotateAbility.Stance.SCUTUM) {
			return Result.wrongClass();
		}
		if (oathRemaining > 0) {
			return Result.onCooldown(oathRemaining);
		}
		return Result.fired();
	}

	/**
	 * Gate and arm together: the production path. A fired press arms the shared
	 * oath lane and stamps the window together, locking Impetus for 50s too;
	 * rejected presses leave both untouched.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now) {
		Result result = trigger(def, runtime.oathRemaining(now), runtime.stance());
		return AbilityGate.gate(result, result.kind() == Kind.FIRED, () -> runtime.tryUseTestudo(now),
				() -> runtime.oathRemaining(now), Result::onCooldown);
	}
}
