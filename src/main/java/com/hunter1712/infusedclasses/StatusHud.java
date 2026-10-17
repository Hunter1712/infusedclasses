package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The strip's rows decision: given the last snapshot and the millis elapsed since it
 * arrived, returns the whole strip as rows. Pure — no screen, player or clock enters,
 * so the layout, the countdowns and the show/hide rules are assertable in JUnit and
 * the HUD element only draws what this returns.
 *
 * <p>Row order is fixed: the anchored stance (Legionary), the signature row, the
 * ultimate row, then the state rows (Momentum, Streak). Windows ride beside their lane
 * chip as extra chips on the same row. Badges are slot and state names, never keys:
 * {@code SIG}/{@code ULT}, {@code MOM}, {@code STR}, {@code GLA}/{@code SCU},
 * {@code IMP}/{@code TES}, {@code AEQ}, {@code ALL}, plus the Roulette outcome initials.
 * A ready lane draws dim with no number; every live chip draws lit. Words never enter
 * the strip — ability, passive, outcome and stance names stay in the fire lines and
 * {@code /class info} — so a chip speaks in its badge and its number alone. A classless
 * or unknown class id renders no rows at all.
 */
public final class StatusHud {
	public static final String SIG = "SIG";
	public static final String ULT = "ULT";
	public static final String MOM = "MOM";
	public static final String STR = "STR";
	public static final String GLA = "GLA";
	public static final String SCU = "SCU";
	public static final String IMP = "IMP";
	public static final String TES = "TES";
	public static final String AEQ = "AEQ";
	public static final String ALL = "ALL";

	/** One chip: its badge, the number it counts (or empty for a ready lane), and lit versus dim. */
	public record Chip(String badge, String value, boolean lit) {
	}

	/** One row: the chips that draw together on one line, a lane chip followed by its riders. */
	public record Row(List<Chip> chips) {
	}

	private StatusHud() {
	}

	/** The whole strip at this snapshot plus elapsed: empty for classless and unknown classes. */
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
					status.stance() == RotateAbility.Stance.GLADIUS ? GLA : SCU, "", true))));
		}
		rows.add(signatureRow(status, def, elapsed));
		rows.add(ultimateRow(status, def, elapsed));
		ConditionalPassive momentum = def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
		if (momentum != null && status.momentumStacks() > 0) {
			rows.add(new Row(List.of(new Chip(MOM,
					status.momentumStacks() + " " + countdown(status.momentumGraceRemainingMs() - elapsed), true))));
		}
		if (def.critRule().hasDice() && status.streak() > 0) {
			rows.add(new Row(List.of(new Chip(STR, Integer.toString(status.streak()), true))));
		}
		return List.copyOf(rows);
	}

	/** The signature lane plus its riders: the stride for Rush, the flip buffs for Rotate, the outcome for Roulette. */
	private static Row signatureRow(PlayerStatusPayload status, ClassDefinition def, long elapsed) {
		List<Chip> chips = new ArrayList<>();
		if (def.hasAbility(ClassRegistry.RUSH_ABILITY)) {
			chips.add(lane(SIG, status.rushRemainingMs(), elapsed));
			// The stride rider outlives the load: it keeps counting after the settle
			// spends the bonus, and the `+` is what drops. The window expiring drops it.
			if (status.rushWindowRemainingMs() > elapsed) {
				chips.add(new Chip("", (status.rushBonusPending() ? "+" : "")
						+ countdown(status.rushWindowRemainingMs() - elapsed), true));
			}
		} else if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			chips.add(lane(SIG, status.rotateRemainingMs(), elapsed));
			// Impetus is one-shot (pending only), so its rider vanishes on the spend;
			// Testudo is the timed catch, live for its window in scutum.
			if (status.impetusPending() && status.flipWindowRemainingMs() > elapsed) {
				chips.add(new Chip(IMP, countdown(status.flipWindowRemainingMs() - elapsed), true));
			}
			if (status.flipTarget() == RotateAbility.Stance.SCUTUM && status.flipWindowRemainingMs() > elapsed) {
				chips.add(new Chip(TES, countdown(status.flipWindowRemainingMs() - elapsed), true));
			}
		} else if (def.hasAbility(ClassRegistry.ROULETTE_ABILITY)) {
			chips.add(lane(SIG, status.rouletteRemainingMs(), elapsed));
			RouletteAbility.Outcome outcome = status.rouletteOutcome();
			if (showsOutcome(outcome) && status.rouletteWindowRemainingMs() > elapsed) {
				chips.add(new Chip(outcome.label().substring(0, 1),
						countdown(status.rouletteWindowRemainingMs() - elapsed), true));
			}
		}
		return new Row(List.copyOf(chips));
	}

	/** The ultimate lane plus its rider: the bare empower count for Rend, the oath chip for AEQ, ALL for All In. */
	private static Row ultimateRow(PlayerStatusPayload status, ClassDefinition def, long elapsed) {
		List<Chip> chips = new ArrayList<>();
		if (def.hasAbility(ClassRegistry.REND_ABILITY)) {
			chips.add(lane(ULT, status.rendRemainingMs(), elapsed));
			// Countdown-only: the empower rider exists exactly while its next-hit
			// snapshot is unspent, so it vanishes on the settle instead of counting on.
			if (status.rendBonusPending() && status.rendWindowRemainingMs() > elapsed) {
				chips.add(new Chip("", countdown(status.rendWindowRemainingMs() - elapsed), true));
			}
		} else if (def.hasAbility(ClassRegistry.AEQUILIBRIUM_ABILITY)) {
			chips.add(lane(ULT, status.oathRemainingMs(), elapsed));
			if (status.aequilibriumWindowRemainingMs() > elapsed) {
				chips.add(new Chip(AEQ, countdown(status.aequilibriumWindowRemainingMs() - elapsed), true));
			}
		} else if (def.hasAbility(ClassRegistry.ALL_IN_ABILITY)) {
			chips.add(lane(ULT, status.allInRemainingMs(), elapsed));
			if (status.allInWindowRemainingMs() > elapsed) {
				chips.add(new Chip(ALL, countdown(status.allInWindowRemainingMs() - elapsed), true));
			}
		}
		return new Row(List.copyOf(chips));
	}

	/**
	 * Whether an outcome draws its chip. Zero is the dead band, so it draws nothing;
	 * every other band grants something while its window lives.
	 */
	private static boolean showsOutcome(RouletteAbility.Outcome outcome) {
		return outcome != null && outcome != RouletteAbility.Outcome.ZERO;
	}

	/** A lane chip: lit with its countdown while cooling, dim with no number the moment it is ready. */
	private static Chip lane(String badge, long remainingMs, long elapsedMs) {
		return remainingMs > elapsedMs
				? new Chip(badge, countdown(remainingMs - elapsedMs), true)
				: new Chip(badge, "", false);
	}

	/**
	 * Countdown text for the strip: whole seconds while the nearest integer is ten or
	 * more, one decimal under, clamped at zero. The whole-second band starts at 9_950
	 * so the tenths branch never rounds up to a flashing {@code 10.0s}; chat keeps the
	 * one-decimal shape everywhere ({@code ChatLine.cooldown}).
	 */
	private static String countdown(long remainingMs) {
		long remaining = Math.max(0L, remainingMs);
		return remaining >= 9_950L
				? String.format(Locale.ROOT, "%ds", (remaining + 500) / 1000)
				: String.format(Locale.ROOT, "%.1fs", remaining / 1000.0);
	}
}
