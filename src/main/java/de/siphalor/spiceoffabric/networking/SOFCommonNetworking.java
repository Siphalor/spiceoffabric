package de.siphalor.spiceoffabric.networking;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.IHungerManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SOFCommonNetworking {
	SOFCommonNetworking() {}

	protected static final Identifier SYNC_FOOD_HISTORY_S2C_PACKET = new Identifier(SpiceOfFabric.MOD_ID, "sync_food_history");
	protected static final Identifier ADD_FOOD_S2C_PACKET = new Identifier(SpiceOfFabric.MOD_ID, "add_food");
	protected static final Identifier CLEAR_FOODS_S2C_PACKET = new Identifier(SpiceOfFabric.MOD_ID, "clear_foods");

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SOFCommonNetworking.syncFoodHistory(handler.player));
	}

	public static boolean hasClientMod(ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}
		return ServerPlayNetworking.canSend(player, SOFCommonNetworking.SYNC_FOOD_HISTORY_S2C_PACKET);
	}

	public static void syncFoodHistory(ServerPlayerEntity player) {
		if (!ServerPlayNetworking.canSend(player, SYNC_FOOD_HISTORY_S2C_PACKET)) {
			return;
		}

		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		((IHungerManager) player.getHungerManager()).spiceOfFabric_getFoodHistory().write(buffer);
		ServerPlayNetworking.send(player, SYNC_FOOD_HISTORY_S2C_PACKET, buffer);
	}

	public static void sendAddFoodPacket(ServerPlayerEntity player, FoodHistoryEntry foodHistoryEntry) {
		if (!ServerPlayNetworking.canSend(player, ADD_FOOD_S2C_PACKET)) {
			return;
		}

		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		foodHistoryEntry.write(buffer);
		ServerPlayNetworking.send(player, ADD_FOOD_S2C_PACKET, buffer);
	}

	public static void sendClearFoodsPacket(ServerPlayerEntity player) {
		if (!ServerPlayNetworking.canSend(player, CLEAR_FOODS_S2C_PACKET)) {
			return;
		}

		ServerPlayNetworking.send(player, CLEAR_FOODS_S2C_PACKET, new PacketByteBuf(Unpooled.buffer()));
	}
}
