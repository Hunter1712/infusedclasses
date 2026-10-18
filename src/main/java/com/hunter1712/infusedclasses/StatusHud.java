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
 * legend. The words are the owners' own: the {@link AbilityRoster} carries the ability
 * names, the stance its display name, the passive and the crit rule their own. Row order
 * is fixed: the anchored stance (Legionary), the signature row, the ultimate row, then
 * the state rows (Momentum, Streak). Each lane line is its roster row's: the word, the
 * countdown, and whatever riders the row's rules draw beside them — usually a dotted bare
 * window number, since one window per lane needs no word; the Roulette band rider is the
 * one rider that keeps its word, because which band spun is the information. A classless
 * or unknown class id renders no rows at all.
 *
 * <p>Countdowns are whole seconds, rounded up, so a live chip never reads zero; a ready
 * lane draws dim with no number and the missing number is what marks it ready.
 */
public final class StatusHud {
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
			RotateAbility.Stance stance = status.stance() == null
					? RotateAbility.Stance.SCUTUM : status.stance();
			rows.add(new Row(List.of(new Chip(stance.displayName(), "", true, false))));
		}
		rows.add(laneRow(status, signatureLane(def), elapsed));
		rows.add(laneRow(status, ultimateLane(def), elapsed));
		ConditionalPassive momentum = def.passiveForTrigger(ConditionalPassive.ON_HIT_DEALT);
		if (momentum != null && status.momentumStacks() > 0) {
			rows.add(new Row(List.of(new Chip(momentum.name(), momentumValue(status, elapsed), true, false))));
		}
		if (def.critRule().hasDice() && status.streak() > 0) {
			rows.add(new Row(List.of(new Chip(CritRule.STREAK, Integer.toString(status.streak()), true, false))));
		}
		// A definition whose abilities map to no lane draws no row rather than a blank one.
		rows.removeIf(row -> row.chips().isEmpty());
		return List.copyOf(rows);
	}

	/** The class-shape pick: which roster row the signature slot draws, null when the class carries none. */
	private static AbilityRoster.Row signatureLane(ClassDefinition def) {
		return firstCarried(def, AbilityRoster.RUSH, AbilityRoster.ROTATE, AbilityRoster.ROULETTE);
	}

	/** The class-shape pick: which roster row the ultimate slot draws, null when the class carries none. */
	private static AbilityRoster.Row ultimateLane(ClassDefinition def) {
		return firstCarried(def, AbilityRoster.REND, AbilityRoster.AEQUILIBRIUM, AbilityRoster.ALL_IN);
	}

	/** The first of these rows whose ability the class carries, null when it carries none of them. */
	private static AbilityRoster.Row firstCarried(ClassDefinition def, AbilityRoster.Row... rows) {
		for (AbilityRoster.Row row : rows) {
			if (def.hasAbility(row.ability())) {
				return row;
			}
		}
		return null;
	}

	/**
	 * One lane line, drawn from the row itself: the word and its countdown, then every
	 * rider the row's rules say draws — each with its own window countdown, bare unless
	 * the rule carries a word.
	 */
	private static Row laneRow(PlayerStatusPayload status, AbilityRoster.Row row, long elapsed) {
		if (row == null) {
			return new Row(List.of());
		}
		List<Chip> chips = new ArrayList<>();
		chips.add(lane(row.name(), row.view().laneRemaining().applyAsLong(status), elapsed));
		for (AbilityRoster.Rider rider : row.view().riders()) {
			long remaining = rider.remaining().applyAsLong(status);
			if (rider.wanted().test(status) && remaining > elapsed) {
				chips.add(rider(rider.label().apply(status), remaining - elapsed));
			}
		}
		return new Row(List.copyOf(chips));
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
