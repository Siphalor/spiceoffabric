package de.siphalor.spiceoffabric.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.siphalor.spiceoffabric.SpiceOfFabric;
//- import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.container.FoodJournalScreenHandler;
import de.siphalor.spiceoffabric.container.FoodJournalView;
import de.siphalor.spiceoffabric.networking.SOFCommonNetworking;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.Collection;
import java.util.Collections;

public class SOFCommands {
	private static final String AMOUNT_ARGUMENT = "amount";
	private static final String TARGETS_ARGUMENT = "targets";

	private SOFCommands() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((commandDispatcher, registryAccess, registrationEnvironment) -> {
			if (SpiceOfFabric.config.enableJournalCommand) {
				commandDispatcher.register(Commands.literal(SpiceOfFabric.MOD_ID + ":journal")
						.executes(context -> openJournal(context.getSource())));
			}
			commandDispatcher.register(Commands.literal(SpiceOfFabric.MOD_ID + ":clear_history")
					.requires(source -> source.hasPermission(2))
					.executes(context ->
							clearHistory(
									context.getSource(),
									Collections.singleton(context.getSource().getPlayer())
							)
					).then(
							Commands.argument(TARGETS_ARGUMENT, EntityArgument.players())
									.executes(context ->
											clearHistory(
													context.getSource(),
													EntityArgument.getPlayers(context, TARGETS_ARGUMENT)
											)
									)
					)
			);
			commandDispatcher.register(Commands.literal(SpiceOfFabric.MOD_ID + ":set_base_max_health")
					.requires(source -> source.hasPermission(2))
					.then(
							Commands.argument(TARGETS_ARGUMENT, EntityArgument.players())
									.then(
											Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, 200))
													.executes(context ->
															setBaseMaxHealth(
																	context.getSource(),
																	EntityArgument.getPlayers(context, TARGETS_ARGUMENT),
																	IntegerArgumentType.getInteger(context, AMOUNT_ARGUMENT)
															)
													)
									)
					)
					.then(
							Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, 200))
									.executes(context ->
											setBaseMaxHealth(
													context.getSource(),
													Collections.singleton(context.getSource().getPlayer()),
													IntegerArgumentType.getInteger(context, AMOUNT_ARGUMENT)
											)
									)
					)
			);
			commandDispatcher.register(Commands.literal(SpiceOfFabric.MOD_ID + ":update_max_health")
					.requires(source -> source.hasPermission(2))
					.executes(context ->
							updateMaxHealth(context.getSource(), Collections.singleton(context.getSource().getPlayer()))
					).then(
							Commands.argument(TARGETS_ARGUMENT, EntityArgument.players())
									.executes(context ->
											updateMaxHealth(context.getSource(), EntityArgument.getPlayers(context, TARGETS_ARGUMENT))
									)
					)
			);
		});
	}

	private static int openJournal(CommandSourceStack commandSource) throws CommandSyntaxException {
		ServerPlayer player = commandSource.getPlayer();
		player.openMenu(new FoodJournalScreenHandler.Factory(player, FoodJournalView.getDefault()));
		return 1;
	}

	private static int clearHistory(CommandSourceStack commandSource, Collection<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			((IHungerManager) player.getFoodData()).spiceOfFabric_clearHistory();
			if (SpiceOfFabric.config.carrot.enable) {
				SpiceOfFabric.updateMaxHealth(player, true, true);
			}
			if (SpiceOfFabric.hasClientMod(player)) {
				SOFCommonNetworking.sendClearFoodsPacket(player);
				player.displayClientMessage(Component.translatable("spiceoffabric.command.clear_history.was_cleared"), false);
			} else {
				player.displayClientMessage(Component.literal("Your food history has been cleared"), false);
			}
		}

		if (commandSource.getEntity() instanceof ServerPlayer && SpiceOfFabric.hasClientMod(commandSource.getPlayer())) {
			commandSource.sendSuccess(() -> Component.translatable("spiceoffabric.command.clear_history.cleared_players", players.size()), true);
		} else {
			commandSource.sendSuccess(() -> Component.literal("Cleared food histories of " + players.size() + " players."), true);
		}
		return players.size();
	}

	private static int setBaseMaxHealth(CommandSourceStack commandSource, Collection<ServerPlayer> players, int amount) {
		for (ServerPlayer player : players) {
			AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
			//noinspection ConstantConditions
			maxHealthAttr.setBaseValue(amount);
			if (SpiceOfFabric.config.carrot.enable) {
				SpiceOfFabric.updateMaxHealth(player, true, true);
			}
			if (SpiceOfFabric.hasClientMod(player)) {
				player.displayClientMessage(Component.translatable("spiceoffabric.command.set_base_max_health.target"), false);
			} else {
				player.displayClientMessage(Component.literal("Your health has been adjusted"), false);
			}
		}

		if (commandSource.getEntity() instanceof ServerPlayer && SpiceOfFabric.hasClientMod(commandSource.getPlayer())) {
			commandSource.sendSuccess(() -> Component.translatable("spiceoffabric.command.set_base_max_health.executor", players.size(), amount, amount / 2D), false);
		} else {
			commandSource.sendSuccess(() -> Component.literal("Set base health of %d players to %d (%s hearts)".formatted(players.size(), amount, amount / 2D)), false);
		}
		return players.size();
	}

	private static int updateMaxHealth(CommandSourceStack commandSource, Collection<ServerPlayer> players) {
		boolean sourceHasMod = commandSource.getEntity() instanceof ServerPlayer && SpiceOfFabric.hasClientMod(commandSource.getPlayer());

		for (ServerPlayer player : players) {
			SpiceOfFabric.updateMaxHealth(player, true, true);
		}
		if (sourceHasMod) {
			commandSource.sendSuccess(() -> Component.translatable("spiceoffabric.command.update_max_health.success", players.size()), false);
		} else {
			commandSource.sendSuccess(() -> Component.literal("Refreshed the max health of " + players.size() + " players"), false);
		}
		return players.size();
	}
}
