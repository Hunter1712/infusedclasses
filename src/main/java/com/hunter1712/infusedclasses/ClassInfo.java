package com.hunter1712.infusedclasses;

import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code /class} info lines: a pure decision over the definition and one runtime state
 * read. Lanes come from the {@link AbilityRoster}, the only mapping from a class's actives
 * to a word and a read, so the copy cannot name a lane the class does not carry; a class
 * with an unknown active simply leaves it out. No class yields one plain line. The entry
 * sends the lines verbatim.
 */
public final class ClassInfo {
	/** No class: one line, gray like the pick line's tone. */
	private static final ChatLine NO_CLASS = new ChatLine("No class chosen", ChatFormatting.GRAY);

	private ClassInfo() {
	}

	/**
	 * The lines for the invoking player's entry to send: the class line
	 * ({@code Class: <displayName>}, gray), one line per lane the class carries, in
	 * roster order ({@code Rush: 18.4s} while cooling, {@code Rend: ready} otherwise),
	 * and the Legionary's current stance. A null definition yields the single no-class
	 * line and reads no state.
	 */
	public static List<ChatLine> lines(ClassDefinition def, PlayerRuntime.State state) {
		if (def == null) {
			return List.of(NO_CLASS);
		}
		List<ChatLine> lines = new ArrayList<>();
		lines.add(new ChatLine("Class: " + def.displayName(), ChatFormatting.GRAY));
		for (AbilityRoster.Row row : AbilityRoster.all()) {
			if (def.hasAbility(row.ability())) {
				lines.add(laneLine(row, state));
			}
		}
		if (def.hasAbility(ClassRegistry.ROTATE_ABILITY)) {
			lines.add(new ChatLine("Stance: " + state.stance().displayName(), null));
		}
		return lines;
	}

	private static ChatLine laneLine(AbilityRoster.Row row, PlayerRuntime.State state) {
		long remaining = row.laneRemaining(state);
		return remaining > 0
				? ChatLine.cooldown(row.name(), remaining)
				: new ChatLine(row.name() + ": ready", null);
	}
}
