package com.hunter1712.infusedclasses;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;

/** Adapter for {@link WarCryAbility}: the single place an outcome reaches the player. */
public final class AbilityApplier {
	private AbilityApplier() {
	}

	/** A result with no outcome and nothing to say is a no-op: wrong-class and on-cooldown presses. */
	public static void apply(ServerPlayer player, WarCryAbility.Result result) {
		WarCryAbility.Outcome outcome = result.outcome();
		if (outcome != null) {
			BuiltInRegistries.MOB_EFFECT.get(outcome.effect()).ifPresent(effect ->
					player.addEffect(new MobEffectInstance(effect, outcome.durationTicks(), outcome.amplifier())));
			if (player.level() instanceof ServerLevel level) {
				BuiltInRegistries.SOUND_EVENT.getOptional(outcome.sound()).ifPresent(sound ->
						level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F));
				BuiltInRegistries.PARTICLE_TYPE.getOptional(outcome.particle())
						.filter(SimpleParticleType.class::isInstance)
						.map(SimpleParticleType.class::cast)
						.ifPresent(particle -> particleRing(level, player, particle));
			}
		}
		WarCryAbility.ChatLine announcement = result.announcement();
		if (announcement != null) {
			MutableComponent line = Component.literal(announcement.text());
			if (announcement.style() != null) {
				line = line.withStyle(announcement.style());
			}
			player.displayClientMessage(line, true);
		}
	}

	// ponytail: flat ring of particles. Real effect cloud only if the visual ever matters.
	private static void particleRing(ServerLevel level, ServerPlayer player, SimpleParticleType particle) {
		Vec3 at = player.position();
		for (int i = 0; i < 24; i++) {
			double angle = (Math.PI * 2 * i) / 24;
			level.sendParticles(particle,
					at.x + Math.cos(angle) * 2.0, at.y + 0.2, at.z + Math.sin(angle) * 2.0,
					1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}
