package com.hunter1712.infusedclasses;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Adapter for {@link WarCryAbility} and {@link SecondWindAbility}: the single place an outcome
 * reaches the player.
 */
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
		announce(player, result.announcement());
	}

	/** A Second Wind result: heals what the decision names, puffs the healed player, then says what it says. Consume (stacks, armor, signal) is the passive applier's job. */
	public static void apply(ServerPlayer player, SecondWindAbility.Result result) {
		if (result.healHp() > 0) {
			player.heal(result.healHp());
			if (player.level() instanceof ServerLevel level) {
				Vec3 at = player.position();
				critPuff(level, at.x, at.y + 1.0, at.z);
			}
		}
		announce(player, result.announcement());
	}

	/**
	 * A decided extension: re-applies Strength at amplifier 1 for the decided ticks and puffs the
	 * victim so extensions read as hits, not re-fires. Says nothing. Zero or negative ticks is a
	 * no-op, and reports whether anything was applied so the entry knows the clock is worth sending.
	 */
	public static boolean applyExtension(ServerPlayer attacker, LivingEntity victim, int nextTicks) {
		if (nextTicks <= 0) {
			return false;
		}
		attacker.addEffect(new MobEffectInstance(MobEffects.STRENGTH, nextTicks, WarCryAbility.EFFECT_AMPLIFIER));
		if (attacker.level() instanceof ServerLevel level) {
			Vec3 at = victim.position();
			critPuff(level, at.x, at.y + 1.0, at.z);
		}
		return true;
	}

	/** One announcement path for every ability result: null says nothing. */
	private static void announce(ServerPlayer player, ChatLine announcement) {
		if (announcement == null) {
			return;
		}
		MutableComponent line = Component.literal(announcement.text());
		if (announcement.style() != null) {
			line = line.withStyle(announcement.style());
		}
		player.displayClientMessage(line, true);
	}

	/** One small crit puff. Counts are manual-check only, not JUnit. */
	private static void critPuff(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.CRIT, x, y, z, 8, 0.3, 0.3, 0.3, 0.2);
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
