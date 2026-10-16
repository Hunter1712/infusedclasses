package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongBiFunction;

/**
 * The {@code /class} info lines: a pure decision over the definition, the runtime and the
 * passed now. The lane table below is the only mapping from a class's actives to runtime
 * reads, so the copy cannot name a lane the class does not carry; a definition with an
 * unmapped active simply leaves it out. No class yields one plain line. The entry sends
 * the lines verbatim.
 */
public final class ClassInfo {
	/** No class: one line, gray like the pick line's tone. */
	private static final ChatLine NO_CLASS = new ChatLine("No class chosen", ChatFormatting.GRAY);

	/** One lane row: the ability id that gates it, its display name, and its runtime read. */
	private record Lane(Identifier ability, String name, ToLongBiFunction<PlayerRuntime, Long> remaining) {
	}

	private static final List<Lane> LANES = List.of(
			new Lane(ClassRegistry.RUSH_ABILITY, RushAbility.NAME, (runtime, now) -> runtime.rushRemaining(now)),
			new Lane(ClassRegistry.REND_ABILITY, RendAbility.NAME, (runtime, now) -> runtime.rendRemaining(now)),
			new Lane(ClassRegistry.ROTATE_ABILITY, RotateAbility.NAME, (runtime, now) -> runtime.rotateRemaining(now)),
			new Lane(ClassRegistry.AEQUILIBRIUM_ABILITY, AequilibriumAbility.NAME,
					(runtime, now) -> runtime.oathRemaining(now)),
			new Lane(ClassRegistry.ROULETTE_ABILITY, RouletteAbility.NAME,
					(runtime, now) -> runtime.rouletteRemaining(now)),
			new Lane(ClassRegistry.ALL_IN_ABILITY, AllInAbility.NAME,
					(runtime, now) -> runtime.allInRemaining(now)));

	private ClassInfo() {
	}

	/**
	 * The lines for the invoking player's entry to send: the class line
	 * ({@code Class: <displayName>}, gray), one line per lane the class carries
	 * ({@code Rush: 18.4s} while cooling, {@code Rend: ready} otherwise), and the
	 * Legionary's current stance. Null definition yields the single no-class line.
	 */
	public static List<ChatLine> lines(ClassDefinition def, PlayerRuntime runtime, long now) {
		if (def == null) {
			return List.of(NO_CLASS);
		}
		List<ChatLine> lines = new ArrayList<>();
		lines.add(new ChatLine("Class: " + def.displayName(), ChatFormatting.GRAY));
		for (Lane lane : LANES) {
			if (def.hasAbility(lane.ability())) {
				lines.add(laneLine(lane, runtime, now));
			}
		}
		if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			lines.add(new ChatLine("Stance: " + runtime.stance().displayName(), null));
		}
		return lines;
	}

	private static ChatLine laneLine(Lane lane, PlayerRuntime runtime, long now) {
		long remaining = lane.remaining().applyAsLong(runtime, now);
		return remaining > 0
				? ChatLine.cooldown(lane.name(), remaining)
				: new ChatLine(lane.name() + ": ready", null);
	}
}
