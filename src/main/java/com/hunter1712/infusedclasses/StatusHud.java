package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The stack's rows decision: given the last snapshot and the millis elapsed since it
 * arrived, returns the whole status stack as rows. Pure — no screen, player or clock
 * enters, so the roster, the countdowns and the show/hide rules are assertable in JUnit
 * and the HUD element only draws what this returns.
 *
 * <p>The stack speaks words: ability, stance and state names in Title Case, never badges
 * and never keys — a remapped key cannot make the stack lie, and a name reads without a
 * legend. Row order is fixed: the anchored stance (Legionary), the signature row, the
 * ultimate row, then the state rows (Momentum, Streak). Windows ride beside their lane
 * chip as dotted bare numbers — one window per lane, so the number needs no word; the
 * Roulette outcome rider is the one rider that keeps its word, because which band spun
 * is the information. A classless or unknown class id renders no rows at all.
 *
 * <p>Countdowns are whole seconds, rounded up, so a live chip never reads zero; a ready
 * lane draws dim with no number and the missing number is what marks it ready.
 */
public final class StatusHud {
	/** The stack's words: ability, stance and state names in Title Case, never keys. */
	public static final String RUSH = "Rush";
	public static final String REND = "Rend";
	public static final String ROTATE = "Rotate";
	public static final String AEQUILIBRIUM = "Aequilibrium";
	public static final String ROULETTE = "Roulette";
	public static final String ALL_IN = "All In";
	public static final String GLADIUS = "Gladius";
	public static final String SCUTUM = "Scutum";
	public static final String MOMENTUM = "Momentum";
	public static final String STREAK = "Streak";

	/**
	 * One chip: its words (empty on a bare rider), its number text (empty for a ready lane
	 * and for the stance), lit versus dim, and whether it rides another chip's line.
	 */
	public record Chip(String label, String value, boolean lit, boolean rider) {
	}

	/** One row: the chips that draw together on one line, a lane chip followed by its riders. */
	public record Row(List<Chip> chips) {
	}

	private StatusHud() {
	}

	/** The whole stack at this snapshot plus elapsed: empty for classless and unknown classes. */
	public static List<Row> rows(PlayerStatusPayload status, long elapsedMs) {
		ClassDefinition def = status.classId() == null || status.classId().isEmpty()
				? null
				: ClassRegistry.byId(Identifier.tryParse(status.classId()));
		if (def == null) {
			return List.of();
		}
		long elapsed = Math.max(0L, elapsedMs);
		List<Row> rows = new ArrayList<>();
		if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			rows.add(new Row(List.of(new Chip(
					status.stance() == RotateAbility.Stance.GLADIUS ? GLADIUS : SCUTUM, "", true, false))));
		}
		rows.add(signatureRow(status, def, elapsed));
		rows.add(ultimateRow(status, def, elapsed));
		ConditionalPassive momentum = def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
		if (momentum != null && status.momentumStacks() > 0) {
			rows.add(new Row(List.of(new Chip(MOMENTUM, momentumValue(status, elapsed), true, false))));
		}
		if (def.critRule().hasDice() && status.streak() > 0) {
			rows.add(new Row(List.of(new Chip(STREAK, Integer.toString(status.streak()), true, false))));
		}
		// A definition whose abilities map to no lane draws no row rather than a blank one.
		rows.removeIf(row -> row.chips().isEmpty());
		return List.copyOf(rows);
	}

	/** The signature lane plus its riders: the Rush window for Rush, the flip buffs for Rotate, the band for Roulette. */
	private static Row signatureRow(PlayerStatusPayload status, ClassDefinition def, long elapsed) {
		List<Chip> chips = new ArrayList<>();
		if (def.hasAbility(ClassRegistry.RUSH_ABILITY)) {
			chips.add(lane(RUSH, status.rushRemainingMs(), elapsed));
			// The Rush window's rider counts while the window lives, spent or not: the
			// loaded bonus draws nothing of its own, the number is the whole story.
			if (status.rushWindowRemainingMs() > elapsed) {
				chips.add(rider("", status.rushWindowRemainingMs() - elapsed));
			}
		} else if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			chips.add(lane(ROTATE, status.rotateRemainingMs(), elapsed));
			// Impetus is one-shot (pending only), so its rider vanishes on the spend;
			// Testudo is the timed catch, live for its window in scutum.
			if (status.impetusPending() && status.flipWindowRemainingMs() > elapsed) {
				chips.add(rider("", status.flipWindowRemainingMs() - elapsed));
			}
			if (status.flipTarget() == RotateAbility.Stance.SCUTUM && status.flipWindowRemainingMs() > elapsed) {
				chips.add(rider("", status.flipWindowRemainingMs() - elapsed));
			}
		} else if (def.hasAbility(ClassRegistry.ROULETTE_ABILITY)) {
			chips.add(lane(ROULETTE, status.rouletteRemainingMs(), elapsed));
			RouletteAbility.Outcome outcome = status.rouletteOutcome();
			if (showsOutcome(outcome) && status.rouletteWindowRemainingMs() > elapsed) {
				chips.add(rider(outcome.label(), status.rouletteWindowRemainingMs() - elapsed));
			}
		}
		return new Row(List.copyOf(chips));
	}

	/** The ultimate lane plus its rider: the bare Rend bonus count, the oath for AEQ, the window for All In. */
	private static Row ultimateRow(PlayerStatusPayload status, ClassDefinition def, long elapsed) {
		List<Chip> chips = new ArrayList<>();
		if (def.hasAbility(ClassRegistry.REND_ABILITY)) {
			chips.add(lane(REND, status.rendRemainingMs(), elapsed));
			// Countdown-only: the Rend rider exists exactly while its next-hit
			// snapshot is unspent, so it vanishes on the settle instead of counting on.
			if (status.rendBonusPending() && status.rendWindowRemainingMs() > elapsed) {
				chips.add(rider("", status.rendWindowRemainingMs() - elapsed));
			}
		} else if (def.hasAbility(ClassRegistry.AEQUILIBRIUM_ABILITY)) {
			chips.add(lane(AEQUILIBRIUM, status.oathRemainingMs(), elapsed));
			if (status.aequilibriumWindowRemainingMs() > elapsed) {
				chips.add(rider("", status.aequilibriumWindowRemainingMs() - elapsed));
			}
		} else if (def.hasAbility(ClassRegistry.ALL_IN_ABILITY)) {
			chips.add(lane(ALL_IN, status.allInRemainingMs(), elapsed));
			if (status.allInWindowRemainingMs() > elapsed) {
				chips.add(rider("", status.allInWindowRemainingMs() - elapsed));
			}
		}
		return new Row(List.copyOf(chips));
	}

	/**
	 * Whether an outcome draws its rider. Zero is the dead band, so it draws nothing;
	 * every other band grants something while its window lives.
	 */
	private static boolean showsOutcome(RouletteAbility.Outcome outcome) {
		return outcome != null && outcome != RouletteAbility.Outcome.ZERO;
	}

	/**
	 * Momentum's words: stacks, then the grace countdown while one is live. A stale
	 * snapshot — stacks still reported, grace already spent — prints bare stacks rather
	 * than a `0s`, so the number only ever shows live time.
	 */
	private static String momentumValue(PlayerStatusPayload status, long elapsed) {
		long grace = status.momentumGraceRemainingMs() - elapsed;
		return grace > 0
				? status.momentumStacks() + " \u00b7 " + countdown(grace)
				: Integer.toString(status.momentumStacks());
	}

	/** A lane chip: lit with its countdown while cooling, dim with no number the moment it is ready. */
	private static Chip lane(String label, long remainingMs, long elapsedMs) {
		return remainingMs > elapsedMs
				? new Chip(label, countdown(remainingMs - elapsedMs), true, false)
				: new Chip(label, "", false, false);
	}

	/** A rider chip: label usually empty — the lane's word tells the story — and its number is the ride. */
	private static Chip rider(String label, long remainingMs) {
		return new Chip(label, countdown(remainingMs), true, true);
	}

	/**
	 * Countdown text for the stack: whole seconds, rounded up, so the last second reads
	 * {@code 1s} until the moment the lane is ready and the number is gone.
	 */
	private static String countdown(long remainingMs) {
		return String.format(Locale.ROOT, "%ds", (Math.max(0L, remainingMs) + 999L) / 1000L);
	}
}
