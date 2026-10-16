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
 * A ready lane draws dim with no number; every live chip draws lit. Labels come from
 * the class definition and the ability constants, so copy cannot drift. A classless or
 * unknown class id renders no rows at all.
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

	/** One chip: its badge box, its text label, its value text, and lit versus dim. */
	public record Chip(String badge, String label, String value, boolean lit) {
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
					status.stance() == RotateAbility.Stance.GLADIUS ? GLA : SCU,
					status.stance().displayName(), "", true))));
		}
		rows.add(signatureRow(status, def, elapsed));
		rows.add(ultimateRow(status, def, elapsed));
		ConditionalPassive momentum = def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
		if (momentum != null && status.momentumStacks() > 0) {
			rows.add(new Row(List.of(new Chip(MOM, momentum.name(),
					status.momentumStacks() + " " + countdown(status.momentumGraceRemainingMs() - elapsed), true))));
		}
		if (def.critRule().hasDice() && status.streak() > 0) {
			rows.add(new Row(List.of(new Chip(STR, CritRule.STREAK, Integer.toString(status.streak()), true))));
		}
		return List.copyOf(rows);
	}

	/** The signature lane plus its riders: stride for Rush, the flip buffs for Rotate, the outcome for Roulette. */
	private static Row signatureRow(PlayerStatusPayload status, ClassDefinition def, long elapsed) {
		List<Chip> chips = new ArrayList<>();
		if (def.hasAbility(ClassRegistry.RUSH_ABILITY)) {
			chips.add(lane(SIG, RushAbility.NAME, status.rushRemainingMs(), elapsed));
			// The stride rider outlives the load: it keeps counting after the settle
			// spends the bonus, and the `+` is what drops. The window expiring drops it.
			if (status.rushWindowRemainingMs() > elapsed) {
				chips.add(new Chip("", "", (status.rushBonusPending() ? "+" : "")
						+ countdown(status.rushWindowRemainingMs() - elapsed), true));
			}
		} else if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			chips.add(lane(SIG, RotateAbility.NAME, status.rotateRemainingMs(), elapsed));
			// Impetus is one-shot (pending only), so its rider vanishes on the spend;
			// Testudo is the timed catch, live for its window in scutum.
			if (status.impetusPending() && status.flipWindowRemainingMs() > elapsed) {
				chips.add(new Chip(IMP, RotateAbility.IMPETUS_NAME,
						countdown(status.flipWindowRemainingMs() - elapsed), true));
			}
			if (status.flipTarget() == RotateAbility.Stance.SCUTUM && status.flipWindowRemainingMs() > elapsed) {
				chips.add(new Chip(TES, RotateAbility.TESTUDO_NAME,
						countdown(status.flipWindowRemainingMs() - elapsed), true));
			}
		} else if (def.hasAbility(ClassRegistry.ROULETTE_ABILITY)) {
			chips.add(lane(SIG, RouletteAbility.NAME, status.rouletteRemainingMs(), elapsed));
			RouletteAbility.Outcome outcome = status.rouletteOutcome();
			if (showsOutcome(outcome) && status.rouletteWindowRemainingMs() > elapsed) {
				chips.add(new Chip(outcome.label().substring(0, 1), outcome.label(),
						countdown(status.rouletteWindowRemainingMs() - elapsed), true));
			}
		}
		return new Row(List.copyOf(chips));
	}

	/** The ultimate lane plus its rider: empower for Rend, AEQ for the oath, ALL for All In. */
	private static Row ultimateRow(PlayerStatusPayload status, ClassDefinition def, long elapsed) {
		List<Chip> chips = new ArrayList<>();
		if (def.hasAbility(ClassRegistry.REND_ABILITY)) {
			chips.add(lane(ULT, RendAbility.NAME, status.rendRemainingMs(), elapsed));
			// Countdown-only: the empower rider exists exactly while its next-hit
			// snapshot is unspent, so it vanishes on the settle instead of counting on.
			if (status.rendBonusPending() && status.rendWindowRemainingMs() > elapsed) {
				chips.add(new Chip("", "", countdown(status.rendWindowRemainingMs() - elapsed), true));
			}
		} else if (def.hasAbility(ClassRegistry.AEQUILIBRIUM_ABILITY)) {
			chips.add(lane(ULT, AequilibriumAbility.NAME, status.oathRemainingMs(), elapsed));
			if (status.aequilibriumWindowRemainingMs() > elapsed) {
				chips.add(new Chip(AEQ, AequilibriumAbility.NAME,
						countdown(status.aequilibriumWindowRemainingMs() - elapsed), true));
			}
		} else if (def.hasAbility(ClassRegistry.ALL_IN_ABILITY)) {
			chips.add(lane(ULT, AllInAbility.NAME, status.allInRemainingMs(), elapsed));
			if (status.allInWindowRemainingMs() > elapsed) {
				chips.add(new Chip(ALL, AllInAbility.NAME,
						countdown(status.allInWindowRemainingMs() - elapsed), true));
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
	private static Chip lane(String badge, String label, long remainingMs, long elapsedMs) {
		if (remainingMs > elapsedMs) {
			return new Chip(badge, label, countdown(remainingMs - elapsedMs), true);
		}
		return new Chip(badge, label, "", false);
	}

	/** One-decimal countdown, clamped at zero; the same `%.1fs` the chat lines use. */
	private static String countdown(long remainingMs) {
		return String.format(Locale.ROOT, "%.1fs", Math.max(0L, remainingMs) / 1000.0);
	}
}
