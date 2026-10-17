package com.hunter1712.infusedclasses;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.function.Predicate;

/** Wiring for the {@code /class} commands. Validation stays in {@link PlayerClasses}. */
public final class ClassCommands {
	/** The per-player "has cheats": permission level 2, vanilla's gamemaster. */
	private static final Predicate<CommandSourceStack> GAMEMASTER =
			source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);

	private ClassCommands() {
	}

	public static void register() {
		// ponytail: plain string arg, not a custom ArgumentType. Suggests ids, parses one line.
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
				dispatcher.register(Commands.literal("class")
						// Open to everyone: the root asks about the invoking player, like /class info.
						.executes(ClassCommands::info)
						.then(Commands.literal("info")
								.executes(ClassCommands::info))
						.then(Commands.literal("set")
								.requires(GAMEMASTER)
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
														ctx.getSource().sendFailure(Component.literal(InfusedClasses.UNKNOWN_OR_LOCKED));
														return 0;
													}
													return setClass(ctx.getSource(),
															EntityArgument.getPlayer(ctx, "player"), id);
												}))))
						// testing-only: reopens the picker for GUI iteration. Picking is still server-validated.
						.then(Commands.literal("select")
								.requires(GAMEMASTER)
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> {
											ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
											openPicker(player);
											ctx.getSource().sendSuccess(() -> Component.literal(
													"Opened class picker for " + player.getName().getString()), true);
											return 1;
										})))
						// self-only, cheats-gated: reopens the invoking player's own picker. The picker is the feedback.
						.then(Commands.literal("pick")
								.requires(GAMEMASTER)
								.executes(ctx -> {
									openPicker(ctx.getSource().getPlayerOrException());
									return 1;
								}))));
	}

	/** {@code /class} and {@code /class info}: the invoking player's own class, lanes and stance. */
	private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ClassDefinition def = PlayerClassAccess.of(player).currentDefinition();
		// Classless reads no transient state (ADR-0005): the lines decision needs a
		// runtime state only when there is a class to draw lines for.
		PlayerRuntime.State state = def == null
				? null : InfusedClasses.runtime(player).state(System.currentTimeMillis());
		for (ChatLine line : ClassInfo.lines(def, state)) {
			AbilityApplier.announce(player, line, false);
		}
		return 1;
	}

	/** The picker signal: the player's current class id, empty when none. */
	private static void openPicker(ServerPlayer player) {
		String current = PlayerClassAccess.of(player).writeId();
		ServerPlayNetworking.send(player, new OpenClassScreenPayload(current == null ? "" : current));
	}

	private static int setClass(CommandSourceStack source, ServerPlayer player, Identifier id) {
		return ClassSwitch.trySwitch(player, id) == null ? 0 : 1;
	}
}
