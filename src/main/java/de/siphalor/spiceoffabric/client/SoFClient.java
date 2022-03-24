package de.siphalor.spiceoffabric.client;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;

public class SoFClient implements ClientModInitializer {
	private static final String LAST_EATEN_BASE_TRANSLATION_KEY = SpiceOfFabric.MOD_ID + ".item.tooltip.last_eaten";
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

		ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
			if (Config.showLastEatenTips != Config.ITEM_TIP_DISPLAY.NONE && Config.food.historyLength > 0) {
				ClientPlayerEntity player = MinecraftClient.getInstance().player;
				if (player == null) {
					return;
				}
				IHungerManager hungerManager = (IHungerManager) player.getHungerManager();
				if (hungerManager == null) {
					return;
				}
				FoodHistory foodHistory = hungerManager.spiceOfFabric_getFoodHistory();
				int lastEaten = foodHistory.getLastEaten(stack);
				if (lastEaten < 0) {
					return;
				}

				BaseText text;
				if (lastEaten == 0) {
					text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".simple.last", lastEaten);
				} else if (lastEaten == 1) {
					text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".simple.one", lastEaten);
				} else {
					text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".simple", lastEaten);
				}

				if (Config.showLastEatenTips == Config.ITEM_TIP_DISPLAY.EXTENDED) {
					int left = Config.food.historyLength - lastEaten;
					if (left == 1) {
						text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".extended.one", text, Config.food.historyLength - lastEaten);
					} else {
						text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".extended", text, Config.food.historyLength - lastEaten);
					}
				}

				for (String line : StringUtils.split(text.getString(), '\n')) {
					lines.add(new LiteralText(line).styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));
				}
			}
		});
	}
}
