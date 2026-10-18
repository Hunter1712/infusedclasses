package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * The ability roster: one row per ability the mod fires, and the one place its word, its
 * lane read, the stack's view of it and its slot binds live. Consumers walk the rows —
 * {@code /class info} draws its cooldown lines, the HUD's rows decision draws each lane
 * line with its riders, {@link BindMorph} resolves what the reused slots mean — so an
 * ability's facts cannot drift between them. Words are the ability modules' own NAME
 * constants; a lane read is a pure read of the runtime state, while the view reads the
 * wire snapshot a client already holds.
 *
 * <p>The ultimate bind keys on the {@link #ROTATE} row (ADR-0009): the Legionary's
 * Aequilibrium row carries the word and the shared oath lane, not a second bind.
 */
public final class AbilityRoster {
	/**
	 * One ability's shared facts: the id that gates it, its Title Case word, its lane
	 * read over the runtime state, the stack's view of the row, and the slot binds it
	 * answers (null when the ability answers none).
	 */
	public record Row(
			Identifier ability,
			String name,
			ToLongFunction<PlayerRuntime.State> laneRemaining,
			View view,
			BindMorph.SignatureTarget signature,
			BindMorph.UltimateTarget ultimate) {
	}

	/** The stack's view of a row: the payload's lane read, and the rider rules that draw beside it. */
	public record View(ToLongFunction<PlayerStatusPayload> laneRemaining, List<Rider> riders) {
	}

	/**
	 * One rider rule: whether it draws beside the lane ({@code wanted}), the window it
	 * reads, and what it says — usually nothing, since the lane's word tells the story.
	 */
	public record Rider(
			Predicate<PlayerStatusPayload> wanted,
			ToLongFunction<PlayerStatusPayload> remaining,
			Function<PlayerStatusPayload, String> label) {
	}

	/** A window rider that draws whenever its window lives, saying nothing of its own. */
	private static Rider window(ToLongFunction<PlayerStatusPayload> remaining) {
		return new Rider(status -> true, remaining, status -> "");
	}

	/** A window rider that draws only while {@code wanted} holds — a pending bonus, a timed stance. */
	private static Rider window(Predicate<PlayerStatusPayload> wanted, ToLongFunction<PlayerStatusPayload> remaining) {
		return new Rider(wanted, remaining, status -> "");
	}

	public static final Row RUSH = new Row(ClassRegistry.RUSH_ABILITY, RushAbility.NAME,
			PlayerRuntime.State::rushRemaining,
			new View(PlayerStatusPayload::rushRemainingMs,
					// The Rush window's rider counts while the window lives, spent or not: the
					// loaded bonus draws nothing of its own, the number is the whole story.
					List.of(window(PlayerStatusPayload::rushWindowRemainingMs))),
			BindMorph.SignatureTarget.RUSH, null);
	public static final Row REND = new Row(ClassRegistry.REND_ABILITY, RendAbility.NAME,
			PlayerRuntime.State::rendRemaining,
			new View(PlayerStatusPayload::rendRemainingMs,
					// Countdown-only: the rider exists exactly while its next-hit snapshot is
					// unspent, so it vanishes on the settle instead of counting on.
					List.of(window(PlayerStatusPayload::rendBonusPending,
							PlayerStatusPayload::rendWindowRemainingMs))),
			null, BindMorph.UltimateTarget.REND);
	public static final Row ROTATE = new Row(ClassRegistry.ROTATE_ABILITY, RotateAbility.NAME,
			PlayerRuntime.State::rotateRemaining,
			new View(PlayerStatusPayload::rotateRemainingMs, List.of(
					// Impetus is one-shot (pending only), so its rider vanishes on the spend;
					window(PlayerStatusPayload::impetusPending, PlayerStatusPayload::flipWindowRemainingMs),
					// Testudo is the timed catch, live for its window in scutum.
					window(status -> status.flipTarget() == RotateAbility.Stance.SCUTUM,
							PlayerStatusPayload::flipWindowRemainingMs))),
			BindMorph.SignatureTarget.ROTATE, BindMorph.UltimateTarget.AEQUILIBRIUM);
	public static final Row AEQUILIBRIUM = new Row(ClassRegistry.AEQUILIBRIUM_ABILITY, AequilibriumAbility.NAME,
			PlayerRuntime.State::oathRemaining,
			new View(PlayerStatusPayload::oathRemainingMs,
					List.of(window(PlayerStatusPayload::aequilibriumWindowRemainingMs))),
			null, null);
	public static final Row ROULETTE = new Row(ClassRegistry.ROULETTE_ABILITY, RouletteAbility.NAME,
			PlayerRuntime.State::rouletteRemaining,
			new View(PlayerStatusPayload::rouletteRemainingMs,
					List.of(new Rider(
							// Zero is the dead band and draws nothing; every other band draws
							// its name, because which band spun is the information.
							status -> status.rouletteOutcome() != null
									&& status.rouletteOutcome() != RouletteAbility.Outcome.ZERO,
							PlayerStatusPayload::rouletteWindowRemainingMs,
							status -> status.rouletteOutcome() == null ? "" : status.rouletteOutcome().label()))),
			BindMorph.SignatureTarget.ROULETTE, null);
	public static final Row ALL_IN = new Row(ClassRegistry.ALL_IN_ABILITY, AllInAbility.NAME,
			PlayerRuntime.State::allInRemaining,
			new View(PlayerStatusPayload::allInRemainingMs,
					List.of(window(PlayerStatusPayload::allInWindowRemainingMs))),
			null, BindMorph.UltimateTarget.ALL_IN);

	/** Rows in lane order: the order {@code /class info} prints and the classes carry their actives in. */
	private static final List<Row> ALL = List.of(RUSH, REND, ROTATE, AEQUILIBRIUM, ROULETTE, ALL_IN);

	private AbilityRoster() {
	}

	public static List<Row> all() {
		return ALL;
	}

	/** The row for an ability id, null when the id is not an ability this mod fires. */
	public static Row byAbility(Identifier ability) {
		for (Row row : ALL) {
			if (row.ability().equals(ability)) {
				return row;
			}
		}
		return null;
	}
}
