package de.siphalor.spiceoffabric.networking;

import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

public class SOFClientNetworking extends SOFCommonNetworking {
	private static PacketByteBuf lastSyncPacket;

	private SOFClientNetworking() {}

	public static void init() {
		ClientPlayConnectionEvents.JOIN.register(SOFClientNetworking::onJoined);
		ClientPlayNetworking.registerGlobalReceiver(SYNC_FOOD_HISTORY_S2C_PACKET, SOFClientNetworking::onFoodHistorySyncPacketReceived);
		ClientPlayNetworking.registerGlobalReceiver(ADD_FOOD_S2C_PACKET, SOFClientNetworking::onAddFoodPacketReceived);
		ClientPlayNetworking.registerGlobalReceiver(CLEAR_FOODS_S2C_PACKET, SOFClientNetworking::onClearFoodPackedReceived);
	}

	private static void onJoined(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
		assert client.player != null;
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

}
