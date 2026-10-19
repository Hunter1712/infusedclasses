package com.hunter1712.infusedclasses;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server event wiring for the entry: intent receivers, first-join picker, respawn copy,
 * logout drop, hit trigger, and the per-tick decay-plus-apply. Decisions stay in pure
 * modules and application in adapters; this only routes between them. The hit body lives
 * in {@link MeleeHook} and the slot presses in {@link SlotPresses}, so this file keeps
 * the wiring, the lifecycle fan-outs and the one steady-state emission point.
 */
final class ServerEvents {
	private ServerEvents() {
	}

	static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (!PlayerClassAccess.of(player).hasClass()) {
				sender.sendPacket(new OpenClassScreenPayload(""));
			}
			// One forced snapshot on join, classless included: the client starts from a
			// known state instead of guessing until the first change point.
			StatusSync.sendNow(player, InfusedClasses.now());
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerClassAccess.of(newPlayer).copyFrom(PlayerClassAccess.of(oldPlayer));
			// Only the choice crosses. The old player's transient state is dropped, and the new
			// player's starts empty, which is what a respawn has always done to it.
			// Load-bearing ordering lives in the runtime store: both players share one UUID.
			InfusedClasses.runtimes().respawn(newPlayer.getUUID());
			StatusSync.forget(newPlayer.getUUID());
			StatusSync.sendNow(newPlayer, InfusedClasses.now());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			InfusedClasses.runtimes().remove(handler.getPlayer().getUUID());
			StatusSync.forget(handler.getPlayer().getUUID());
		});

		// The melee hook: one AFTER_DAMAGE body, routed and settled in MeleeHook.
		ServerLivingEntityEvents.AFTER_DAMAGE.register(MeleeHook::afterDamage);

		// Server tick: the passive pass, the drained-window end and the snapshot are one
		// named sequence; this only loops the players and sends.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = InfusedClasses.now();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				ClassDefinition def = PlayerClassAccess.of(player).currentDefinition();
				if (def == null) {
					continue;
				}
				PlayerStatusPayload snapshot =
						TickPass.run(player, def, InfusedClasses.runtime(player), now);
				// The one steady-state emission point: a tick that moved a lane, a window
				// or a stack tells the client; a quiet tick compares for free.
				StatusSync.maybeSend(player, snapshot);
			}
		});
	}

	/**
	 * Intent receivers: what a reused bind means per class plus the picker switch path.
	 * The arms and the classless bail live in {@link SlotPresses}; the initializer keeps
	 * payload registration and delegates.
	 */
	static void registerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(SelectClassPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					ClassDefinition def = ClassSwitch.trySwitch(player, payload.classId());
					if (def == null) {
						return;
					}
					context.responseSender().sendPacket(new ClassConfirmedPayload());
				}));

		ServerPlayNetworking.registerGlobalReceiver(RushPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> SlotPresses.pressSignature(context.player())));

		ServerPlayNetworking.registerGlobalReceiver(RendPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> SlotPresses.pressUltimate(context.player())));
	}
}
