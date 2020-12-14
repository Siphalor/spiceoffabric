package de.siphalor.spiceoffabric;

import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.server.Commands;
import de.siphalor.spiceoffabric.util.IHungerManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.HashSet;

public class SpiceOfFabric implements ModInitializer {

	public static final String MOD_ID = "spiceoffabric";
	public static final String FOOD_HISTORY_ID = "spiceOfFabric_history";
	public static final String FOOD_JOURNAL_FLAG = MOD_ID + ":food_journal";
	public static final Identifier MOD_PRESENT_C2S_PACKET = new Identifier(MOD_ID, "client_mod_present");
	public static final Identifier SYNC_FOOD_HISTORY_S2C_PACKET = new Identifier(MOD_ID, "sync_food_history");
	public static final Identifier ADD_FOOD_S2C_PACKET = new Identifier(MOD_ID, "add_food");
	public static final Identifier CLEAR_FOODS_S2C_PACKET = new Identifier(MOD_ID, "clear_foods");

	@Override
	public void onInitialize() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			syncFoodHistory(handler.player);
		});

		Commands.register();
	}

	public static void onEaten(ServerPlayerEntity player, FoodHistory foodHistory, ItemStack stack) {
		foodHistory.addFood(stack, player);
		player.networkHandler.sendPacket(new HealthUpdateS2CPacket(player.getHealth(), player.getHungerManager().getFoodLevel(), player.getHungerManager().getSaturationLevel()));
		int health = (int) player.getMaxHealth() / 2;
		if (Config.carrot.enable && (health < Config.carrot.maxHearts || Config.carrot.maxHearts == -1)) {
			if (foodHistory.carrotHistory == null)
				foodHistory.carrotHistory = new HashSet<>();
			Config.setHeartUnlockExpressionValues(foodHistory.carrotHistory.size(), health);
			boolean changed = false;
			int loops = 0;
			while (foodHistory.carrotHistory.size() >= Config.heartUnlockExpression.evaluate()) {
				Config.heartUnlockExpression.setVariable("heartAmount", ++health);
				changed = true;
				if (++loops > 50) {
					System.err.println("Spice of Fabric health gain overflowed. This might be due to an incorrect formula.");
					return;
				}
			}
			if (changed) {
				player.world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
				if (Config.carrot.maxHearts != -1)
					health = Math.min(health, Config.carrot.maxHearts);
				player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(2 * health);
			}
		}
	}

	public static void syncFoodHistory(ServerPlayerEntity serverPlayerEntity) {
		if (ServerPlayNetworking.canSend(serverPlayerEntity, SYNC_FOOD_HISTORY_S2C_PACKET)) {
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			((IHungerManager) serverPlayerEntity.getHungerManager()).spiceOfFabric_getFoodHistory().write(buffer);
			ServerPlayNetworking.send(serverPlayerEntity, SYNC_FOOD_HISTORY_S2C_PACKET, buffer);
		}
	}
}
