package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

/**
 * The reused slot binds' landing: one press path for both slots, with the switch arms
 * kept explicit in the wiring layer per ADR-0009 — this routes, the abilities decide.
 * The entry keeps payload registration and the receivers; what a press lands lives here.
 */
final class SlotPresses {
	private SlotPresses() {
	}

	/** One slot press: the class read, the classless bail and the runtime lookup are the one path both binds share. */
	static void press(ServerPlayer player, SlotPress slot) {
		long now = InfusedClasses.now();
		ClassDefinition def = PlayerClassAccess.of(player).currentDefinition();
		// A classless press can only reject, silently, so it costs no state
		// (ADR-0005): the runtime lookup below runs for a classed player only.
		if (def == null) {
			return;
		}
		slot.land(player, def, InfusedClasses.runtime(player), now);
	}

	/** What one slot's press lands once the class is known: the target's ability plus its adapter. */
	interface SlotPress {
		void land(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now);
	}

	/**
	 * The signature slot: the reused bind's target fires and its landing runs, one line per
	 * arm. The arms stay explicit here per ADR-0009: this routes, the abilities decide.
	 */
	static void pressSignature(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		// Reused signature bind: the wheel for Gamblers, the rotate intent for Legionaries,
		// Offload for everyone else, so binds survive the class.
		switch (BindMorph.signatureFor(def)) {
			// Rotate announces without fanfare on purpose: it carries no cast sound.
			case ROTATE -> {
				RotateAbility.Result result = RotateAbility.fire(def, runtime, now);
				AbilityApplier.announce(player, result.outcome().announcement(), true);
			}
			case ROULETTE -> {
				RouletteAbility.Result result =
						RouletteAbility.fire(def, runtime, now, player.getRandom().nextFloat());
				AbilityApplier.announceAndCast(player, result.outcome().announcement(),
						result.outcome().fired(), true);
			}
			case OFFLOAD -> {
				AbilityOutcome result = OffloadAbility.fire(def, runtime, now);
				AbilityApplier.announceAndCast(player, result.announcement(), result.fired(), true);
			}
		}
	}

	/**
	 * The ultimate slot: the reused bind's target fires and its landing runs, one line per
	 * arm, the arms explicit here for the same reason.
	 */
	static void pressUltimate(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, long now) {
		// Reused ultimate bind: All In for Gamblers, the oath ultimate for Legionaries,
		// Onslaught for everyone else.
		switch (BindMorph.ultimateFor(def)) {
			case AEQUILIBRIUM -> {
				AbilityOutcome result = AequilibriumAbility.fire(def, runtime, now);
				AbilityApplier.announceAndCast(player, result.announcement(), result.fired(), true);
			}
			case ALL_IN -> {
				AbilityOutcome result = AllInAbility.fire(def, runtime, now);
				AbilityApplier.announceAndCast(player, result.announcement(), result.fired(), true);
			}
			case ONSLAUGHT -> {
				AbilityOutcome result = OnslaughtAbility.fire(def, runtime, now);
				AbilityApplier.announceAndCast(player, result.announcement(), result.fired(), true);
			}
		}
	}
}
