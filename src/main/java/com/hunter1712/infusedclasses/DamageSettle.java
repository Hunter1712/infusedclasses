package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Damage settle adapter: the single guarded {@code hurt} for the whole mod plus the
 * stance-mult settle every melee branch funnels through. Bonus hurts re-enter the
 * damage hook on the same thread, so one re-entrancy flag guards them all: the entry
 * returns early while it is set, and nested hurts set it around the inner call.
 * Server logic is single-threaded, so a plain flag is enough.
 */
public final class DamageSettle {
	private static boolean resolving;

	private DamageSettle() {
	}

	/** Whether a settled hurt is on the stack: the hook grants nothing twice while true. */
	public static boolean isResolving() {
		return resolving;
	}

	/**
	 * Lands stamped bonus damage on the just-hit entity. Reports whether anything was
	 * dealt: a re-entrant call (from the hurt below) or a dead target deals nothing.
	 */
	public static boolean dealBonus(ServerPlayer attacker, Entity target, float bonusHp) {
		if (target == null || !target.isAlive() || bonusHp <= 0) {
			return false;
		}
		return hurtGuarded(target, attacker.damageSources().playerAttack(attacker), bonusHp);
	}

	/**
	 * Settles a stance mult on a just-hit entity: over 1.0 lands extra hurt through
	 * the guarded path (dead targets deal nothing); under 1.0 heals back
	 * the discount, so the net hit reads as base times mult. Exactly 1.0 touches
	 * nothing. The single home for stance settle: the entry maps, this applies.
	 */
	public static void settleMult(ServerPlayer attacker, Entity target, float baseDamageTaken, double mult) {
		if (attacker == null) {
			return;
		}
		settleWithSource(target, baseDamageTaken, mult, attacker.damageSources().playerAttack(attacker));
	}

	/**
	 * Mob-melee take on a Legionary: same settle, sourced from the mob so the entry
	 * never resolves damage sources itself. Discounts (mult &lt; 1.0) heal back like
	 * the player path, so both resolutions stay identical.
	 */
	public static void settleMobTake(ServerPlayer victim, LivingEntity mobAttacker, float baseDamageTaken,
			double take) {
		if (victim == null || mobAttacker == null || !victim.isAlive() || take == 1.0) {
			return;
		}
		DamageSource source = mobAttacker.damageSources().mobAttack(mobAttacker);
		settleWithSource(victim, baseDamageTaken, take, source);
	}

	private static void settleWithSource(Entity target, float baseDamageTaken, double mult, DamageSource source) {
		if (target == null || !target.isAlive() || mult == 1.0 || source == null) {
			return;
		}
		float extra = (float) (baseDamageTaken * (mult - 1.0));
		if (extra > 0) {
			hurtGuarded(target, source, extra);
		} else if (target instanceof LivingEntity living) {
			living.heal(-extra);
		}
	}

	/** Single guarded hurt: extra damage re-enters the damage hook on the same thread. */
	private static boolean hurtGuarded(Entity target, DamageSource source, float amount) {
		if (resolving) {
			return false;
		}
		resolving = true;
		try {
			target.hurt(source, amount);
		} finally {
			resolving = false;
		}
		return true;
	}
}
