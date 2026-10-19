package com.hunter1712.infusedclasses;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Adapter for the crit verdict's feedback. A crit replays the vanilla cue — the crit
 * particle burst, spawned here because 1.21.11's own {@code Player.crit} is empty, plus
 * the {@code PLAYER_ATTACK_CRIT} sound — and a betrayal gets a distinct crack instead.
 * NORMAL and NONE cue nothing: vanilla's own weak/strong swing sounds keep playing for
 * every hit.
 */
public final class CritCue {
	/** The vanilla crit sound, replayed by a mod crit. */
	public static final Identifier CRIT_SOUND = Identifier.withDefaultNamespace("entity.player.attack.crit");
	/** Betrayal cue: the dice breaking instead of spiking. */
	public static final Identifier BETRAYAL_SOUND = Identifier.withDefaultNamespace("entity.item.break");

	private CritCue() {
	}

	/** Cues one landed hit's verdict. Called from the settle path, once per hit, classless included. */
	public static void apply(ServerPlayer attacker, Entity target, CritRoll.Verdict verdict) {
		switch (verdict) {
			case CRIT -> {
				critParticles(target);
				AbilityApplier.playCast(attacker, CRIT_SOUND);
			}
			case BETRAYAL -> AbilityApplier.playCast(attacker, BETRAYAL_SOUND);
			case NORMAL, NONE -> {
			}
		}
	}

	/** The vanilla crit particle burst, at the victim: the mod spawns it since the vanilla crit method is empty. */
	private static void critParticles(Entity target) {
		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(),
					10, 0.5, 0.5, 0.5, 0.0);
		}
	}
}
