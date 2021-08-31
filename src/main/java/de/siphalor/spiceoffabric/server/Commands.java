package de.siphalor.spiceoffabric.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.IHungerManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.Collection;
import java.util.Collections;

public class Commands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((commandDispatcher, dedicated) -> {
			commandDispatcher.register(CommandManager.literal(SpiceOfFabric.MOD_ID + ":clear_history")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(context ->
							clearHistory(
									context.getSource(),
									Collections.singleton(context.getSource().getPlayer())
							)
					).then(
							CommandManager.argument("targets", EntityArgumentType.players())
							.executes(context ->
									clearHistory(
											context.getSource(),
											EntityArgumentType.getPlayers(context, "targets")
									)
							)
					)
			);
		});
	}

	private static int clearHistory(ServerCommandSource commandSource, Collection<ServerPlayerEntity> players) {
		for(ServerPlayerEntity player : players) {
			((IHungerManager) player.getHungerManager()).spiceOfFabric_clearHistory();
			if (Config.carrot.enable) {
				SpiceOfFabric.updateMaxHealth(player, true, true);
			}
			if (ServerPlayNetworking.canSend(player, SpiceOfFabric.CLEAR_FOODS_S2C_PACKET)) {
				ServerPlayNetworking.send(player, SpiceOfFabric.CLEAR_FOODS_S2C_PACKET, new PacketByteBuf(Unpooled.buffer()));
				player.sendMessage(new TranslatableText("spiceoffabric.command.clear_history.was_cleared"), false);
			} else {
				player.sendMessage(new LiteralText("Your food history has been cleared"), false);
			}
		}

		try {
			if (commandSource.getEntity() instanceof ServerPlayerEntity && SpiceOfFabric.hasMod(commandSource.getPlayer())) {
				commandSource.sendFeedback(new TranslatableText("spiceoffabric.command.clear_history.cleared_players", players.size()), true);
			} else {
				commandSource.sendFeedback(new LiteralText("Cleared food histories of " + players.size() + " players."), true);
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
		return players.size();
	}
}
