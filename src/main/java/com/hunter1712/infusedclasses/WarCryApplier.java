package com.hunter1712.infusedclasses;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for {@link WarCryAbility}: the single place a shout reaches the world. Announces
 * on the shouter's action bar, then lays each carried debuff on every alive hostile in
 * radius. Players never match the predicate and the caster is the scan's except-arg, so
 * only hostile mobs eat it. Vanilla overwrite semantics: re-shouting refreshes.
 */
public final class WarCryApplier {
	private WarCryApplier() {
	}

	/** A fired shout: say it, roar, then debuff the pack. Rejects still say their line. */
	public static void apply(ServerPlayer player, WarCryAbility.Result result) {
		AbilityApplier.announce(player, result.announcement());
		if (result.kind() != WarCryAbility.Kind.FIRED) {
			return;
		}
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		// The press must land even on a whiff: sound always, particles per victim.
		Vec3 at = player.position();
		level.playSound(null, at.x, at.y, at.z,
				SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.0F);
		// Holders resolve once per shout, not per mob: the registry answers the same twice.
		List<Holder<MobEffect>> holders = new ArrayList<>();
		for (Identifier id : WarCryAbility.DEBUFF_EFFECTS) {
			Holder<MobEffect> holder = effect(player, id);
			if (holder != null) {
				holders.add(holder);
			}
		}
		AABB shout = player.getBoundingBox().inflate(WarCryAbility.RADIUS_BLOCKS);
		for (Entity entity : player.level().getEntities(player, shout,
				e -> e instanceof Monster && e.isAlive())) {
			if (entity instanceof Monster mob) {
				for (Holder<MobEffect> holder : holders) {
					mob.addEffect(new MobEffectInstance(
							holder, WarCryAbility.EFFECT_TICKS, WarCryAbility.AMPLIFIER));
				}
				Vec3 over = mob.position();
				level.sendParticles(ParticleTypes.ANGRY_VILLAGER, true, true,
						over.x, over.y + mob.getBbHeight() + 0.3, over.z, 12, 0.3, 0.2, 0.3, 0.0);
			}
		}
	}

	private static Holder<MobEffect> effect(ServerPlayer player, Identifier id) {
		return player.registryAccess().lookupOrThrow(Registries.MOB_EFFECT)
				.get(ResourceKey.create(Registries.MOB_EFFECT, id))
				.orElse(null);
	}
}
