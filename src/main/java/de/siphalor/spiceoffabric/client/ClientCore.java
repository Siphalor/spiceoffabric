package de.siphalor.spiceoffabric.client;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;

public class ClientCore implements ClientModInitializer {
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
	}
}
