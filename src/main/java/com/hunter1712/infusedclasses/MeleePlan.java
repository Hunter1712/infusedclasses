package com.hunter1712.infusedclasses;

import java.util.function.Supplier;

/**
 * Melee plan decisions: what one melee hit earns and settles, decided purely from
 * definitions and transient state. The server entry maps, adapters apply; this owns
 * every ability-id check the hook needs, so the entry never compares ability ids to
 * gate rewards. The victim runtime arrives lazily: a non-Legionary victim costs no
 * transient state, as before.
 */
public final class MeleePlan {
	private MeleePlan() {
	}

	/** Whether this swing earns conditional-passive stacks: full swings only, spam earns zero. */
	public static boolean shouldBump(float charge) {
		return BonusCurve.isFullSwing(charge);
	}

	/** Outgoing melee multiplier for an attacker: the stance deal mult, 1.0 for anyone else. */
	public static double dealFor(ClassDefinition atkDef, PlayerRuntime runtime, long now) {
		if (atkDef == null || !atkDef.activeAbilities().contains(ClassRegistry.ROTATE_ABILITY)) {
			return 1.0;
		}
		return RotateAbility.dealMult(runtime.stance(), runtime.isTestudoLive(now));
	}

	/** Incoming melee multiplier for a victim: the stance take mult, 1.0 for anyone else. */
	public static double takeFor(ClassDefinition victimDef, Supplier<PlayerRuntime> runtime, long now) {
		if (victimDef == null || !victimDef.activeAbilities().contains(ClassRegistry.ROTATE_ABILITY)) {
			return 1.0;
		}
		PlayerRuntime r = runtime.get();
		return RotateAbility.takeMult(r.stance(), r.isImpetusLive(now));
	}

	/** Window bonus for an attacker inside a live Impetus window, through the charge curve. 0 otherwise. */
	public static float impetusBonusFor(ClassDefinition atkDef, PlayerRuntime runtime, long now, float charge) {
		if (atkDef == null || !atkDef.activeAbilities().contains(ClassRegistry.IMPETUS_ABILITY)
				|| !runtime.isImpetusLive(now)) {
			return 0;
		}
		return ImpetusAbility.bonusFor(charge);
	}
}
