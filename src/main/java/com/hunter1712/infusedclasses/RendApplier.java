package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;

/**
 * Adapter for {@link RendAbility}: the single place a Rend press lands.
 * Announces on the presser's action bar and plays
 * {@link RendAbility#CAST_SOUND} — on FIRED only.
 * Fizzles (wrong class, cooldown, no momentum) say their line and touch nothing.
 * The empower damage itself lands in {@link DamageSettle#dealBonus}, called by the damage hook
 * on the next qualifying melee hit. Consuming the spent stacks stays the passive
 * applier's job.
 */
public final class RendApplier {
	private RendApplier() {
	}

	public static void apply(ServerPlayer presser, RendAbility.Result result) {
		AbilityApplier.announceAndCast(presser, result.announcement(),
				result.kind() == RendAbility.Kind.FIRED, RendAbility.CAST_SOUND);
	}
}
