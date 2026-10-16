package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;

/**
 * Adapter for {@link RushAbility}: the single place an engage reaches the player.
 * Cleanses harmful effects, announces on the action bar
 * and plays {@link RushAbility#CAST_SOUND} — on a fired outcome only. Rejects (wrong
 * class, cooldown) say their line and touch nothing. The stride stat rides the tick
 * while the window is live; the Momentum bonus lands later, on the next qualifying melee hit.
 */
public final class RushApplier {
	private RushApplier() {
	}

	public static void apply(ServerPlayer player, AbilityOutcome result) {
		boolean fired = result.fired();
		if (fired) {
			cleanse(player);
		}
		AbilityApplier.announceAndCast(player, result.announcement(), fired, RushAbility.CAST_SOUND, true);
	}

	/**
	 * Stride tick: the window's pace sits on the player while live, comes off after.
	 * Non-Juggernauts never arm the window, so they only ever meet the remove path.
	 */
	static void tick(ServerPlayer player, PlayerRuntime runtime, long now) {
		PassiveEffect.Modifier stride = RushAbility.strideModifier();
		if (runtime.isRushLive(now)) {
			AttributeWriter.write(player, stride);
		} else {
			AttributeWriter.remove(player, stride.attribute(), stride.modifierId());
		}
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
