package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

/**
 * The reused slot binds' landing: one walk from the slot to its roster row, the row's own
 * fire, and the shared announcement. A further ability adds a row to {@link AbilityRoster},
 * not an arm here; the two binds share this path so they cannot land differently.
 */
final class SlotPresses {
	private SlotPresses() {
	}

	/** The signature bind: the row the class's first signature active names, Offload for everyone else. */
	static void pressSignature(ServerPlayer player) {
		press(player, AbilityRoster.Slot.SIGNATURE);
	}

	/** The ultimate bind: the row the class's first ultimate active names, Onslaught for everyone else. */
	static void pressUltimate(ServerPlayer player) {
		press(player, AbilityRoster.Slot.ULTIMATE);
	}

	private static void press(ServerPlayer player, AbilityRoster.Slot slot) {
		ClassDefinition def = PlayerClassAccess.of(player).currentDefinition();
		// A classless press can only reject, silently, so it costs no state (ADR-0005):
		// the runtime lookup below runs for a classed player only.
		if (def == null) {
			return;
		}
		AbilityRoster.Row row = BindMorph.slotFor(def, slot);
		// One roll per press: only the wheel reads it, and every press draws it so the
		// row's fire keeps one shape.
		AbilityOutcome result = row.fire().fire(def, InfusedClasses.runtime(player),
				InfusedClasses.now(), player.getRandom().nextFloat());
		// The row decides the fanfare: Rotate announces without one on purpose, every
		// other ability plays the cast sound when it fired.
		AbilityApplier.announceAndCast(player, result.announcement(), result.fired() && row.fans(), true);
	}
}
