package de.siphalor.spiceoffabric.client;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.item.FoodContainerItem;
import de.siphalor.spiceoffabric.util.FoodUtils;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class SoFClient implements ClientModInitializer {
	private static PacketByteBuf lastSyncPacket;

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register(SoFClient::onJoined);
		ClientPlayNetworking.registerGlobalReceiver(SpiceOfFabric.SYNC_FOOD_HISTORY_S2C_PACKET, SoFClient::onFoodHistorySyncPacketReceived);
		ClientPlayNetworking.registerGlobalReceiver(SpiceOfFabric.ADD_FOOD_S2C_PACKET, SoFClient::onAddFoodPacketReceived);
		ClientPlayNetworking.registerGlobalReceiver(SpiceOfFabric.CLEAR_FOODS_S2C_PACKET, SoFClient::onClearFoodPackedReceived);

		ItemTooltipCallback.EVENT.register(SoFClient::itemTooltipCallback);

		if (!Config.items.usePolymer && SpiceOfFabric.foodContainerItems != null) {
			registerModelPredicateProviders();
		}
	}

	private static void onJoined(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
		if (lastSyncPacket != null) {
			((IHungerManager) client.player.getHungerManager()).spiceOfFabric_getFoodHistory().read(lastSyncPacket);
			lastSyncPacket.release();
			lastSyncPacket = null;
		}
	}

	private static void onFoodHistorySyncPacketReceived(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
		if (client.player != null && client.player.getHungerManager() != null) {
			((IHungerManager) client.player.getHungerManager()).spiceOfFabric_getFoodHistory().read(buf);
		} else {
			lastSyncPacket = new PacketByteBuf(buf.copy());
			lastSyncPacket.retain();
		}
	}

	private static void onAddFoodPacketReceived(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
		((IHungerManager) client.player.getHungerManager()).spiceOfFabric_getFoodHistory().addFood(FoodHistoryEntry.from(buf));
	}

	private static void onClearFoodPackedReceived(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
		((IHungerManager) client.player.getHungerManager()).spiceOfFabric_clearHistory();
	}

	private static void itemTooltipCallback(ItemStack stack, TooltipContext context, List<Text> lines) {
		lines.addAll(1, FoodUtils.getClientTooltipAdditions(MinecraftClient.getInstance().player, stack));
	}

	private static void registerModelPredicateProviders() {
		ClampedModelPredicateProvider predicateProvider = (stack, world, entity, seed) ->
				((FoodContainerItem) stack.getItem()).isInventoryEmpty(stack) ? 0 : 1;
		Identifier predicateId = new Identifier(SpiceOfFabric.MOD_ID, "filled");
		for (Item item : SpiceOfFabric.foodContainerItems) {
			ModelPredicateProviderRegistry.register(item, predicateId, predicateProvider);
		}
	}
}
