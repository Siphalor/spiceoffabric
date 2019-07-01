package de.siphalor.spiceoffabric.server;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.PacketByteBuf;

import java.util.Collection;
import java.util.Collections;

public class Commands {
	public static void register() {
		//noinspection CodeBlock2Expr
		CommandRegistry.INSTANCE.register(false, commandDispatcher -> {
			commandDispatcher.register(CommandManager.literal("spiceoffabric:clearfoods")
					.requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2))
					.executes(context -> clearFoods(context.getSource(), Collections.singleton(context.getSource().getPlayer())))
				.then(CommandManager.argument("targets", EntityArgumentType.players())
					.executes(
					context -> clearFoods(context.getSource(), EntityArgumentType.getPlayers(context, "targets"))
				))
			);
		});
	}

	private static int clearFoods(ServerCommandSource commandSource, Collection<ServerPlayerEntity> serverPlayerEntities) {
		for(ServerPlayerEntity serverPlayerEntity : serverPlayerEntities) {
			((IHungerManager) serverPlayerEntity.getHungerManager()).spiceOfFabric_clearHistory();
			if(((IServerPlayerEntity) serverPlayerEntity).spiceOfFabric_hasClientMod()) {
				ServerSidePacketRegistry.INSTANCE.sendToPlayer(serverPlayerEntity, SpiceOfFabric.CLEAR_FOODS_S2C_PACKET, new PacketByteBuf(Unpooled.buffer()));
				if(Config.carrotEnabled.value)
					serverPlayerEntity.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(Config.startHearts.value * 2);
				serverPlayerEntity.sendChatMessage(new TranslatableText("spiceoffabric.command.clearfoods.was_cleared"), MessageType.CHAT);
			} else {
				serverPlayerEntity.sendChatMessage(new LiteralText("Your food history has been cleared"), MessageType.CHAT);
			}
		}

        if(commandSource.getEntity() instanceof ServerPlayerEntity && (serverPlayerEntities.size() != 1 && serverPlayerEntities.iterator().next() == commandSource.getEntity())) {
        	if(((IServerPlayerEntity) commandSource.getEntity()).spiceOfFabric_hasClientMod()) {
        		commandSource.sendFeedback(new TranslatableText("spiceoffabric.command.clearfoods.cleared_players", serverPlayerEntities.size()), true);
			} else {
        		commandSource.sendFeedback(new LiteralText("Cleared food histories of " + serverPlayerEntities.size() + " players."), true);
			}
		}
		return serverPlayerEntities.size();
	}
}
