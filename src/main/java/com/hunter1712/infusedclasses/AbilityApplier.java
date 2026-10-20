package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Shared adapter seams: the single place ability outcomes are announced and cast
 * sounds are played. The one cast fanfare id sits here beside the path that plays
 * it; effect ids ride in decisions and resolve at their single use sites. Per-ability
 * adapters own what their own result lands; this owns what they share.
 */
public final class AbilityApplier {
	private AbilityApplier() {
	}

	/** One text conversion for every chat line, so a command source and a player read the same style. */
	public static MutableComponent component(ChatLine line) {
		MutableComponent text = Component.literal(line.text());
		if (line.style() != null) {
			text = text.withStyle(line.style());
		}
		return text;
	}

	/** One announcement path for every ability result and choice line: null says nothing. If actionBar is true, sends to action bar; otherwise to chat. */
	public static void announce(ServerPlayer player, ChatLine announcement, boolean actionBar) {
		if (announcement == null) {
			return;
		}
		player.displayClientMessage(component(announcement), actionBar);
	}

	/**
	 * The one fanfare: every fired ability plays this, the mod's only cast sound.
	 * The evoker's own cast spell, so landing a cast reads as one.
	 */
	public static final Identifier CAST_SOUND = Identifier.withDefaultNamespace("entity.evoker.cast_spell");

	/** The fanfare plays above the shared volume: the evoker's low whoosh needs it to read over combat. */
	private static final float CAST_VOLUME = 1.6F;

	/** One play path for every mod sound: resolves the id and plays it at the player. Null sound plays nothing. */
	public static void playCast(ServerPlayer player, Identifier soundId) {
		playCast(player, soundId, 1.0F);
	}

	private static void playCast(ServerPlayer player, Identifier soundId, float volume) {
		if (player.level() instanceof ServerLevel level && soundId != null) {
			Vec3 at = player.position();
			Holder<SoundEvent> sound = player.registryAccess().lookupOrThrow(Registries.SOUND_EVENT)
					.get(ResourceKey.create(Registries.SOUND_EVENT, soundId))
					.orElse(null);
			if (sound == null) {
				return;
			}
			level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, 1.0F);
		}
	}

	/**
	 * One announce-plus-fanfare path for every fired-or-fizzle outcome: says the line,
	 * plays {@link #CAST_SOUND} on FIRED only. Null says nothing.
	 * If actionBar is true, sends to action bar; otherwise to chat.
	 */
	public static void announceAndCast(ServerPlayer player, ChatLine announcement, boolean fired,
			boolean actionBar) {
		announce(player, announcement, actionBar);
		if (fired) {
			playCast(player, CAST_SOUND, CAST_VOLUME);
		}
	}
}
