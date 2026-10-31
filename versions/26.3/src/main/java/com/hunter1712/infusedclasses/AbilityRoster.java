package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * The ability roster: one row per ability the mod fires, and the one place its lane, its
 * slot, its gating id, its word, its fire and the stack's view of it live. Consumers walk
 * the rows — {@code /class info}, the HUD's rows decision, {@link BindMorph} and
 * {@link SlotPresses} — so an ability's facts cannot drift between them. Both lane reads
 * derive from the row's {@link Lane}, so a row cannot draw one lane and count down another.
 *
 * <p>A row owns both halves a slot bind needs: its {@link Slot} and its pure fire, so the
 * bind walk picks a row rather than an id a second table would map back. The lane read
 * stays split in two (ADR-0023); riders are spelled over their own row's lane's window.
 */
public final class AbilityRoster {
	/** The two reused binds every class shares. */
	public enum Slot {
		SIGNATURE,
		ULTIMATE
	}

	/** One ability's fire as the row carries it; the float is the press's one roll, read by the wheel only. */
	@FunctionalInterface
	public interface Fire {
		AbilityOutcome fire(ClassDefinition def, PlayerRuntime runtime, long now, float roll);
	}

	/**
	 * One ability's shared facts: the lane it runs on, the slot it belongs to, the id that
	 * gates it, its Title Case word, the rider rules that draw beside its lane line, whether
	 * a fired press plays the cast fanfare, and its fire.
	 */
	public record Row(Lane lane, Slot slot, Identifier ability, String name, List<Rider> riders,
			boolean fans, Fire fire) {
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

	/** Offload: the loosened window draws its countdown while it lives, spent or not; the price is already paid. */
	public static final Row OFFLOAD = new Row(Lane.OFFLOAD, Slot.SIGNATURE, ClassRegistry.OFFLOAD_ABILITY,
			OffloadAbility.NAME, List.of(window(Lane.OFFLOAD)), true,
			(def, runtime, now, roll) -> OffloadAbility.fire(def, runtime, now));

	/** Onslaught: the window draws its countdown while it lives; both ends of it are bar-side. */
	public static final Row ONSLAUGHT = new Row(Lane.ONSLAUGHT, Slot.ULTIMATE, ClassRegistry.ONSLAUGHT_ABILITY,
			OnslaughtAbility.NAME, List.of(window(Lane.ONSLAUGHT)), true,
			(def, runtime, now, roll) -> OnslaughtAbility.fire(def, runtime, now));

	/**
	 * Rotate: the Impetus rider is one-shot (pending only), so it vanishes on the spend;
	 * the Testudo rider is the timed catch, live for its window in Scutum. Its fire is the
	 * one that announces without fanfare: the toggle carries no cast sound.
	 */
	public static final Row ROTATE = new Row(Lane.ROTATE, Slot.SIGNATURE, ClassRegistry.ROTATE_ABILITY,
			RotateAbility.NAME,
			List.of(pendingWindow(Lane.ROTATE),
					new Rider(status -> status.flipTarget() == RotateAbility.Stance.SCUTUM,
							windowRemaining(Lane.ROTATE), status -> "")),
			false, (def, runtime, now, roll) -> RotateAbility.fire(def, runtime, now));

	/** Aequilibrium: the window draws while it lives; it rides the shared oath lane. */
	public static final Row AEQUILIBRIUM = new Row(Lane.OATH, Slot.ULTIMATE, ClassRegistry.AEQUILIBRIUM_ABILITY,
			AequilibriumAbility.NAME, List.of(window(Lane.OATH)), true,
			(def, runtime, now, roll) -> AequilibriumAbility.fire(def, runtime, now));

	/** The wheel's rider: Zero is the dead band and draws nothing, every other band draws its name. */
	public static final Row ROULETTE = new Row(Lane.ROULETTE, Slot.SIGNATURE, ClassRegistry.ROULETTE_ABILITY,
			RouletteAbility.NAME,
			List.of(new Rider(
					status -> status.rouletteOutcome() != null
							&& status.rouletteOutcome() != RouletteAbility.Outcome.ZERO,
					windowRemaining(Lane.ROULETTE),
					status -> status.rouletteOutcome() == null ? "" : status.rouletteOutcome().label())),
			true, RouletteAbility::fire);

	/** All In: the ultimate's window, persistent like Aequilibrium's. */
	public static final Row ALL_IN = new Row(Lane.ALL_IN, Slot.ULTIMATE, ClassRegistry.ALL_IN_ABILITY,
			AllInAbility.NAME, List.of(window(Lane.ALL_IN)), true,
			(def, runtime, now, roll) -> AllInAbility.fire(def, runtime, now));

	/** Rows in lane order: the order {@code /class info} prints and the classes carry their actives in. */
	private static final List<Row> ALL = List.of(OFFLOAD, ONSLAUGHT, ROTATE, AEQUILIBRIUM, ROULETTE, ALL_IN);

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
