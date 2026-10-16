package com.hunter1712.infusedclasses;

import net.minecraft.world.entity.player.Player;

/**
 * The charge of the swing the hit hook is settling, captured at swing time because
 * vanilla zeroes the attack-strength ticker before the hit lands: read from the
 * damage hook, {@code getAttackStrengthScale} is always 0 and every full-swing gate
 * was dead. Implemented on every Player by the player mixin, so the cast below is
 * always safe.
 */
public interface SwingChargeAccess {
	float swingCharge();

	/** Mixin-backed accessor. Cast is safe: the mixin implements this on every Player. */
	static float of(Player player) {
		return ((SwingChargeAccess) player).swingCharge();
	}
}
