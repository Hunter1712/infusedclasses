package com.hunter1712.infusedclasses;

import net.minecraft.world.entity.player.Player;

/**
 * The charge of the swing the hit hook is settling, captured at the one place each
 * melee damage route passes. Normal swings zero the attack-strength ticker at the top
 * of {@code onAttack}, before the hit lands, so the capture sits just above that
 * reset; spear thrusts and mace smashes damage inside {@code stabAttack} and reach
 * that reset after the damage (spears) or never (mace), so a second capture sits at
 * the stab's head. The hook itself runs after all of these, so a read from there is
 * stale and the capture is the full-swing gates' only source. Implemented on every
 * Player by the player mixin, so the cast below is always safe.
 */
public interface SwingChargeAccess {
	float swingCharge();

	/** Mixin-backed accessor. Cast is safe: the mixin implements this on every Player. */
	static float of(Player player) {
		return ((SwingChargeAccess) player).swingCharge();
	}
}
