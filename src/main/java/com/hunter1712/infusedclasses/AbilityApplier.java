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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.phys.Vec3;

/**
 * Shared adapter seams: the single place ability outcomes are announced and registry ids
 * are resolved. Ids ride in decisions, holders resolve here — never the reverse.
 * Per-ability adapters own what their own result lands; this owns what they share.
 */
public final class AbilityApplier {
	private AbilityApplier() {
	}

	/** One announcement path for every ability result: null says nothing. */
	public static void announce(ServerPlayer player, ChatLine announcement) {
		if (announcement == null) {
			return;
		}
		MutableComponent line = Component.literal(announcement.text());
		if (announcement.style() != null) {
			line = line.withStyle(announcement.style());
		}
		player.displayClientMessage(line, true);
	}

	/** One holder lookup for every effect an adapter lays: ids ride in decisions, holders resolve here. Missing stays silent. */
	public static Holder<MobEffect> effectHolder(ServerPlayer player, Identifier id) {
		if (id == null) {
			return null;
		}
		return player.registryAccess().lookupOrThrow(Registries.MOB_EFFECT)
				.get(ResourceKey.create(Registries.MOB_EFFECT, id))
				.orElse(null);
	}

	/** One holder lookup for every cast sound: ids ride in decisions, holders resolve here. Missing stays silent. */
	public static Holder<SoundEvent> soundHolder(ServerPlayer player, Identifier id) {
		if (id == null) {
			return null;
		}
		return player.registryAccess().lookupOrThrow(Registries.SOUND_EVENT)
				.get(ResourceKey.create(Registries.SOUND_EVENT, id))
				.orElse(null);
	}

	/** One cast fanfare for every fired ability: resolves the sound id and plays it. Fizzles never reach here. Null sound plays nothing. */
	public static void playCast(ServerPlayer player, Identifier soundId) {
		if (player.level() instanceof ServerLevel level) {
			Vec3 at = player.position();
			Holder<SoundEvent> sound = soundHolder(player, soundId);
			if (sound == null) {
				return;
			}
			level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
	}

	/**
	 * One announce-plus-fanfare path for every fired-or-fizzle outcome: says the line,
	 * plays the sound on FIRED only. Null says nothing, missing sound stays silent.
	 */
	public static void announceAndCast(ServerPlayer player, ChatLine announcement, boolean fired,
			Identifier soundId) {
		announce(player, announcement);
		if (fired) {
			playCast(player, soundId);
		}
	}
}
