package de.siphalor.spiceoffabric.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.container.FoodJournalScreenHandler;
import de.siphalor.spiceoffabric.container.FoodJournalView;
import de.siphalor.spiceoffabric.networking.SOFCommonNetworking;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.Collection;
import java.util.Collections;

public class SOFCommands {
	private static final String AMOUNT_ARGUMENT = "amount";
	private static final String TARGETS_ARGUMENT = "targets";

	private SOFCommands() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((commandDispatcher, dedicated) -> {
			if (SOFConfig.enableJournalCommand) {
				commandDispatcher.register(CommandManager.literal(SpiceOfFabric.MOD_ID + ":journal")
						.executes(context -> openJournal(context.getSource())));
			}

			commandDispatcher.register(CommandManager.literal(SpiceOfFabric.MOD_ID + ":clear_history")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(context ->
							clearHistory(
									context.getSource(),
									Collections.singleton(context.getSource().getPlayer())
							)
					).then(
							CommandManager.argument(TARGETS_ARGUMENT, EntityArgumentType.players())
									.executes(context ->
											clearHistory(
													context.getSource(),
													EntityArgumentType.getPlayers(context, TARGETS_ARGUMENT)
											)
									)
					)
			);
			commandDispatcher.register(CommandManager.literal(SpiceOfFabric.MOD_ID + ":set_base_max_health")
					.requires(source -> source.hasPermissionLevel(2))
					.then(
							CommandManager.argument(TARGETS_ARGUMENT, EntityArgumentType.players())
									.then(
											CommandManager.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, 200))
													.executes(context ->
															setBaseMaxHealth(
																	context.getSource(),
																	EntityArgumentType.getPlayers(context, TARGETS_ARGUMENT),
																	IntegerArgumentType.getInteger(context, AMOUNT_ARGUMENT)
															)
													)
									)
					)
					.then(
							CommandManager.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, 200))
									.executes(context ->
											setBaseMaxHealth(
													context.getSource(),
													Collections.singleton(context.getSource().getPlayer()),
													IntegerArgumentType.getInteger(context, AMOUNT_ARGUMENT)
											)
									)
					)
			);
			commandDispatcher.register(CommandManager.literal(SpiceOfFabric.MOD_ID + ":update_max_health")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(context ->
							updateMaxHealth(context.getSource(), Collections.singleton(context.getSource().getPlayer()))
					).then(
							CommandManager.argument(TARGETS_ARGUMENT, EntityArgumentType.players())
									.executes(context ->
											updateMaxHealth(context.getSource(), EntityArgumentType.getPlayers(context, TARGETS_ARGUMENT))
									)
					)
			);
		});
	}

	private static int openJournal(ServerCommandSource commandSource) throws CommandSyntaxException {
		ServerPlayerEntity player = commandSource.getPlayer();
		player.openHandledScreen(new FoodJournalScreenHandler.Factory(player, FoodJournalView.getDefault()));
		return 1;
	}

	private static int clearHistory(ServerCommandSource commandSource, Collection<ServerPlayerEntity> players) {
		for (ServerPlayerEntity player : players) {
			((IHungerManager) player.getHungerManager()).spiceOfFabric_clearHistory();
			if (SOFConfig.carrot.enable) {
				SpiceOfFabric.updateMaxHealth(player, true, true);
			}
			if (SpiceOfFabric.hasClientMod(player)) {
				SOFCommonNetworking.sendClearFoodsPacket(player);
				player.sendMessage(new TranslatableText("spiceoffabric.command.clear_history.was_cleared"), false);
			} else {
				player.sendMessage(new LiteralText("Your food history has been cleared"), false);
			}
		}

		try {
			if (commandSource.getEntity() instanceof ServerPlayerEntity && SpiceOfFabric.hasClientMod(commandSource.getPlayer())) {
				commandSource.sendFeedback(new TranslatableText("spiceoffabric.command.clear_history.cleared_players", players.size()), true);
			} else {
				commandSource.sendFeedback(new LiteralText("Cleared food histories of " + players.size() + " players."), true);
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
		return players.size();
	}

	private static int setBaseMaxHealth(ServerCommandSource commandSource, Collection<ServerPlayerEntity> players, int amount) {
		for (ServerPlayerEntity player : players) {
			EntityAttributeInstance maxHealthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
			//noinspection ConstantConditions
			maxHealthAttr.setBaseValue(amount);
			if (SOFConfig.carrot.enable) {
				SpiceOfFabric.updateMaxHealth(player, true, true);
			}
			if (SpiceOfFabric.hasClientMod(player)) {
				player.sendMessage(new TranslatableText("spiceoffabric.command.set_base_max_health.target"), false);
			} else {
				player.sendMessage(new LiteralText("Your health has been adjusted"), false);
			}
		}

		try {
			if (commandSource.getEntity() instanceof ServerPlayerEntity && SpiceOfFabric.hasClientMod(commandSource.getPlayer())) {
				commandSource.sendFeedback(new TranslatableText("spiceoffabric.command.set_base_max_health.executor", players.size(), amount, amount / 2D), false);
			} else {
				commandSource.sendFeedback(new LiteralText("Set base health of %d players to %d (%s hearts)".formatted(players.size(), amount, amount / 2D)), false);
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
		return players.size();
	}

	private static int updateMaxHealth(ServerCommandSource commandSource, Collection<ServerPlayerEntity> players) {
		boolean sourceHasMod;
		try {
			sourceHasMod = commandSource.getEntity() instanceof ServerPlayerEntity && SpiceOfFabric.hasClientMod(commandSource.getPlayer());
		} catch (CommandSyntaxException e) {
			sourceHasMod = false;
		}

		for (ServerPlayerEntity player : players) {
			SpiceOfFabric.updateMaxHealth(player, true, true);
		}
		if (sourceHasMod) {
			commandSource.sendFeedback(new TranslatableText("spiceoffabric.command.update_max_health.success", players.size()), false);
		} else {
			commandSource.sendFeedback(new LiteralText("Refreshed the max health of " + players.size() + " players"), false);
		}
		return players.size();
	}
}
