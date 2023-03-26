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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class SoFClient implements ClientModInitializer {
	private static PacketByteBuf lastSyncPacket;

	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (lastSyncPacket != null) {
				((IHungerManager) client.player.getHungerManager()).spiceOfFabric_getFoodHistory().read(lastSyncPacket);
				lastSyncPacket.release();
				lastSyncPacket = null;
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(SpiceOfFabric.SYNC_FOOD_HISTORY_S2C_PACKET, (client, handler, buf, responseSender) -> {
			if (client.player != null && client.player.getHungerManager() != null) {
				((IHungerManager) client.player.getHungerManager()).spiceOfFabric_getFoodHistory().read(buf);
			} else {
				lastSyncPacket = new PacketByteBuf(buf.copy());
				lastSyncPacket.retain();
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(SpiceOfFabric.ADD_FOOD_S2C_PACKET, (client, handler, buf, responseSender) ->
				((IHungerManager) client.player.getHungerManager()).spiceOfFabric_getFoodHistory().addFood(FoodHistoryEntry.from(buf))
		);
		ClientPlayNetworking.registerGlobalReceiver(SpiceOfFabric.CLEAR_FOODS_S2C_PACKET, (client, handler, buf, responseSender) ->
				((IHungerManager) client.player.getHungerManager()).spiceOfFabric_clearHistory()
		);

		if (!Config.items.usePolymer) {
			if (SpiceOfFabric.foodContainerItems != null) {
				ClampedModelPredicateProvider predicateProvider = (stack, world, entity, seed) ->
						((FoodContainerItem) stack.getItem()).isInventoryEmpty(stack) ? 0 : 1;
				Identifier predicateId = new Identifier(SpiceOfFabric.MOD_ID, "filled");
				for (Item item : SpiceOfFabric.foodContainerItems) {
					ModelPredicateProviderRegistry.register(item, predicateId, predicateProvider);
				}
			}
		}

		ItemTooltipCallback.EVENT.register((stack, context, lines) ->
				lines.addAll(1, FoodUtils.getClientTooltipAdditions(MinecraftClient.getInstance().player, stack)));
	}
}
