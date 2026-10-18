package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

/**
 * Adapter for {@link RendAbility}: the single place a Rend press reaches the player.
 * Announces and plays {@link RendAbility#CAST_SOUND} on a fired outcome, then spends the
 * priced passive — a fizzle ("No Momentum") announces and spends nothing, and rejects say
 * nothing at all. The bonus itself lands later, on the next melee hit inside the window.
 */
public final class RendApplier {
	private RendApplier() {
	}

	public static void apply(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime, RendAbility.Result result) {
		AbilityApplier.announceAndCast(player, result.outcome().announcement(),
				result.outcome().fired(), RendAbility.CAST_SOUND, true);
		// The adapter spends only the priced passive; the spent stacks are what the
		// window's stamped bonus is worth on the hit that lands.
		if (result.consume()) {
			PassiveApplier.consumeSpent(player, runtime, def);
		}
	}
}
