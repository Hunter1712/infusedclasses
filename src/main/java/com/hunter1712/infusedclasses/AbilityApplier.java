package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;

/**
 * Adapter for {@link SecondWindAbility}: the single place its outcome reaches the player.
 */
public final class AbilityApplier {
	private AbilityApplier() {
	}

	/**
	 * A Second Wind result: shields what the decision names, puffs the shielded player, then says what it says.
	 * Consume (stacks, armor) is the passive applier's job. Reports whether the shield landed: nothing owed
	 * means nothing lost, so the entry only spends stacks on a real shield.
	 */
	public static boolean apply(ServerPlayer player, SecondWindAbility.Result result) {
		boolean shielded = result.shieldHp() <= 0 || shield(player, result.shieldHp());
		if (result.kind() == SecondWindAbility.Kind.FIRED) {
			applyCastSlow(player);
		}
		announce(player, result.announcement());
		return shielded;
	}

	/** One announcement path for every ability result: null says nothing. Shared with the Bloodlust adapter. */
	static void announce(ServerPlayer player, ChatLine announcement) {
		if (announcement == null) {
			return;
		}
		MutableComponent line = Component.literal(announcement.text());
		if (announcement.style() != null) {
			line = line.withStyle(announcement.style());
		}
		player.displayClientMessage(line, true);
	}

	/** Casting commitment: brief self-slow, identical on every active. Shared, not duplicated. */
	static void applyCastSlow(ServerPlayer player) {
		Holder<MobEffect> holder = effectHolder(player, Spellcast.SLOWNESS);
		if (holder != null) {
			player.addEffect(new MobEffectInstance(holder, Spellcast.SLOW_TICKS, Spellcast.SLOW_AMPLIFIER));
		}
	}

	/** Hearts for the shield: forced past the particle limiter, so Minimal sees them too. Counts are manual-check only, not JUnit. */
	private static void heartPuff(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.HEART, true, true, x, y, z, 12, 0.3, 0.3, 0.3, 0.0);
	}

	/**
	 * The shield itself: the vanilla effect carries the 10s window and raises the absorption cap
	 * by 4 HP/level, then the exact amount trims its quantized grant to stacks x 1.0
	 * (amplifierFor guarantees the grant always reaches). Reports whether it landed: a missing
	 * effect must never cost stacks. Expiry needs no scheduler: it drops the effect's cap
	 * modifier and vanilla clamps the pool to the new max (0), so the shield dies with the icon.
	 */
	private static boolean shield(ServerPlayer player, float shieldHp) {
		Holder<MobEffect> holder = effectHolder(player, SecondWindAbility.ABSORPTION);
		if (holder == null) {
			return false;
		}
		player.addEffect(new MobEffectInstance(holder, SecondWindAbility.ABSORPTION_TICKS,
				SecondWindAbility.amplifierFor(shieldHp)));
		player.setAbsorptionAmount(shieldHp);
		if (player.level() instanceof ServerLevel level) {
			Vec3 at = player.position();
			heartPuff(level, at.x, at.y + 1.0, at.z);
		}
		return true;
	}

	/** One holder lookup for every effect this adapter lays: ids ride in decisions, holders resolve here. */
	private static Holder<MobEffect> effectHolder(ServerPlayer player, Identifier id) {
		return player.registryAccess().lookupOrThrow(Registries.MOB_EFFECT)
				.get(ResourceKey.create(Registries.MOB_EFFECT, id))
				.orElse(null);
	}
}
