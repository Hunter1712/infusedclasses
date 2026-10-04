package com.hunter1712.infusedclasses;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.world.item.AxeItem;
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
		registerTestingCommands();
	}

	private static void registerPayloads() {
		PayloadTypeRegistry.playC2S().register(ClassPackets.SELECT_CLASS, SelectClassPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ClassPackets.WAR_CRY, WarCryPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ClassPackets.SPEND_TALENT, SpendTalentPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ClassPackets.REQUEST_TALENTS, RequestTalentsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ClassPackets.RESPEC_TALENTS, RespecTalentsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClassPackets.OPEN_CLASS_SCREEN, OpenClassScreenPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClassPackets.CLASS_CONFIRMED, ClassConfirmedPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClassPackets.TALENT_STATE, TalentStatePayload.STREAM_CODEC);
	}

	private static void registerEvents() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			PlayerClasses classes = PlayerClasses.of(player);
			classes.refresh(player);
			if (!classes.hasClass()) {
				sender.sendPacket(new OpenClassScreenPayload());
			}
		});

		// Vanilla clears attribute modifiers on respawn and the new entity starts with
		// empty in-memory state, so copy the choice before re-applying passives.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerClasses.of(newPlayer).copyFrom(PlayerClasses.of(oldPlayer));
			PlayerClasses.of(newPlayer).refresh(newPlayer);
		});

		// Class XP: any class levels on kills (per-class map); Warrior earns melee-affinity bonus. Vanilla XP untouched.
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, killer, victim, source) -> {
			if (killer instanceof ServerPlayer player && PlayerClasses.of(player).hasClass()) {
				boolean melee = ClassRegistry.WARRIOR.equals(PlayerClasses.of(player).classId()) && isMeleeAffinity(player);
				int gained = PlayerClasses.of(player).addKillXp(melee);
				if (gained > 0) {
					levelUpFeedback(player);
				}
			}
		});

		// Thick Skin: taking hits stacks armor only when unlocked; tick decay handles the rest.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player
					&& SkillTrees.thickSkinUnlocked(PlayerClasses.of(player).unlockedNodes())) {
				PlayerClasses classes = PlayerClasses.of(player);
				classes.thickSkin().hit(System.currentTimeMillis(), SkillTrees.capFor(classes.unlockedNodes()));
				classes.refreshThickSkin(player);
			}
		});

		// ponytail: one server-tick scan for stack decay. Per-player tick hooks only if this ever shows in a profile.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = System.currentTimeMillis();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				PlayerClasses classes = PlayerClasses.of(player);
				if (classes.thickSkin().count() > 0
						&& classes.thickSkin().tick(now, SkillTrees.decayFor(classes.unlockedNodes())) == 0) {
					classes.refreshThickSkin(player);
				}
			}
		});
	}

	private static void registerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(ClassPackets.SELECT_CLASS, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					ClassDefinition def = PlayerClasses.of(player).setClass(player, payload.classId());
					if (def == null) {
						player.displayClientMessage(Component.literal(UNKNOWN_OR_LOCKED).withStyle(ChatFormatting.RED), false);
						return;
					}
					context.responseSender().sendPacket(new ClassConfirmedPayload());
					player.displayClientMessage(Component.literal("Class: ").withStyle(ChatFormatting.GRAY)
							.append(Component.literal(def.displayName()).withStyle(ChatFormatting.WHITE)), false);
				}));

		ServerPlayNetworking.registerGlobalReceiver(ClassPackets.WAR_CRY, (payload, context) -> context.server()
				.execute(() -> warCry(context.player())));

		ServerPlayNetworking.registerGlobalReceiver(ClassPackets.REQUEST_TALENTS, (payload, context) -> context.server()
				.execute(() -> sendTalentState(context.player())));

		ServerPlayNetworking.registerGlobalReceiver(ClassPackets.RESPEC_TALENTS, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					PlayerClasses.of(player).respec();
					PlayerClasses.of(player).refresh(player);
					sendTalentState(player);
				}));

		ServerPlayNetworking.registerGlobalReceiver(ClassPackets.SPEND_TALENT, (payload, context) -> context.server()
				.execute(() -> {
					ServerPlayer player = context.player();
					PlayerClasses classes = PlayerClasses.of(player);
					if (!spendTalent(player, payload.nodeId())) {
						return;
					}
					classes.refresh(player);
					sendTalentState(player);
				}));
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

		// ponytail: plain string arg, same as /class set. Every talent action has a command fallback.
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
				dispatcher.register(Commands.literal("talents")
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							sendTalentState(player);
							return 1;
						})
						.then(Commands.literal("spend")
								.then(Commands.argument("node", StringArgumentType.word())
										.suggests((ctx, b) -> {
											for (SkillTrees.Node n : SkillTrees.NODES) {
												b.suggest(n.id());
											}
											return b.buildFuture();
										})
										.executes(ctx -> {
											ServerPlayer player = ctx.getSource().getPlayerOrException();
											String node = StringArgumentType.getString(ctx, "node");
											if (!spendTalent(player, node)) {
												return 0;
											}
											PlayerClasses.of(player).refresh(player);
											sendTalentState(player);
											return 1;
										})))
						.then(Commands.literal("respec")
								.executes(ctx -> respec(ctx.getSource(), ctx.getSource().getPlayerOrException()))
								.then(Commands.argument("target", EntityArgument.player())
										.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
										.executes(ctx -> respec(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))));
	}

	/** Testing/admin extras. Separate registration so the main /talents chain stays untouched. */
	private static void registerTestingCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
				dispatcher.register(Commands.literal("talents")
						.then(Commands.literal("xp")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.then(Commands.argument("target", EntityArgument.player())
										.then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000))
												.executes(ctx -> grantXp(ctx.getSource(),
														EntityArgument.getPlayer(ctx, "target"),
														IntegerArgumentType.getInteger(ctx, "amount"))))))
						.then(Commands.literal("grant")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.then(Commands.argument("target", EntityArgument.player())
										.then(Commands.argument("points", IntegerArgumentType.integer(1, 100))
												.executes(ctx -> grantPoints(ctx.getSource(),
														EntityArgument.getPlayer(ctx, "target"),
														IntegerArgumentType.getInteger(ctx, "points"))))))));
	}

	private static int respec(CommandSourceStack source, ServerPlayer player) {
		PlayerClasses classes = PlayerClasses.of(player);
		int refund = classes.respec();
		classes.refresh(player);
		sendTalentState(player);
		source.sendSuccess(
				() -> Component.literal("Respec " + player.getName().getString() + ": refunded " + refund + " points"), true);
		return 1;
	}

	/** Testing/admin: raw XP grant with normal level-up feedback. */
	private static int grantXp(CommandSourceStack source, ServerPlayer player, int amount) {
		PlayerClasses classes = PlayerClasses.of(player);
		if (!classes.hasClass()) {
			source.sendFailure(Component.literal("Target has no class"));
			return 0;
		}
		int gained = classes.addXp(amount);
		if (gained > 0) {
			levelUpFeedback(player);
		} else {
			source.sendSuccess(
					() -> Component.literal("Granted " + amount + " XP to " + player.getName().getString()
							+ " (level " + classes.level() + ", " + classes.progress().xp + " XP)"), true);
		}
		return 1;
	}

	/** Testing/admin: unspent points straight in, no grind. */
	private static int grantPoints(CommandSourceStack source, ServerPlayer player, int points) {
		PlayerClasses classes = PlayerClasses.of(player);
		if (!classes.hasClass()) {
			source.sendFailure(Component.literal("Target has no class"));
			return 0;
		}
		classes.progress().unspent += points;
		classes.refresh(player);
		sendTalentState(player);
		source.sendSuccess(
				() -> Component.literal("Granted " + points + " points to " + player.getName().getString()
						+ " (" + classes.unspentPoints() + " unspent)"), true);
		return 1;
	}

	/** Title plus sound plus chat on every level-up, kills and grants alike. */
	static void levelUpFeedback(ServerPlayer player) {
		PlayerClasses classes = PlayerClasses.of(player);
		if (player.level() instanceof ServerLevel level) {
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
					SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		player.connection.send(new ClientboundSetTitleTextPacket(
				Component.literal("Level " + classes.level()).withStyle(ChatFormatting.GOLD)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
				Component.literal(classes.unspentPoints() + " talent point"
						+ (classes.unspentPoints() == 1 ? "" : "s") + " — /talents").withStyle(ChatFormatting.GRAY)));
		player.displayClientMessage(Component.literal(
				"Level " + classes.level() + " — " + classes.unspentPoints() + " talent point"
						+ (classes.unspentPoints() == 1 ? "" : "s") + " (/talents)"), false);
	}

	private static int setClass(CommandSourceStack source, ServerPlayer player, Identifier id) {
		ClassDefinition def = PlayerClasses.of(player).setClass(player, id);
		if (def != null) {
			source.sendSuccess(() -> Component.literal("Set " + player.getName().getString()
					+ " to " + def.displayName()), true);
			return 1;
		}
		source.sendFailure(Component.literal(UNKNOWN_OR_LOCKED + ": " + id));
		return 0;
	}

	static String talentStateString(ServerPlayer player) {
		PlayerClasses classes = PlayerClasses.of(player);
		SkillTrees.Progress p = classes.progress();
		return p == null ? SkillTrees.encodeProgress(new SkillTrees.Progress()) : SkillTrees.encodeProgress(p);
	}

	static void sendTalentState(ServerPlayer player) {
		ServerPlayNetworking.send(player, new TalentStatePayload(talentStateString(player)));
	}

	/** Warrior affinity: axes by class, swords by id convention (no SwordItem on this version). */
	static boolean isMeleeAffinity(ServerPlayer player) {
		var item = player.getMainHandItem().getItem();
		if (item instanceof AxeItem) {
			return true;
		}
		Identifier key = BuiltInRegistries.ITEM.getKey(item);
		return key != null && key.getPath().endsWith("_sword");
	}

	/** Validates class/gates/points server-side and chats the reason on denial. True when spent. */
	static boolean spendTalent(ServerPlayer player, String nodeId) {
		PlayerClasses classes = PlayerClasses.of(player);
		SkillTrees.Progress p = classes.progress();
		if (p == null) {
			player.displayClientMessage(Component.literal("Pick a class first").withStyle(ChatFormatting.RED), false);
			return false;
		}
		if (!ClassRegistry.WARRIOR.equals(classes.classId())) {
			player.displayClientMessage(Component.literal("Warrior talents only in MVP").withStyle(ChatFormatting.RED), false);
			return false;
		}
		String reason = SkillTrees.unlockRejectReason(p.unlocked, p.unspent, nodeId);
		if (reason != null) {
			player.displayClientMessage(Component.literal(reason).withStyle(ChatFormatting.RED), false);
			return false;
		}
		classes.tryUnlock(nodeId);
		player.displayClientMessage(Component.literal("Unlocked " + nodeId).withStyle(ChatFormatting.GREEN), false);
		return true;
	}

	static void warCry(ServerPlayer player) {
		PlayerClasses classes = PlayerClasses.of(player);
		WarCryAbility.Result result = WarCryAbility.trigger(classes, System.currentTimeMillis());
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
		SkillTrees.WarCryEffects cry = SkillTrees.warCryEffects(classes.unlockedNodes());
		player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, cry.strengthTicks(), cry.strengthAmp()));
		if (cry.resistance()) {
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, cry.resistTicks(), cry.resistAmp()));
		}
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
