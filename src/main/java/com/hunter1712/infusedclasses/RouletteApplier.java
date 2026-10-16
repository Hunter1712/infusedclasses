package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

/**
 * Adapter for {@link RouletteAbility}: the single place a spin reaches the player.
 * Fired spins heal what their outcome heals at once (Vigor, Jackpot), announce their
 * outcome and play the spin sound; rejects say their line and touch nothing. The stat
 * outcomes ride the passive applier's window path and the deal mults ride the melee plan.
 */
public final class RouletteApplier {
	private RouletteApplier() {
	}

	public static void apply(ServerPlayer player, RouletteAbility.Result result) {
		boolean fired = result.kind() == RouletteAbility.Kind.FIRED;
		if (fired && result.outcome().healHp() > 0) {
			player.heal(result.outcome().healHp());
		}
		AbilityApplier.announceAndCast(player, result.announcement(), fired, RouletteAbility.CAST_SOUND, true);
	}
}
