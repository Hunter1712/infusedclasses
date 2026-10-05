package com.hunter1712.infusedclasses;

import net.minecraft.world.entity.player.Player;

/**
 * The one thing the mod needs from a Player: the class it chose.
 * Implemented on every Player by the player mixin, so the cast below is always safe.
 *
 * Narrowed when {@link PlayerClasses} split: only the persisted choice lives on the player now, so
 * this hands out the choice and nothing else. It cannot be deleted while the player holds the
 * choice, because a cast to an interface is the only way for the server entry to reach a field
 * added by a mixin.
 */
public interface PlayerClassAccess {
	PlayerClasses classes();

	/** Mixin-backed accessor. Cast is safe: the mixin implements this on every Player. */
	static PlayerClasses of(Player player) {
		return ((PlayerClassAccess) player).classes();
	}
}
