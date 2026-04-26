package de.siphalor.spiceoffabric.networking;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.tweed5.core.api.container.ConfigContainer;
import de.siphalor.tweed5.defaultextensions.patch.api.PatchExtension;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

@Slf4j
public class SOFClientNetworking {
	private static SyncFoodHistoryS2CPacket lastFoodHistorySyncPacket;

	private SOFClientNetworking() {}

	public static void init() {
		ClientPlayConnectionEvents.JOIN.register(SOFClientNetworking::onJoined);
		//# if MC_VERSION_NUMBER >= 12006
		ClientPlayNetworking.registerGlobalReceiver(ConfigSyncS2CPacket.TYPE, (packet, context) ->
			onConfigSyncPacketReceived(packet)
		);
		ClientPlayNetworking.registerGlobalReceiver(SyncFoodHistoryS2CPacket.TYPE, (packet, context) ->
			onFoodHistorySyncPacketReceived(packet, context.client())
		);
		ClientPlayNetworking.registerGlobalReceiver(AddFoodToHistoryS2CPacket.TYPE, (packet, context) ->
			onAddFoodPacketReceived(packet, context.client())
		);
		ClientPlayNetworking.registerGlobalReceiver(ClearFoodHistoryS2CPacket.TYPE, (packet, context) ->
			onClearFoodPackedReceived(context.client())
		);
		//# else
		//- ClientPlayNetworking.registerGlobalReceiver(ConfigSyncS2CPacket.PAYLOAD_ID, (client, handler, buf, responseSender) ->
		//- 	onConfigSyncPacketReceived(ConfigSyncS2CPacket.readFromBuf(buf))
		//- );
		//- ClientPlayNetworking.registerGlobalReceiver(SyncFoodHistoryS2CPacket.PAYLOAD_ID, (client, handler, buf, responseSender) ->
		//- 	onFoodHistorySyncPacketReceived(SyncFoodHistoryS2CPacket.readFromBuf(buf), client)
		//- );
		//- ClientPlayNetworking.registerGlobalReceiver(AddFoodToHistoryS2CPacket.PAYLOAD_ID, (client, handler, buf, responseSender) ->
		//- 	onAddFoodPacketReceived(AddFoodToHistoryS2CPacket.readFromBuf(buf), client)
		//- );
		//- ClientPlayNetworking.registerGlobalReceiver(ClearFoodHistoryS2CPacket.PAYLOAD_ID, (client, handler, buf, responseSender) ->
		//- 	onClearFoodPackedReceived(client)
		//- );
		//# end
	}

	private static void onJoined(ClientPacketListener handler, PacketSender sender, Minecraft client) {
		assert client.player != null;
		if (lastFoodHistorySyncPacket != null) {
			((IHungerManager) client.player.getFoodData()).spiceOfFabric_getFoodHistory()
					.applyPacket(lastFoodHistorySyncPacket);
			lastFoodHistorySyncPacket = null;
		}
	}

	private static void onConfigSyncPacketReceived(ConfigSyncS2CPacket packet) {
		if (packet.getConfig() == null) {
			return;
		}

		ConfigContainer<SOFConfig> configContainer = SpiceOfFabric.configContainerHelper.configContainer();
		PatchExtension patchExtension = configContainer.extension(PatchExtension.class).orElseThrow();

		SpiceOfFabric.config = configContainer.rootEntry().deepCopy(SpiceOfFabric.globalConfig);
		patchExtension.patch(configContainer.rootEntry(), SpiceOfFabric.config, packet.getConfig(), packet.getPatchInfo());
	}

	private static void onFoodHistorySyncPacketReceived(SyncFoodHistoryS2CPacket packet, Minecraft client) {
		if (client.player != null && client.player.getFoodData() != null) {
			((IHungerManager) client.player.getFoodData()).spiceOfFabric_getFoodHistory().applyPacket(packet);
		} else {
			lastFoodHistorySyncPacket = packet;
		}
	}

	private static void onAddFoodPacketReceived(AddFoodToHistoryS2CPacket packet, Minecraft client) {
		((IHungerManager) client.player.getFoodData()).spiceOfFabric_getFoodHistory().addFood(packet.getEntry());
	}

	private static void onClearFoodPackedReceived(Minecraft client) {
		((IHungerManager) client.player.getFoodData()).spiceOfFabric_clearHistory();
	}
}
