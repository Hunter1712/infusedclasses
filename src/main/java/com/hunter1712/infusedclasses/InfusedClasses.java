package com.hunter1712.infusedclasses;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Server side. Authoritative: client sends intents, every decision happens here. */
public class InfusedClasses implements ModInitializer {
	static final String UNKNOWN_OR_LOCKED = "Unknown or locked class";
	/** The transient half of per-player state, held here because it is not reachable from the player. */
	private static final PlayerRuntimes RUNTIMES = new PlayerRuntimes();

	@Override
	public void onInitialize() {
		registerPayloads();
		registerEvents();
		registerReceivers();
		ClassCommands.register();
	}

	private static void registerPayloads() {
		PayloadTypeRegistry.playC2S().register(SelectClassPayload.TYPE, SelectClassPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(WarCryPayload.TYPE, WarCryPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(OpenClassScreenPayload.TYPE, OpenClassScreenPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClassConfirmedPayload.TYPE, ClassConfirmedPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PassiveStacksPayload.TYPE, PassiveStacksPayload.STREAM_CODEC);
	}

	private static void registerEvents() {
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
			RUNTIMES.respawn(newPlayer.getUUID());
			// Fresh runtime reads as zero-stack entries, which clear the client's rows.
			sendStacks(newPlayer, PlayerClassAccess.of(newPlayer).currentDefinition(), System.currentTimeMillis());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				RUNTIMES.remove(handler.getPlayer().getUUID()));

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return;
			}
			PlayerClasses classes = PlayerClassAccess.of(player);
			// Classless players are inert, and cost nothing: no transient state is made for them.
			ClassDefinition def = classes.currentDefinition();
			if (def == null) {
				return;
			}
			long now = System.currentTimeMillis();
			runtime(player).onTrigger(def, ConditionalPassive.ON_HIT_TAKEN, now);
			// A hit always re-stamps grace, so the countdown is always worth re-sending.
			sendStacks(player, def, now);
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
				PlayerRuntime runtime = runtime(player);
				// A tick that moved no stacks changed nothing the client shows, so it sends nothing.
				if (!PassiveApplier.apply(player, def, runtime, now).isEmpty()) {
					sendStacks(player, def, now);
				}
			}
		});
	}

	/**
	 * The transient state of one player, and the whole of its lifetime: created the first time
	 * something needs it, replaced by an empty one when the player respawns, dropped when they
	 * log out. Nothing in it is persisted, so none of it is ever written or copied.
	 */
	private static PlayerRuntime runtime(ServerPlayer player) {
		return RUNTIMES.runtime(player.getUUID());
	}

	/** Switch path shared by the picker receiver and the admin command. */
	static void onClassChanged(ServerPlayer player, ClassDefinition oldDef, ClassDefinition newDef) {
		if (oldDef == newDef) {
			return;
		}
		PassiveApplier.onClassChanged(player, runtime(player), oldDef, newDef);
		// The old stacks just dropped to zero: send both sides so no old row sticks around.
		sendStacksForSwitch(player, oldDef, newDef);
	}

	/** Switch clear: zero-stack entries for both sides, so no old row sticks around. */
	private static void sendStacksForSwitch(ServerPlayer player, ClassDefinition oldDef, ClassDefinition newDef) {
		long now = System.currentTimeMillis();
		List<PassiveStacksPayload.Entry> out = new ArrayList<>();
		if (oldDef != null) {
			out.addAll(PassiveStacksPayload.entriesFor(oldDef, runtime(player), now));
		}
		if (newDef != null) {
			out.addAll(PassiveStacksPayload.entriesFor(newDef, runtime(player), now));
		}
		if (!out.isEmpty()) {
			ServerPlayNetworking.send(player, new PassiveStacksPayload(out));
		}
	}

	/** One stacks signal, only for players holding a class. Classless players cost no packet. */
	private static void sendStacks(ServerPlayer player, ClassDefinition def, long now) {
		if (def == null) {
			return;
		}
		ServerPlayNetworking.send(player, new PassiveStacksPayload(PassiveStacksPayload.entriesFor(def, runtime(player), now)));
	}

	private static void registerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(SelectClassPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					PlayerClasses classes = PlayerClassAccess.of(player);
					ClassDefinition oldDef = classes.currentDefinition();
					ClassDefinition def = classes.trySetClass(payload.classId());
					if (def == null) {
						player.displayClientMessage(Component.literal(UNKNOWN_OR_LOCKED).withStyle(ChatFormatting.RED), false);
						return;
					}
					onClassChanged(player, oldDef, def);
						context.responseSender().sendPacket(new ClassConfirmedPayload());
					player.displayClientMessage(Component.literal("Class: ").withStyle(ChatFormatting.GRAY)
							.append(Component.literal(def.displayName()).withStyle(ChatFormatting.WHITE)), false);
				}));

		ServerPlayNetworking.registerGlobalReceiver(WarCryPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					long now = System.currentTimeMillis();
					PlayerClasses classes = PlayerClassAccess.of(player);
					WarCryAbility.Result result = WarCryAbility.fire(classes.currentDefinition(), runtime(player), now);
					AbilityApplier.apply(player, result);
				}));
	}
}
