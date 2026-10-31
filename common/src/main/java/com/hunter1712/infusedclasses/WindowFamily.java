package com.hunter1712.infusedclasses;


import java.util.List;
import java.util.function.Function;

/**
 * One window-aura family: the marker ability that owns it, every modifier it can write, and
 * the pure decision of what its live windows want. The row lives with the ability whose
 * window it spells, so adding a window ability adds its family there instead of editing the
 * applier. The applier walks the rows once, so the write and remove sides cannot drift, and
 * a class without the marker sweeps whatever the family could have written.
 */
public record WindowFamily(
		String marker,
		List<PassiveEffect.Modifier> all,
		Function<PlayerRuntime.State, List<PassiveEffect.Modifier>> wanted) {

	/**
	 * A family whose window is the lane's own: wanted exactly while that lane's window
	 * lives, the full set swept otherwise. The families that read a plain lane clock
	 * (Aequilibrium, Offload) share this one wanted shape.
	 */
	public static WindowFamily whileLive(String marker, Lane lane, List<PassiveEffect.Modifier> all) {
		return new WindowFamily(marker, all, state -> state.lane(lane).windowRemainingMs() > 0 ? all : List.of());
	}

	/**
	 * Every window family in the game, each row owned by the ability whose window it spells.
	 * Adding a window ability adds its family there, on the ability itself, so the tick,
	 * the sweep and the roster all read one list and no adapter names an ability id or a lane.
	 */
	public static List<WindowFamily> allFamilies() {
		return List.of(
				RotateAbility.windowFamily(),
				AequilibriumAbility.windowFamily(),
				RouletteAbility.windowFamily(),
				OffloadAbility.windowFamily());
	}
}
