package com.hunter1712.infusedclasses;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Adapter for {@link SecondWindAbility}: the single place its outcome reaches the player.
 */
public final class AbilityApplier {
	private AbilityApplier() {
	}

	/** A Second Wind result: heals what the decision names, puffs the healed player, then says what it says. Consume (stacks, armor) is the passive applier's job. */
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
}
