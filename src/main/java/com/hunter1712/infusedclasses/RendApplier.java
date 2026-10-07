package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Adapter for {@link RendAbility}: the single place an empower reaches the player and,
 * later, its target. Announces on the presser's action bar and roars — on FIRED only.
 * Fizzles (wrong class, cooldown, no momentum) say their line and touch nothing.
 * The empower damage itself lands in {@link #dealEmpower}, called by the damage hook
 * on the next qualifying melee hit. Consuming the spent stacks stays the passive
 * applier's job.
 */
public final class RendApplier {
	// Guard for empower damage: the extra hurt below re-enters the damage hook on the
	// same thread, and must not earn stacks or bonuses a second time. Server logic is
	// single-threaded, so a plain flag is enough.
	private static boolean resolvingEmpower;

	private RendApplier() {
	}

	public static void apply(ServerPlayer presser, RendAbility.Result result) {
		AbilityApplier.announce(presser, result.announcement());
		if (result.kind() != RendAbility.Kind.FIRED) {
			return;
		}
		AbilityApplier.playCast(presser, RendAbility.CAST_SOUND);
	}

	/**
	 * Lands stamped empower damage on the just-hit entity. Reports whether anything was
	 * dealt: a re-entrant call (from the hurt below) or a dead target deals nothing.
	 */
	public static boolean dealEmpower(ServerPlayer attacker, Entity target, float empowerHp) {
		if (resolvingEmpower || target == null || !target.isAlive() || empowerHp <= 0) {
			return false;
		}
		resolvingEmpower = true;
		try {
			target.hurt(attacker.damageSources().playerAttack(attacker), empowerHp);
		} finally {
			resolvingEmpower = false;
		}
		return true;
	}
}
