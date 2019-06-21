package de.siphalor.spiceoffabric.client;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.client.MinecraftClient;

public class ClientCore implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientSidePacketRegistry.INSTANCE.register(SpiceOfFabric.SYNC_FOOD_HISTORY_S2C_PACKET, (packetContext, packetByteBuf) -> ((IHungerManager) MinecraftClient.getInstance().player.getHungerManager()).spiceOfFabric_getFoodHistory().read(packetByteBuf));
		ClientSidePacketRegistry.INSTANCE.register(SpiceOfFabric.ADD_FOOD_S2C_PACKET, (packetContext, packetByteBuf) -> ((IHungerManager) packetContext.getPlayer().getHungerManager()).spiceOfFabric_getFoodHistory().addFood(FoodHistoryEntry.from(packetByteBuf)));
	}
}
