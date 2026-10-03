package dev.infusedclasses;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** Server side. Authoritative: client sends intents, every decision happens here. */
public class InfusedClasses implements ModInitializer {
	public static final String MOD_ID = "infusedclasses";

	public static final CustomPacketPayload.Type<SelectClassPayload> SELECT_CLASS =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "select_class"));
	public static final CustomPacketPayload.Type<OpenClassScreenPayload> OPEN_CLASS_SCREEN =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "open_class_screen"));
	public static final CustomPacketPayload.Type<WarCryPayload> WAR_CRY =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "war_cry"));
	public static final CustomPacketPayload.Type<ClassConfirmedPayload> CLASS_CONFIRMED =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "class_confirmed"));

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(SELECT_CLASS, SelectClassPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(WAR_CRY, WarCryPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(OPEN_CLASS_SCREEN, OpenClassScreenPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(CLASS_CONFIRMED, ClassConfirmedPayload.STREAM_CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			PlayerClasses classes = PlayerClasses.of(player);
			classes.refresh(player);
			if (!classes.hasClass()) {
				sender.sendPacket(new OpenClassScreenPayload());
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				PlayerClasses.of(newPlayer).refresh(newPlayer));

		ServerPlayNetworking.registerGlobalReceiver(SELECT_CLASS, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					ClassDefinition def = PlayerClasses.of(player).setClass(player, payload.classId());
					if (def == null) {
						player.displayClientMessage(Component.literal("Unknown or locked class").withStyle(ChatFormatting.RED), false);
						return;
					}
					context.responseSender().sendPacket(new ClassConfirmedPayload());
					player.displayClientMessage(Component.literal("Class: ").withStyle(ChatFormatting.GRAY)
							.append(Component.literal(def.displayName()).withStyle(ChatFormatting.WHITE)), false);
				}));

		ServerPlayNetworking.registerGlobalReceiver(WAR_CRY, (payload, context) -> context.server()
				.execute(() -> warCry(context.player())));

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
														ctx.getSource().sendFailure(Component.literal("Unknown or locked class"));
														return 0;
													}
													return setClass(ctx.getSource(),
															EntityArgument.getPlayer(ctx, "player"), id);
												}))))));
	}

	private static int setClass(CommandSourceStack source, ServerPlayer player, Identifier id) {
		ClassDefinition def = PlayerClasses.of(player).setClass(player, id);
		if (def != null) {
			source.sendSuccess(() -> Component.literal("Set " + player.getName().getString()
					+ " to " + def.displayName()), true);
			return 1;
		}
		source.sendFailure(Component.literal("Unknown or locked class: " + id));
		return 0;
	}

	static void warCry(ServerPlayer player) {
		PlayerClasses classes = PlayerClasses.of(player);
		if (!ClassRegistry.WARRIOR.equals(classes.classId())) {
			return;
		}
		long now = System.currentTimeMillis();
		long remaining = classes.cooldowns().warCryRemaining(now);
		if (remaining > 0) {
			player.displayClientMessage(Component.literal("War Cry: "
					+ String.format(java.util.Locale.ROOT, "%.0fs", remaining / 1000.0)), true);
			return;
		}
		classes.cooldowns().useWarCry(now);
		player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, Cooldowns.WAR_CRY_EFFECT_TICKS, 1));
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
