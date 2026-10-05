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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server side. Authoritative: client sends intents, every decision happens here. */
public class InfusedClasses implements ModInitializer {
	static final String UNKNOWN_OR_LOCKED = "Unknown or locked class";
	/** The transient half of per-player state, held here because it is not reachable from the player. */
	private static final Map<UUID, PlayerRuntime> RUNTIMES = new ConcurrentHashMap<>();

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
			// Load-bearing ordering: both players share one UUID, so the old entry has to go
			// before the new one is created. The other order would hand the respawned player the
			// stacks their old body earned. This lives in wiring the suite does not cover, so it
			// is marked rather than faked.
			RUNTIMES.remove(oldPlayer.getUUID());
			runtime(newPlayer);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				RUNTIMES.remove(handler.getPlayer().getUUID()));

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return;
			}
			PlayerClasses classes = PlayerClassAccess.of(player);
			// Classless players are inert, and cost nothing: no transient state is made for them.
			if (!classes.hasClass()) {
				return;
			}
			runtime(player).onTrigger(classes.currentDefinition(), "on_hit_taken", System.currentTimeMillis());
		});

		// Server tick: decay conditional passive stacks, then put their effect on the player.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = System.currentTimeMillis();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				PlayerClasses classes = PlayerClassAccess.of(player);
				if (!classes.hasClass()) {
					continue;
				}
				ClassDefinition def = classes.currentDefinition();
				PlayerRuntime runtime = runtime(player);
				boolean stacksMoved = runtime.tickDecay(def, now);
				PassiveApplier.apply(player, def, runtime, stacksMoved);
			}
		});
	}

	/**
	 * The transient state of one player, and the whole of its lifetime: created the first time
	 * something needs it, replaced by an empty one when the player respawns, dropped when they
	 * log out. Nothing in it is persisted, so none of it is ever written or copied.
	 */
	private static PlayerRuntime runtime(ServerPlayer player) {
		return RUNTIMES.computeIfAbsent(player.getUUID(), id -> new PlayerRuntime());
	}

	private static void registerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(SelectClassPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					PlayerClasses classes = PlayerClassAccess.of(player);
					ClassDefinition def = classes.trySetClass(payload.classId());
					if (def == null) {
						player.displayClientMessage(Component.literal(UNKNOWN_OR_LOCKED).withStyle(ChatFormatting.RED), false);
						return;
					}
						context.responseSender().sendPacket(new ClassConfirmedPayload());
					player.displayClientMessage(Component.literal("Class: ").withStyle(ChatFormatting.GRAY)
							.append(Component.literal(def.displayName()).withStyle(ChatFormatting.WHITE)), false);
				}));

		ServerPlayNetworking.registerGlobalReceiver(WarCryPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> warCry(context.player())));
	}

	static void warCry(ServerPlayer player) {
		PlayerClasses classes = PlayerClassAccess.of(player);
		AbilityApplier.apply(player, WarCryAbility.trigger(classes.currentDefinition(),
				runtime(player).cooldowns(), System.currentTimeMillis()));
	}
}
