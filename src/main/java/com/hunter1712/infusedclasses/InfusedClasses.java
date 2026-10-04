package com.hunter1712.infusedclasses;

import com.hunter1712.infusedclasses.mixin.PlayerClassAccess;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server side. Authoritative: client sends intents, every decision happens here. */
public class InfusedClasses implements ModInitializer {
	public static final String MOD_ID = ClassPackets.MOD_ID;
	private static final String UNKNOWN_OR_LOCKED = "Unknown or locked class";
	/** The transient half of per-player state, held here because it is not reachable from the player. */
	private static final Map<UUID, PlayerRuntime> RUNTIMES = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		registerPayloads();
		registerEvents();
		registerReceivers();
		registerCommands();
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
				applyPassiveEffects(player, def, runtime, stacksMoved);
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

	/**
	 * Adapter for {@link PassiveEffect}: asks the decision module what each passive on the class
	 * should be contributing, then makes the player's attributes agree. The decision alone names
	 * the attribute, the modifier id and the amount; nothing here decides what to write.
	 *
	 * Idempotent: an attribute is only written when what is applied differs from what should be,
	 * so the modifier is created once and left alone while the stacks hold still. A passive with
	 * no stacks is not resolved at all, because there is nothing it could contribute and nothing
	 * that could have changed unless decay just moved the stacks.
	 */
	private static void applyPassiveEffects(ServerPlayer player, ClassDefinition def, PlayerRuntime runtime,
			boolean stacksMoved) {
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			PassiveEffect.Modifier wanted = PassiveEffect.forStacks(passive, runtime.stacks(passive));
			if (wanted == null) {
				// No decision means no stacks. The only thing that can still be on the player is
				// a modifier those stacks earned, and only the definition knows the id it was
				// written under, so this is the one path that reads the passive rather than the
				// decision. There is no decision to be inconsistent with here.
				if (stacksMoved) {
					remove(player, passive.attribute(), passive.modifierId());
				}
				continue;
			}
			apply(player, wanted);
		}
	}

	/** Writes exactly what the decision names, and only when it differs from what is applied. */
	private static void apply(ServerPlayer player, PassiveEffect.Modifier wanted) {
		AttributeInstance instance = attribute(player, wanted.attribute());
		if (instance == null) {
			return;
		}
		AttributeModifier existing = instance.getModifier(wanted.modifierId());
		if (existing != null) {
			if (existing.amount() == wanted.amount()) {
				return; // already correct
			}
			instance.removeModifier(wanted.modifierId());
		}
		instance.addPermanentModifier(new AttributeModifier(
				wanted.modifierId(), wanted.amount(), AttributeModifier.Operation.ADD_VALUE));
	}

	/** Takes off a modifier stacks used to earn. A no-op when there is none. */
	private static void remove(ServerPlayer player, ResourceKey<Attribute> attribute, Identifier modifierId) {
		AttributeInstance instance = attribute(player, attribute);
		if (instance != null) {
			instance.removeModifier(modifierId);
		}
	}

	private static AttributeInstance attribute(ServerPlayer player, ResourceKey<Attribute> key) {
		Holder<Attribute> holder = player.registryAccess().lookupOrThrow(Registries.ATTRIBUTE)
				.get(key)
				.orElse(null);
		return holder == null ? null : player.getAttribute(holder);
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

	private static void registerCommands() {
		// ponytail: plain string arg, not a custom ArgumentType. Suggests ids, parses one line.
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
				dispatcher.register(Commands.literal("class")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.literal("set")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.argument("class", StringArgumentType.word())
												.suggests((ctx, b) -> {
													for (ClassDefinition d : ClassRegistry.all()) {
														b.suggest(d.id().toString());
													}
													return b.buildFuture();
												})
												.executes(ctx -> {
													Identifier id = Identifier.tryParse(StringArgumentType.getString(ctx, "class"));
													if (id == null) {
														ctx.getSource().sendFailure(Component.literal(UNKNOWN_OR_LOCKED));
														return 0;
													}
													return setClass(ctx.getSource(),
															EntityArgument.getPlayer(ctx, "player"), id);
												}))))
						// Reopens the picker for GUI iteration. Picking is still server-validated,
						// so reopening never bypasses the locked-class check.
						.then(Commands.literal("select")
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> {
											ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
											ServerPlayNetworking.send(player, new OpenClassScreenPayload());
											ctx.getSource().sendSuccess(() -> Component.literal(
													"Opened class picker for " + player.getName().getString()), true);
											return 1;
										})))));
	}

	private static int setClass(CommandSourceStack source, ServerPlayer player, Identifier id) {
		PlayerClasses classes = PlayerClassAccess.of(player);
		ClassDefinition def = classes.trySetClass(id);
		if (def != null) {
			source.sendSuccess(() -> Component.literal("Set " + player.getName().getString()
					+ " to " + def.displayName()), true);
			return 1;
		}
		source.sendFailure(Component.literal(UNKNOWN_OR_LOCKED + ": " + id));
		return 0;
	}

	static void warCry(ServerPlayer player) {
		PlayerClasses classes = PlayerClassAccess.of(player);
		applyAbility(player, WarCryAbility.trigger(classes.currentDefinition(),
				runtime(player).cooldowns(), System.currentTimeMillis()));
	}

	/**
	 * Applies one ability result. The single place an outcome reaches the player: a result with no
	 * outcome and nothing to say is a no-op, which is what wrong-class and on-cooldown presses are.
	 */
	private static void applyAbility(ServerPlayer player, WarCryAbility.Result result) {
		WarCryAbility.Outcome outcome = result.outcome();
		if (outcome != null) {
			BuiltInRegistries.MOB_EFFECT.get(outcome.effect()).ifPresent(effect ->
					player.addEffect(new MobEffectInstance(effect, outcome.durationTicks(), outcome.amplifier())));
			if (player.level() instanceof ServerLevel level) {
				BuiltInRegistries.SOUND_EVENT.getOptional(outcome.sound()).ifPresent(sound ->
						level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F));
				BuiltInRegistries.PARTICLE_TYPE.getOptional(outcome.particle())
						.filter(SimpleParticleType.class::isInstance)
						.map(SimpleParticleType.class::cast)
						.ifPresent(particle -> particleRing(level, player, particle));
			}
		}
		WarCryAbility.ChatLine announcement = result.announcement();
		if (announcement != null) {
			MutableComponent line = Component.literal(announcement.text());
			if (announcement.style() != null) {
				line = line.withStyle(announcement.style());
			}
			player.displayClientMessage(line, true);
		}
	}

	// ponytail: flat ring of particles. Real effect cloud only if the visual ever matters.
	private static void particleRing(ServerLevel level, ServerPlayer player, SimpleParticleType particle) {
		Vec3 at = player.position();
		for (int i = 0; i < 24; i++) {
			double angle = (Math.PI * 2 * i) / 24;
			level.sendParticles(particle,
					at.x + Math.cos(angle) * 2.0, at.y + 0.2, at.z + Math.sin(angle) * 2.0,
					1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}
