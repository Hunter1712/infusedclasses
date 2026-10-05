package com.hunter1712.infusedclasses;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Wiring for the {@code /class} commands. Validation stays in {@link PlayerClasses}. */
public final class ClassCommands {
	private ClassCommands() {
	}

	public static void register() {
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
														ctx.getSource().sendFailure(Component.literal(InfusedClasses.UNKNOWN_OR_LOCKED));
														return 0;
													}
													return setClass(ctx.getSource(),
															EntityArgument.getPlayer(ctx, "player"), id);
												}))))
						// testing-only: reopens the picker for GUI iteration. Picking is still server-validated,
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
		ClassDefinition oldDef = classes.currentDefinition();
		ClassDefinition def = classes.trySetClass(id);
		if (def != null) {
			InfusedClasses.onClassChanged(player, oldDef, def);
			source.sendSuccess(() -> Component.literal("Set " + player.getName().getString()
					+ " to " + def.displayName()), true);
			return 1;
		}
		source.sendFailure(Component.literal(InfusedClasses.UNKNOWN_OR_LOCKED + ": " + id));
		return 0;
	}
}
