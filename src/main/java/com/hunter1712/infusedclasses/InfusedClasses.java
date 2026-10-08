package com.hunter1712.infusedclasses;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Server side. Authoritative: client sends intents, every decision happens here. */
public class InfusedClasses implements ModInitializer {
	static final String UNKNOWN_OR_LOCKED = "Unknown or locked class";
	/** The transient half of per-player state, held here because it is not reachable from the player. */
	private static final PlayerRuntimes RUNTIMES = new PlayerRuntimes();

	@Override
	public void onInitialize() {
		registerPayloads();
		ServerEvents.register();
		registerReceivers();
		ClassCommands.register();
	}

	private static void registerPayloads() {
		PayloadTypeRegistry.playC2S().register(SelectClassPayload.TYPE, SelectClassPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(RushPayload.TYPE, RushPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(RendPayload.TYPE, RendPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(OpenClassScreenPayload.TYPE, OpenClassScreenPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClassConfirmedPayload.TYPE, ClassConfirmedPayload.STREAM_CODEC);
	}

	/**
	 * The transient state of one player, and the whole of its lifetime: created the first time
	 * something needs it, replaced by an empty one when the player respawns, dropped when they
	 * log out. Nothing in it is persisted, so none of it is ever written or copied.
	 */
	static PlayerRuntime runtime(ServerPlayer player) {
		return RUNTIMES.runtime(player.getUUID());
	}

	/** The runtime store, for event wiring that owns no state of its own. */
	static PlayerRuntimes runtimes() {
		return RUNTIMES;
	}

	/** Switch path shared by the picker receiver and the admin command. */
	static void onClassChanged(ServerPlayer player, ClassDefinition oldDef, ClassDefinition newDef) {
		if (oldDef == newDef) {
			return;
		}
		PlayerRuntime runtime = runtime(player);
		PassiveApplier.onClassChanged(player, runtime, oldDef, newDef);
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

		ServerPlayNetworking.registerGlobalReceiver(RushPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					long now = System.currentTimeMillis();
					PlayerClasses classes = PlayerClassAccess.of(player);
					ClassDefinition def = classes.currentDefinition();
					PlayerRuntime runtime = runtime(player);
					// Legionary reuses the R bind as the rotate intent, so binds survive.
					// Rotate announces without fanfare on purpose: it carries no cast sound.
					if (BindMorph.usesRotate(def)) {
						RotateAbility.Result result = RotateAbility.fire(def, runtime, now);
						AbilityApplier.announce(player, result.announcement());
						return;
					}
					RushAbility.Result result = RushAbility.fire(def, runtime, now);
					RushApplier.apply(player, result);
				}));

		ServerPlayNetworking.registerGlobalReceiver(RendPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					long now = System.currentTimeMillis();
					PlayerClasses classes = PlayerClassAccess.of(player);
					ClassDefinition def = classes.currentDefinition();
					PlayerRuntime runtime = runtime(player);
					// Legionary reuses the G bind as the oath intent: stance morphs it.
					switch (BindMorph.oathFor(def, runtime.stance())) {
						case IMPETUS -> {
							ImpetusAbility.Result result = ImpetusAbility.fire(def, runtime, now);
							AbilityApplier.announceAndCast(player, result.announcement(),
									result.kind() == ImpetusAbility.Kind.FIRED, ImpetusAbility.CAST_SOUND);
						}
						case TESTUDO -> {
							TestudoAbility.Result result = TestudoAbility.fire(def, runtime, now);
							AbilityApplier.announceAndCast(player, result.announcement(),
									result.kind() == TestudoAbility.Kind.FIRED, TestudoAbility.CAST_SOUND);
						}
						case REND -> {
							RendAbility.Result result = RendAbility.fire(def, runtime, now);
							AbilityApplier.announceAndCast(player, result.announcement(),
									result.kind() == RendAbility.Kind.FIRED, RendAbility.CAST_SOUND);
							// The adapter spends only the priced passive; a fizzle spends nothing.
							// The empower damage lands on the next melee hit inside the window.
							if (result.consume()) {
								PassiveApplier.consumeSpent(player, runtime, def);
							}
						}
					}
				}));
	}
}
