package com.hunter1712.infusedclasses;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

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
		PayloadTypeRegistry.playC2S().register(WarCryPayload.TYPE, WarCryPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(SecondWindPayload.TYPE, SecondWindPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(OpenClassScreenPayload.TYPE, OpenClassScreenPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClassConfirmedPayload.TYPE, ClassConfirmedPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PassiveStacksPayload.TYPE, PassiveStacksPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(WarCryExpiryPayload.TYPE, WarCryExpiryPayload.STREAM_CODEC);
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
		// The old stacks just dropped to zero: send both sides so no old row sticks around.
		ServerSignals.sendStacksForSwitch(player, oldDef, newDef, runtime);
		// A new class starts with no War Cry, so no old row sticks around either.
		runtime.clearWarCrySignal();
		ServerSignals.sendWarCry(player, 0);
	}

	/**
	 * Attacker path for the same damage event: one decided extension per victim. Only players
	 * and hostile mobs roll — passive animals never feed the upkeep — and only War Cry's own
	 * Strength (amplifier 1) counts, so a beacon's Strength I can neither arm a proc nor feed the row.
	 */
	static void onAttackerHit(ServerPlayer attacker, LivingEntity victim) {
		ClassDefinition def = PlayerClassAccess.of(attacker).currentDefinition();
		if (def == null) {
			return;
		}
		var current = attacker.getEffect(MobEffects.STRENGTH);
		int next = WarCryExtension.extendRoll(new WarCryExtension.Hit(
				ClassRegistry.WARRIOR_ABILITY.equals(def.activeAbility()),
				attacker.getAttackStrengthScale(0.0F) == 1.0F,
				current != null && current.getAmplifier() == WarCryAbility.EFFECT_AMPLIFIER,
				victim instanceof ServerPlayer || victim instanceof Enemy,
				current == null ? 0 : current.getDuration(),
				attacker.getRandom().nextFloat()));
		// The applier owns the effect and the puff; a true answer means the clock is worth sending.
		if (AbilityApplier.applyExtension(attacker, victim, next)) {
			runtime(attacker).markWarCrySent();
			ServerSignals.sendWarCry(attacker, next);
		}
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
					if (result.kind() == WarCryAbility.Kind.FIRED) {
						runtime(player).markWarCrySent();
						ServerSignals.sendWarCry(player, WarCryAbility.EFFECT_TICKS);
					}
				}));

		ServerPlayNetworking.registerGlobalReceiver(SecondWindPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					long now = System.currentTimeMillis();
					PlayerClasses classes = PlayerClassAccess.of(player);
					ClassDefinition def = classes.currentDefinition();
					PlayerRuntime runtime = runtime(player);
					SecondWindAbility.Result result = SecondWindAbility.fire(def, runtime, now);
					AbilityApplier.apply(player, result);
					// The adapter consumes only the priced passive; a true answer means the stacks signal went stale.
					if (result.consume() && PassiveApplier.consumeSpent(player, runtime, def)) {
						ServerSignals.sendStacks(player, def, runtime, now);
					}
				}));
	}
}
