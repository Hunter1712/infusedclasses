package com.hunter1712.infusedclasses;

import com.hunter1712.infusedclasses.mixin.LivingEntityAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

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
	 * The source arrives as a supplier so it is only resolved by this adapter, never
	 * by the entry, whichever side dealt the hit.
	 */
	public static void settleTake(Entity target, float baseDamageTaken, double mult,
			Supplier<DamageSource> source) {
		if (target == null || source == null) {
			return;
		}
		settleWithSource(target, baseDamageTaken, mult, source.get());
	}

	private static void settleWithSource(Entity target, float baseDamageTaken, double mult, DamageSource source) {
		if (target == null || !target.isAlive() || source == null) {
			return;
		}
		float extra = settleMath(baseDamageTaken, mult);
		if (extra == 0) {
			return;
		}
		if (extra > 0) {
			hurtGuarded(target, source, extra);
		} else if (target instanceof LivingEntity living) {
			living.heal(-extra);
		}
	}

	/**
	 * Pure settle math: the extra hurt over 1.0, the healed-back discount under it, nothing at
	 * exactly 1.0. Decided here so the curve is pinned without a player; adapters apply it.
	 */
	static float settleMath(float baseDamageTaken, double mult) {
		if (mult == 1.0) {
			return 0;
		}
		return (float) (baseDamageTaken * (mult - 1.0));
	}

	/**
	 * Single guarded hurt: extra damage re-enters the damage hook on the same thread.
	 * Vanilla remembers the biggest hurt of the window in {@code lastHurt} and drops any
	 * same-tick follow-up that does not beat it, so a bonus smaller than the base — a x1.25
	 * crit on a wood sword is a quarter of it — used to land nothing at all. Clearing the
	 * memory for the inner call routes the bonus through the difference branch instead:
	 * armor, absorption, death protection and kill credit still apply, with no second
	 * sound, knockback or hurt flash since the feedback belongs to the base hit. The
	 * memory is left where a single hit of base plus bonus would have left it.
	 */
	private static boolean hurtGuarded(Entity target, DamageSource source, float amount) {
		if (resolving) {
			return false;
		}
		resolving = true;
		try {
			if (target instanceof LivingEntity living && amount > 0) {
				LivingEntityAccessor access = (LivingEntityAccessor) living;
				float lastHurt = access.infused$lastHurt();
				access.infused$lastHurt(0.0F);
				try {
					target.hurt(source, amount);
				} finally {
					access.infused$lastHurt(lastHurt + amount);
				}
			} else {
				target.hurt(source, amount);
			}
		} finally {
			resolving = false;
		}
		return true;
	}
}
