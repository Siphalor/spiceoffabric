package de.siphalor.spiceoffabric.networking;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.IHungerManager;
//- import de.siphalor.tweed5.minecraft.networking.api.SlightlyCompressedByteBufWriter;
//- import io.netty.buffer.Unpooled;
//- import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ClientboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//- import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

//- import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

//- import static de.siphalor.tweed5.serde.extension.api.ReadWriteExtension.write;

public class SOFCommonNetworking {
	SOFCommonNetworking() {}

	public static void init() {
		//# if MC_VERSION_NUMBER >= 12006
		//# if MC_VERSION_NUMBER >= 260100
		var s2cRegistry = PayloadTypeRegistry.clientboundPlay();
		//# else
		//- var s2cRegistry = PayloadTypeRegistry.playS2C();
		//# end
		s2cRegistry.register(ConfigSyncS2CPacket.TYPE, ConfigSyncS2CPacket.STREAM_CODEC);
		s2cRegistry.register(SyncFoodHistoryS2CPacket.TYPE, SyncFoodHistoryS2CPacket.STREAM_CODEC);
		s2cRegistry.register(AddFoodToHistoryS2CPacket.TYPE, AddFoodToHistoryS2CPacket.STREAM_CODEC);
		s2cRegistry.register(ClearFoodHistoryS2CPacket.TYPE, ClearFoodHistoryS2CPacket.STREAM_CODEC);
		//# end

		//# if MC_VERSION_NUMBER >= 260100
		ClientboundPlayChannelEvents.REGISTER
		//# else
		//- S2CPlayChannelEvents.REGISTER
		//# end
				.register((handler, sender, server, channels) -> {
					if (channels.contains(SyncFoodHistoryS2CPacket.PAYLOAD_ID)) {
						syncFoodHistoryUnchecked(handler.player);
					}
					if (channels.contains(ConfigSyncS2CPacket.PAYLOAD_ID)) {
						syncConfigToClientUnchecked(handler.player);
					}
				});
	}

	public static boolean hasClientMod(ServerPlayer player) {
		if (player == null) {
			return false;
		}
		return ServerPlayNetworking.canSend(player, SyncFoodHistoryS2CPacket.PAYLOAD_ID);
	}

	private static void syncConfigToClientUnchecked(ServerPlayer player) {
		ConfigSyncS2CPacket packet = new ConfigSyncS2CPacket(SpiceOfFabric.config);
		//# if MC_VERSION_NUMBER >= 12006
		ServerPlayNetworking.send(player, packet);
		//# else
		//- FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		//- ConfigSyncS2CPacket.writeToBuf(buffer, packet);
		//- ServerPlayNetworking.send(player, ConfigSyncS2CPacket.PAYLOAD_ID, buffer);
		//# end
	}

	public static void syncFoodHistory(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, SyncFoodHistoryS2CPacket.PAYLOAD_ID)) {
			return;
		}

		syncFoodHistoryUnchecked(player);
	}

	private static void syncFoodHistoryUnchecked(ServerPlayer player) {
		FoodHistory foodHistory = ((IHungerManager) player.getFoodData()).spiceOfFabric_getFoodHistory();

		SyncFoodHistoryS2CPacket packet = foodHistory.toPacket();
		//# if MC_VERSION_NUMBER >= 12006
		ServerPlayNetworking.send(player, packet);
		//# else
		//- FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		//- SyncFoodHistoryS2CPacket.writeToBuf(buffer, packet);
		//- ServerPlayNetworking.send(player, SyncFoodHistoryS2CPacket.PAYLOAD_ID, buffer);
		//# end
	}

	public static void sendAddFoodPacket(ServerPlayer player, FoodHistoryEntry foodHistoryEntry) {
		if (!ServerPlayNetworking.canSend(player, AddFoodToHistoryS2CPacket.PAYLOAD_ID)) {
			return;
		}

		AddFoodToHistoryS2CPacket packet = new AddFoodToHistoryS2CPacket(foodHistoryEntry);
		//# if MC_VERSION_NUMBER >= 12006
		ServerPlayNetworking.send(player, packet);
		//# else
		//- FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		//- AddFoodToHistoryS2CPacket.writeToBuf(buffer, packet);
		//- ServerPlayNetworking.send(player, AddFoodToHistoryS2CPacket.PAYLOAD_ID, buffer);
		//# end
	}

	public static void sendClearFoodsPacket(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, ClearFoodHistoryS2CPacket.PAYLOAD_ID)) {
			return;
		}

		//# if MC_VERSION_NUMBER >= 12006
		ServerPlayNetworking.send(player, ClearFoodHistoryS2CPacket.INSTANCE);
		//# else
		//- ServerPlayNetworking.send(player, ClearFoodHistoryS2CPacket.PAYLOAD_ID, new FriendlyByteBuf(Unpooled.buffer()));
		//# end
	}
}
