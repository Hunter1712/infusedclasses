package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/**
 * Server event wiring for the entry: first-join picker, respawn copy, logout drop, hit trigger,
 * and the per-tick decay-plus-apply. Decisions stay in pure modules and application in adapters;
 * this only routes between them and sends the resulting signals.
 */
final class ServerEvents {
	private ServerEvents() {
	}

	static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (!PlayerClassAccess.of(player).hasClass()) {
				sender.sendPacket(new OpenClassScreenPayload());
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerClassAccess.of(newPlayer).copyFrom(PlayerClassAccess.of(oldPlayer));
			// Only the choice crosses. The old player's transient state is dropped, and the new
			// player's starts empty, which is what a respawn has always done to it.
			// Load-bearing ordering lives in the runtime store: both players share one UUID.
			InfusedClasses.runtimes().respawn(newPlayer.getUUID());
			// Fresh runtime reads as zero-stack entries, which clear the client's rows.
			ClassDefinition respawnedDef = PlayerClassAccess.of(newPlayer).currentDefinition();
			ServerSignals.sendStacks(newPlayer, respawnedDef, InfusedClasses.runtime(newPlayer),
					System.currentTimeMillis());
			// Death ends War Cry, so a classed row clears with the stacks. Classless costs no packet.
			if (respawnedDef != null) {
				ServerSignals.sendWarCry(newPlayer, 0);
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				InfusedClasses.runtimes().remove(handler.getPlayer().getUUID()));

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			long now = System.currentTimeMillis();
			if (entity instanceof ServerPlayer victim) {
				PlayerClasses classes = PlayerClassAccess.of(victim);
				// Classless players are inert, and cost nothing: no transient state is made for them.
				ClassDefinition def = classes.currentDefinition();
				if (def != null) {
					InfusedClasses.runtime(victim).onTrigger(def, ConditionalPassive.ON_HIT_TAKEN, now);
					// A hit always re-stamps grace, so the countdown is always worth re-sending.
					ServerSignals.sendStacks(victim, def, InfusedClasses.runtime(victim), now);
				}
			}
			if (source.getEntity() instanceof ServerPlayer attacker) {
				InfusedClasses.onAttackerHit(attacker, entity);
			}
		});

		// Server tick: decay conditional passive stacks, then put their effect on the player.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = System.currentTimeMillis();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				PlayerClasses classes = PlayerClassAccess.of(player);
				ClassDefinition def = classes.currentDefinition();
				if (def == null) {
					continue;
				}
				PlayerRuntime runtime = InfusedClasses.runtime(player);
				// A tick that moved no stacks changed nothing the client shows, so it sends nothing.
				if (!PassiveApplier.apply(player, def, runtime, now).isEmpty()) {
					ServerSignals.sendStacks(player, def, runtime, now);
				}
				// Edge-triggered clock clear: a Warrior whose Strength died (milk, death, timeout)
				// zeroes the row once, so banked ticks can never be resurrected by a later beacon.
				if (ClassRegistry.WARRIOR_ABILITY.equals(def.activeAbility())
						&& runtime.warCrySignal().pollLost(player.hasEffect(MobEffects.STRENGTH))) {
					ServerSignals.sendWarCry(player, 0);
				}
			}
		});
	}
}
