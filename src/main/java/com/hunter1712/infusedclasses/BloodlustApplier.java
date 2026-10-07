package com.hunter1712.infusedclasses;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Adapter for {@link BloodlustAbility}: the single place an opener reaches the player.
 * Announces on the presser's action bar and roars — the press must land even on a whiff —
 * then roots only a fired opener. Fizzles (wrong class, cooldown, too healthy) say their
 * line and touch nothing. It also owns the lifesteal heal write; the damage hook decides
 * when a hit qualifies and routes here. The Bulwark seeding stays the passive applier's job.
 */
public final class BloodlustApplier {
	private BloodlustApplier() {
	}

	public static void apply(ServerPlayer player, BloodlustAbility.Result result) {
		AbilityApplier.announce(player, result.announcement());
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 at = player.position();
		level.playSound(null, at.x, at.y, at.z,
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.0F);
		if (result.kind() != BloodlustAbility.Kind.FIRED) {
			return;
		}
		AbilityApplier.applyCastSlow(player);
	}

	/** One lifesteal heal: the entry decides the hit qualifies, this writes the health. */
	public static void heal(ServerPlayer attacker, int bucket, float damageTaken) {
		attacker.heal(BloodlustAbility.lifestealFor(bucket, damageTaken));
	}
}
