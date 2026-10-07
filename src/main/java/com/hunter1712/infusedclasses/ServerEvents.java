package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server event wiring for the entry: first-join picker, respawn copy, logout drop, hit trigger,
 * and the per-tick decay-plus-apply. Decisions stay in pure modules and application in adapters;
 * this only routes between them.
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
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				InfusedClasses.runtimes().remove(handler.getPlayer().getUUID()));

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			long now = System.currentTimeMillis();
			// Momentum: a direct melee hit builds stacks for any class carrying an
			// on-hit-dealt passive and, inside a live Rush window with its bonus still
			// pending, lands the engage bonus too. Any melee, no charge gate;
			// self-damage excluded by the attacker check, projectiles and spells by
			// the direct check. Gating lives in the pure modules (unknown triggers,
			// missing passives and dead windows are inert), so this branch never
			// compares ability ids: a future stacker works without touching it.
			if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
					&& attacker != entity) {
				ClassDefinition atkDef = PlayerClassAccess.of(attacker).currentDefinition();
				if (atkDef != null) {
					PlayerRuntime atkRuntime = InfusedClasses.runtime(attacker);
					atkRuntime.onTrigger(atkDef, ConditionalPassive.ON_HIT_DEALT, now);
					PassiveApplier.grantRushBonus(atkRuntime, atkDef, now);
					// Rend empower: the stamped bonus lands on this hit and only this one,
					// applied through the adapter like every other outcome.
					int empowerStacks = atkRuntime.consumeRendEmpower(now);
					if (empowerStacks > 0) {
						RendApplier.dealEmpower(attacker, entity, RendAbility.empowerFor(empowerStacks));
					}
				}
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
				PassiveApplier.apply(player, def, runtime, now);
			}
		});
	}
}
