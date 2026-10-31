package com.hunter1712.infusedclasses;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
		// The class argument is vanilla's id argument: tab suggestions and what parses
		// are the same namespaced ids, so every suggestion can be typed back verbatim.
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
				dispatcher.register(Commands.literal("class")
						// Open to everyone: the root asks about the invoking player, like /class info.
						.executes(ClassCommands::info)
						.then(Commands.literal("info")
								.executes(ClassCommands::info))
						.then(Commands.literal("set")
								.requires(GAMEMASTER)
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.argument("class", IdentifierArgument.id())
												.suggests((ctx, b) -> {
													for (ClassDefinition d : ClassRegistry.all()) {
														b.suggest(d.id().toString());
													}
													return b.buildFuture();
												})
												.executes(ctx -> setClass(ctx.getSource(),
														EntityArgument.getPlayer(ctx, "player"),
														IdentifierArgument.getId(ctx, "class"))))))
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
				? null : InfusedClasses.runtime(player).state(InfusedClasses.now());
		for (ChatLine line : ClassInfo.lines(def, state)) {
			AbilityApplier.announce(player, line, false);
		}
		return 1;
	}

	/** The picker signal: the player's current class id, empty when none. One grant per open. */
	static void openPicker(ServerPlayer player) {
		PickerGrants.grant(player.getUUID());
		String current = PlayerClassAccess.of(player).writeId();
		ServerPlayNetworking.send(player, new OpenClassScreenPayload(current == null ? "" : current));
	}

	private static int setClass(CommandSourceStack source, ServerPlayer player, Identifier id) {
		ClassDefinition def = ClassSwitch.trySwitch(player, id);
		if (def == null) {
			if (source.getEntity() != player) {
				send(source, ChatLine.classSetRejected(player.getName().getString()));
			}
			return 0;
		}
		if (source.getEntity() != player) {
			send(source, ChatLine.classSet(player.getName().getString(), def.displayName()));
		}
		return 1;
	}

	/** One command-source line; the target already got the class-choice line from the switch. */
	private static void send(CommandSourceStack source, ChatLine line) {
		MutableComponent sent = AbilityApplier.component(line);
		source.sendSuccess(() -> sent, false);
	}
}
