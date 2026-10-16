package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Roulette decisions: the Gambler's free wheel on {@code R}. Pure gate-and-arm shape
 * on its own 25s lane: one roll over seven bands, good and bad, no HP cost anywhere.
 * Fired spins announce their outcome, arm the lane and stamp the 10s outcome window;
 * rejects arm and spend nothing. Vigor and Jackpot heal at fire; Ward and Haste stat
 * the player through the passive applier's window path while live; Edge, Jackpot and
 * Bust deal mults are read by the melee plan. Zero is a dead band: fired, announced,
 * and worth nothing.
 */
public final class RouletteAbility {
	public static final String NAME = "Roulette";
	/** Outcome window in ticks: 10 seconds, tracked as a press timestamp on the runtime. */
	public static final int WINDOW_TICKS = 200;
	/** Edge: melee dealt x1.25 while its window is live. */
	public static final double EDGE_MULT = 1.25;
	/** Ward: +2 armor while its window is live. */
	public static final double WARD_ARMOR = 2.0;
	/** Haste: +0.02 movement speed while its window is live. */
	public static final double HASTE_SPEED = 0.02;
	/** Vigor: instant 3 HP heal at fire. */
	public static final float VIGOR_HEAL = 3.0F;
	/** Jackpot: Edge, Ward and Haste plus an instant 4 HP heal. */
	public static final float JACKPOT_HEAL = 4.0F;
	/** Bust: melee dealt x0.75 while its window is live. */
	public static final double BUST_MULT = 0.75;
	/** Ward modifier id, resolved by the applier. */
	public static final Identifier WARD_ID = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "roulette_ward");
	/** Haste modifier id, resolved by the applier. */
	public static final Identifier HASTE_ID = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "roulette_haste");
	/** Cast sound id, resolved by the adapter; the pling is the spin. */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("block.note_block.pling");

	/**
	 * The seven bands, in wheel order: the roll cuts land under {@link #outcomeFor}.
	 * Band tops are floats like the rolls they cut: a roll of {@code 0.88F} lands Bust,
	 * not Jackpot, because the cut compares at the same precision the wheel is spun at.
	 */
	public enum Outcome {
		EDGE("Edge", 0.20F, EDGE_MULT, 0),
		WARD("Ward", 0.40F, 1.0, 0),
		HASTE("Haste", 0.60F, 1.0, 0),
		VIGOR("Vigor", 0.80F, 1.0, VIGOR_HEAL),
		JACKPOT("Jackpot", 0.88F, EDGE_MULT, JACKPOT_HEAL),
		BUST("Bust", 0.96F, BUST_MULT, 0),
		ZERO("Zero", 1.00F, 1.0, 0);

		private final String label;
		private final float bandTop;
		private final double dealMult;
		private final float healHp;

		Outcome(String label, float bandTop, double dealMult, float healHp) {
			this.label = label;
			this.bandTop = bandTop;
			this.dealMult = dealMult;
			this.healHp = healHp;
		}

		/** The outcome's own name, as announced. */
		public String label() {
			return label;
		}

		/**
		 * The band's odds in whole percent, read off its own top and the one below, so
		 * the copy cannot drift from the cuts. Whole numbers by construction: the tops
		 * are whole percents.
		 */
		public int percent() {
			float lower = ordinal() == 0 ? 0.0F : values()[ordinal() - 1].bandTop;
			return Math.round((bandTop - lower) * 100F);
		}

		/** Melee dealt mult while this outcome's window is live: Edge and Jackpot x1.25, Bust x0.75, the rest 1.0. */
		public double dealMult() {
			return dealMult;
		}

		/** Instant heal at fire, 0 for every outcome but Vigor and Jackpot. */
		public float healHp() {
			return healHp;
		}
	}

	/** What firing should do. {@code band} is the landed outcome on fire, null on rejects. */
	public record Result(AbilityOutcome outcome, Outcome band) {
		public static Result fired(Outcome band) {
			return new Result(AbilityOutcome.fired(new ChatLine(band.label() + "!", null)), band);
		}

		public static Result wrongClass() {
			return new Result(AbilityOutcome.rejected(), null);
		}

		public static Result onCooldown(long remainingMs) {
			return new Result(AbilityOutcome.onCooldown(NAME, remainingMs), null);
		}
	}

	private static final PassiveEffect.Modifier WARD_PLATE =
			new PassiveEffect.Modifier(ClassRegistry.ARMOR, WARD_ID, WARD_ARMOR);
	private static final PassiveEffect.Modifier HASTE_ALACRITY =
			new PassiveEffect.Modifier(ClassRegistry.MOVEMENT_SPEED, HASTE_ID, HASTE_SPEED);

	private RouletteAbility() {
	}

	/** Picker copy, generated from the numbers so it cannot drift from behaviour. */
	public static String desc() {
		return "One roll: Edge " + Outcome.EDGE.percent() + "% x" + ConditionalPassive.amount(EDGE_MULT)
				+ ", Ward " + Outcome.WARD.percent() + "% +" + ConditionalPassive.amount(WARD_ARMOR)
				+ " armor, Haste " + Outcome.HASTE.percent() + "% +" + ConditionalPassive.amount(HASTE_SPEED)
				+ " speed, Vigor " + Outcome.VIGOR.percent() + "% heal " + ConditionalPassive.amount(VIGOR_HEAL)
				+ ", Jackpot " + Outcome.JACKPOT.percent() + "% all + heal " + ConditionalPassive.amount(JACKPOT_HEAL)
				+ ", Bust " + Outcome.BUST.percent() + "% x" + ConditionalPassive.amount(BUST_MULT)
				+ ", Zero " + Outcome.ZERO.percent() + "%; "
				+ (WINDOW_TICKS / 20) + "s (R, " + (Cooldowns.ROULETTE_MS / 1000) + "s).";
	}

	/** Every stat modifier the wheel can write: both the apply and the removal paths iterate this. */
	public static List<PassiveEffect.Modifier> allStatModifiers() {
		return List.of(WARD_PLATE, HASTE_ALACRITY);
	}

	/** The stats the given outcome grants while its window is live. Timed, never consumed: expiry is silent. */
	public static List<PassiveEffect.Modifier> statAuraFor(Outcome outcome) {
		return switch (outcome) {
			case WARD -> List.of(WARD_PLATE);
			case HASTE -> List.of(HASTE_ALACRITY);
			case JACKPOT -> List.of(WARD_PLATE, HASTE_ALACRITY);
			default -> List.of();
		};
	}

	/** The band one roll cuts into: walk the tops in wheel order, remainder Zero. */
	public static Outcome outcomeFor(float roll) {
		for (Outcome outcome : Outcome.values()) {
			if (roll < outcome.bandTop) {
				return outcome;
			}
		}
		return Outcome.ZERO;
	}

	/**
	 * Decides spinning for Gamblers off cooldown: the outcome is already cut from the
	 * caller's roll. Pure and read-only: only {@link #fire} arms the lane and window.
	 */
	public static Result trigger(ClassDefinition def, long rouletteRemaining, float roll) {
		if (def == null || !def.hasAbility(ClassRegistry.ROULETTE_ABILITY)) {
			return Result.wrongClass();
		}
		if (rouletteRemaining > 0) {
			return Result.onCooldown(rouletteRemaining);
		}
		return Result.fired(outcomeFor(roll));
	}

	/**
	 * Gate and arm together: the production path. A fired spin arms the lane and stamps
	 * the outcome window together; rejected presses leave both untouched. Free: no HP is
	 * spent, checked or touched anywhere.
	 */
	public static Result fire(ClassDefinition def, PlayerRuntime runtime, long now, float roll) {
		Result result = trigger(def, runtime.rouletteRemaining(now), roll);
		return AbilityGate.gate(result, result.outcome().fired(),
				() -> runtime.tryUseRoulette(now, result.band()),
				() -> runtime.rouletteRemaining(now), Result::onCooldown);
	}

	/** Melee dealt mult of the live spin, 1.0 when no outcome window is live. Read by the melee plan. */
	public static double dealMult(PlayerRuntime runtime, long now) {
		if (runtime == null) {
			return 1.0;
		}
		Outcome outcome = runtime.rouletteOutcome(now);
		return outcome == null ? 1.0 : outcome.dealMult();
	}
}
