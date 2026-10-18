package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * The ability roster: one row per ability the mod fires, and the one place its lane, its
 * gating id, its word and the stack's view of it live. Consumers walk the rows —
 * {@code /class info} draws its cooldown lines, the HUD's rows decision draws each lane
 * line with its riders, {@link BindMorph} resolves what the reused slots mean — so an
 * ability's facts cannot drift between them. Words are the ability modules' own NAME
 * constants; both lane reads derive from the row's {@link Lane}, so a row cannot draw one
 * lane and count down another.
 *
 * <p>The lane read stays split in two: server-side consumers read the runtime state, the
 * stack reads the wire snapshot a client already holds (ADR-0023). Riders are always
 * spelled over their own row's lane's window, so a rider cannot read someone else's.
 */
public final class AbilityRoster {
	/**
	 * One ability's shared facts: the lane it runs on, the id that gates it, its Title
	 * Case word, and the rider rules that draw beside its lane line.
	 */
	public record Row(Lane lane, Identifier ability, String name, List<Rider> riders) {
		/** The lane read over the runtime state: the server-side half of the stack's view. */
		public long laneRemaining(PlayerRuntime.State state) {
			return state.laneRemaining(lane);
		}

		/** The lane read over the wire snapshot: the client's half of the stack's view. */
		public long laneRemaining(PlayerStatusPayload status) {
			return status.lane(lane).remainingMs();
		}
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

	/** A rider's window remainder: the lane's own window, so a rider cannot read another lane's. */
	private static ToLongFunction<PlayerStatusPayload> windowRemaining(Lane lane) {
		return status -> status.lane(lane).windowRemainingMs();
	}

	/** Whether the lane's window still carries its one-shot. */
	private static Predicate<PlayerStatusPayload> pending(Lane lane) {
		return status -> status.lane(lane).windowPending();
	}

	/** A window rider that draws while its lane's window lives, saying nothing of its own. */
	private static Rider window(Lane lane) {
		return new Rider(status -> true, windowRemaining(lane), status -> "");
	}

	/** A window rider that draws while its lane's window holds its one-shot, saying nothing of its own. */
	private static Rider pendingWindow(Lane lane) {
		return new Rider(pending(lane), windowRemaining(lane), status -> "");
	}

	/** Rush: the engage's window draws while it lives, spent or not; the loaded bonus says nothing of its own. */
	public static final Row RUSH = new Row(Lane.RUSH, ClassRegistry.RUSH_ABILITY, RushAbility.NAME,
			List.of(window(Lane.RUSH)));

	/** Rend: countdown-only, so the rider vanishes on the settle instead of counting on. */
	public static final Row REND = new Row(Lane.REND, ClassRegistry.REND_ABILITY, RendAbility.NAME,
			List.of(pendingWindow(Lane.REND)));

	/**
	 * Rotate: the Impetus rider is one-shot (pending only), so it vanishes on the spend;
	 * the Testudo rider is the timed catch, live for its window in Scutum.
	 */
	public static final Row ROTATE = new Row(Lane.ROTATE, ClassRegistry.ROTATE_ABILITY, RotateAbility.NAME,
			List.of(pendingWindow(Lane.ROTATE),
					new Rider(status -> status.flipTarget() == RotateAbility.Stance.SCUTUM,
							windowRemaining(Lane.ROTATE), status -> "")));

	/** Aequilibrium: the window draws while it lives; it rides the shared oath lane. */
	public static final Row AEQUILIBRIUM = new Row(Lane.OATH, ClassRegistry.AEQUILIBRIUM_ABILITY,
			AequilibriumAbility.NAME, List.of(window(Lane.OATH)));

	/** The wheel's rider: Zero is the dead band and draws nothing, every other band draws its name. */
	public static final Row ROULETTE = new Row(Lane.ROULETTE, ClassRegistry.ROULETTE_ABILITY, RouletteAbility.NAME,
			List.of(new Rider(
					status -> status.rouletteOutcome() != null
							&& status.rouletteOutcome() != RouletteAbility.Outcome.ZERO,
					windowRemaining(Lane.ROULETTE),
					status -> status.rouletteOutcome() == null ? "" : status.rouletteOutcome().label())));

	/** All In: the ultimate's window, persistent like Aequilibrium's. */
	public static final Row ALL_IN = new Row(Lane.ALL_IN, ClassRegistry.ALL_IN_ABILITY, AllInAbility.NAME,
			List.of(window(Lane.ALL_IN)));

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
