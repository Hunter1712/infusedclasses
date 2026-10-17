package com.hunter1712.infusedclasses;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.ToLongFunction;

/**
 * The ability roster: one row per ability the mod fires, and the one place its word, its
 * lane read and its slot binds live. Consumers walk the rows — {@code /class info} draws
 * its lane lines, the HUD's rows decision draws its words, {@link BindMorph} resolves what
 * the reused slots mean — so an ability's facts cannot drift between them. Words are the
 * ability modules' own NAME constants; a lane read is a pure read of the runtime state.
 *
 * <p>The ultimate bind keys on the {@link #ROTATE} row (ADR-0009): the Legionary's
 * Aequilibrium row carries the word and the shared oath lane, not a second bind.
 */
public final class AbilityRoster {
	/**
	 * One ability's shared facts: the id that gates it, its Title Case word, its lane
	 * read over the runtime state, and the slot binds it answers (null when the ability
	 * answers none).
	 */
	public record Row(
			Identifier ability,
			String name,
			ToLongFunction<PlayerRuntime.State> laneRemaining,
			BindMorph.SignatureTarget signature,
			BindMorph.UltimateTarget ultimate) {
	}

	public static final Row RUSH = new Row(ClassRegistry.RUSH_ABILITY, RushAbility.NAME,
			PlayerRuntime.State::rushRemaining, BindMorph.SignatureTarget.RUSH, null);
	public static final Row REND = new Row(ClassRegistry.REND_ABILITY, RendAbility.NAME,
			PlayerRuntime.State::rendRemaining, null, BindMorph.UltimateTarget.REND);
	public static final Row ROTATE = new Row(ClassRegistry.ROTATE_ABILITY, RotateAbility.NAME,
			PlayerRuntime.State::rotateRemaining, BindMorph.SignatureTarget.ROTATE,
			BindMorph.UltimateTarget.AEQUILIBRIUM);
	public static final Row AEQUILIBRIUM = new Row(ClassRegistry.AEQUILIBRIUM_ABILITY, AequilibriumAbility.NAME,
			PlayerRuntime.State::oathRemaining, null, null);
	public static final Row ROULETTE = new Row(ClassRegistry.ROULETTE_ABILITY, RouletteAbility.NAME,
			PlayerRuntime.State::rouletteRemaining, BindMorph.SignatureTarget.ROULETTE, null);
	public static final Row ALL_IN = new Row(ClassRegistry.ALL_IN_ABILITY, AllInAbility.NAME,
			PlayerRuntime.State::allInRemaining, null, BindMorph.UltimateTarget.ALL_IN);

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
