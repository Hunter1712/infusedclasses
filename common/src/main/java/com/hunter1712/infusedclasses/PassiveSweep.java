package com.hunter1712.infusedclasses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a class switch must take off: the sweep set. Every modifier the old class's kit
 * could write — its tiered bars, its always-on rows, and the whole list of every
 * window-aura family it marks — minus every modifier id the new class writes. One entry
 * per id, so a kit naming an id twice sweeps once; keyed by id, so a class that re-earns
 * a modifier keeps it and the next tick rewrites its value, and a family both classes
 * mark is left to the family's own sweep. Pure: the wiring only removes. Assertable
 * without a player, which is the point — a switch that orphans a modifier is invisible
 * in game until the attribute stays wrong forever.
 */
public final class PassiveSweep {
	private PassiveSweep() {
	}

	/** The sweep set for one switch, empty when the class did not change. */
	public static List<PassiveEffect.Modifier> sweepFor(ClassDefinition oldDef, ClassDefinition newDef) {
		if (oldDef == newDef) {
			return List.of();
		}
		Set<String> stillWritten = new HashSet<>();
		for (PassiveEffect.Modifier kept : writtenBy(newDef)) {
			stillWritten.add(kept.modifierId());
		}
		Set<String> sweptIds = new HashSet<>();
		List<PassiveEffect.Modifier> swept = new ArrayList<>();
		for (PassiveEffect.Modifier written : writtenBy(oldDef)) {
			if (!stillWritten.contains(written.modifierId()) && sweptIds.add(written.modifierId())) {
				swept.add(written);
			}
		}
		return swept;
	}

	/**
	 * Every modifier a class's kit can put on the player: its tiered bars, its always-on
	 * rows, and the whole modifier list of each window family whose marker it holds — the
	 * same sets the tick can write, read through the same mapping. Empty for no class.
	 */
	public static List<PassiveEffect.Modifier> writtenBy(ClassDefinition def) {
		if (def == null) {
			return List.of();
		}
		List<PassiveEffect.Modifier> all = new ArrayList<>();
		for (ConditionalPassive passive : def.passives()) {
			all.addAll(passive.allRows());
		}
		for (ClassDefinition.AlwaysOn alwaysOn : def.alwaysOn()) {
			all.addAll(alwaysOn.modifiers());
		}
		for (WindowFamily family : WindowFamily.allFamilies()) {
			if (def.hasAbility(family.marker())) {
				all.addAll(family.all());
			}
		}
		return all;
	}
}
