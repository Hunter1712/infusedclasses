package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;

/**
 * Adapter for {@link RushAbility}: the single place an engage reaches the player.
 * Cleanses harmful effects, lays Speed for the window, announces on the action bar
 * and plays {@link RushAbility#CAST_SOUND} — on FIRED only. Fizzles (wrong class, cooldown) say their line and
 * touch nothing. The Momentum bonus lands later, on the next qualifying melee hit.
 */
public final class RushApplier {
	private RushApplier() {
	}

	public static void apply(ServerPlayer player, RushAbility.Result result) {
		AbilityApplier.announce(player, result.announcement());
		if (result.kind() != RushAbility.Kind.FIRED) {
			return;
		}
		cleanse(player);
		Holder<MobEffect> holder = player.registryAccess().lookupOrThrow(Registries.MOB_EFFECT)
				.get(ResourceKey.create(Registries.MOB_EFFECT, RushAbility.SPEED_EFFECT))
				.orElse(null);
		if (holder != null) {
			player.addEffect(new MobEffectInstance(holder, RushAbility.SPEED_TICKS, RushAbility.SPEED_AMPLIFIER));
		}
		AbilityApplier.playCast(player, RushAbility.CAST_SOUND);
	}

	/**
	 * Drops every active harmful effect, keeps everything else. Resolved per holder so a
	 * new harmful effect cleanses without a code change; iteration copies first because
	 * removal mutates the live collection.
	 */
	static void cleanse(ServerPlayer player) {
		for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
			Holder<MobEffect> effect = instance.getEffect();
			if (effect.value().getCategory() == MobEffectCategory.HARMFUL) {
				player.removeEffect(effect);
			}
		}
	}
}
