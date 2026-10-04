package com.hunter1712.infusedclasses;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Server side. Authoritative: client sends intents, every decision happens here. */
public class InfusedClasses implements ModInitializer {
	public static final String MOD_ID = ClassPackets.MOD_ID;
	private static final String UNKNOWN_OR_LOCKED = "Unknown or locked class";

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
			PlayerClasses classes = PlayerClasses.of(player);
			if (!classes.hasClass()) {
				sender.sendPacket(new OpenClassScreenPayload());
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerClasses.of(newPlayer).copyFrom(PlayerClasses.of(oldPlayer));
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayer player) {
				PlayerClasses.of(player).onTrigger("on_hit_taken");
			}
		});

		// Server tick: decay conditional passive stacks and re-apply armor modifier.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				PlayerClasses classes = PlayerClasses.of(player);
				if (!classes.hasClass()) {
					continue;
				}
				// Decay stacks
				boolean stacksChanged = classes.tickDecay();
				// Re-apply armor modifier if stacks changed or if we need to initialize
				applyConditionalArmorModifier(player, classes, stacksChanged);
			}
		});
	}

	/**
	 * Applies or removes the conditional armor modifier based on current Bulwark stacks.
	 * Called every tick; only writes to attribute when stacks actually change.
	 */
	private static void applyConditionalArmorModifier(ServerPlayer player, PlayerClasses classes, boolean stacksChanged) {
		ClassDefinition def = classes.currentDefinition();
		if (def == null) {
			return;
		}
		// Find Bulwark passive (the only one with armor effect for now)
		ConditionalPassive bulwark = def.passives().stream()
				.filter(p -> "Bulwark".equals(p.name()))
				.findFirst()
				.orElse(null);
		if (bulwark == null) {
			return;
		}
		int stacks = classes.stacks("Bulwark");
		Identifier modifierId = Identifier.fromNamespaceAndPath(ClassPackets.MOD_ID, "bulwark_armor");

		AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
		if (armor == null) {
			return;
		}

		// If modifier exists, check if value matches current stacks
		AttributeModifier existing = armor.getModifier(modifierId);
		double targetValue = stacks * 1.0; // +1 armor per stack
		if (existing != null && existing.amount() == targetValue) {
			return; // already correct
		}

		// Remove old modifier if present
		if (existing != null) {
			armor.removeModifier(modifierId);
		}

		// Apply new modifier if stacks > 0
		if (stacks > 0) {
			armor.addPermanentModifier(new AttributeModifier(modifierId, targetValue, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	private static void registerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(SelectClassPayload.TYPE, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					PlayerClasses classes = PlayerClasses.of(player);
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
		PlayerClasses classes = PlayerClasses.of(player);
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
		PlayerClasses classes = PlayerClasses.of(player);
		applyAbility(player, WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), System.currentTimeMillis()));
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
			player.displayClientMessage(Component.literal(announcement.text()).withStyle(announcement.style()), true);
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
