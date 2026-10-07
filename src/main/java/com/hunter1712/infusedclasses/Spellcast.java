package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Casting fanfare shared by every active: firing any ability plays its own sound, so
 * landing a cast is one learnable rule. Fizzles and rejects cost nothing and stay
 * silent. Decision data (which sound) rides with each applier; this owns the single
 * play path so it is never duplicated.
 */
public final class Spellcast {
	private Spellcast() {
	}

	/** One play path for every cast sound: null level or sound plays nothing. */
	public static void playCastSound(ServerLevel level, Vec3 at, Holder<SoundEvent> sound) {
		if (level == null || at == null || sound == null) {
			return;
		}
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
	}
}
