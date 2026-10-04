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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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

		// Server tick: decay conditional passive stacks, then put their effect on the player.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				PlayerClasses classes = PlayerClasses.of(player);
				if (!classes.hasClass()) {
					continue;
				}
				classes.tickDecay();
				applyPassiveEffects(player, classes.currentDefinition(), classes);
			}
		});
	}

	/**
	 * Adapter for {@link PassiveEffect}: asks the decision module what each passive on the class
	 * should be contributing, then makes the player's attributes agree. Idempotent: an attribute is
	 * only written when what is applied differs from what should be, so the modifier is created
	 * once and left alone while the stacks hold still.
	 */
	private static void applyPassiveEffects(ServerPlayer player, ClassDefinition def, PlayerClasses classes) {
		if (def == null) {
			return;
		}
		for (ConditionalPassive passive : def.passives()) {
			apply(player, passive, PassiveEffect.forStacks(passive, classes.stacks(passive.name())));
		}
	}

	private static void apply(ServerPlayer player, ConditionalPassive passive, PassiveEffect.Modifier wanted) {
		Holder<Attribute> attribute = player.registryAccess().lookupOrThrow(Registries.ATTRIBUTE)
				.get(passive.attribute())
				.orElse(null);
		if (attribute == null) {
			return;
		}
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		AttributeModifier existing = instance.getModifier(passive.modifierId());
		if (existing != null) {
			if (wanted != null && existing.amount() == wanted.amount()) {
				return; // already correct
			}
			instance.removeModifier(passive.modifierId());
		}
		// No decision means no stacks, so the passive contributes nothing right now.
		if (wanted != null) {
			instance.addPermanentModifier(new AttributeModifier(
					wanted.modifierId(), wanted.amount(), AttributeModifier.Operation.ADD_VALUE));
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
		WarCryAbility.Result result = WarCryAbility.trigger(classes.currentDefinition(), classes.cooldowns(), System.currentTimeMillis());
		switch (result.kind()) {
			case WRONG_CLASS -> {
				return;
			}
			case ON_COOLDOWN -> {
				player.displayClientMessage(Component.literal("War Cry: "
						+ String.format(java.util.Locale.ROOT, "%.0fs", result.remainingMs() / 1000.0)), true);
				return;
			}
			case FIRED -> {
			}
		}
		player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, WarCryAbility.EFFECT_TICKS, WarCryAbility.EFFECT_AMPLIFIER));
		if (player.level() instanceof ServerLevel level) {
			level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.0F);
			Vec3 at = player.position();
			// ponytail: flat ring of particles. Real effect cloud only if the visual ever matters.
			for (int i = 0; i < 24; i++) {
				double angle = (Math.PI * 2 * i) / 24;
				level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
						at.x + Math.cos(angle) * 2.0, at.y + 0.2, at.z + Math.sin(angle) * 2.0,
						1, 0.0, 0.0, 0.0, 0.0);
			}
		}
		player.displayClientMessage(Component.literal("War Cry!").withStyle(ChatFormatting.RED), true);
	}
}
