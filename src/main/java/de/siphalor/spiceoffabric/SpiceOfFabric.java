package de.siphalor.spiceoffabric;

import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.server.Commands;
import de.siphalor.spiceoffabric.util.IHungerManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SpiceOfFabric implements ModInitializer {

	public static final String MOD_ID = "spiceoffabric";
	public static final String NBT_FOOD_HISTORY_ID = "spiceOfFabric_history";
	public static final String NBT_VERSION_ID = "spiceOfFabric_version";
	public static final int NBT_VERSION = 1;
	public static final String FOOD_JOURNAL_FLAG = MOD_ID + ":food_journal";
	public static final Identifier SYNC_FOOD_HISTORY_S2C_PACKET = new Identifier(MOD_ID, "sync_food_history");
	public static final Identifier ADD_FOOD_S2C_PACKET = new Identifier(MOD_ID, "add_food");
	public static final Identifier CLEAR_FOODS_S2C_PACKET = new Identifier(MOD_ID, "clear_foods");

	public static final UUID PLAYER_HEALTH_MODIFIER_UUID = UUID.nameUUIDFromBytes(MOD_ID.getBytes(StandardCharsets.UTF_8));

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
		if (Config.carrot.enable && (player.getMaxHealth() < Config.carrot.maxHealth || Config.carrot.maxHealth == -1)) {
			SpiceOfFabric.updateMaxHealth(player);
		}
	}

	public static EntityAttributeModifier createHealthModifier(double amount) {
		return new EntityAttributeModifier(
				PLAYER_HEALTH_MODIFIER_UUID,
				MOD_ID,
				amount,
				EntityAttributeModifier.Operation.ADDITION
		);
	}

	public static void updateMaxHealth(PlayerEntity player) {
		EntityAttributeInstance maxHealthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		maxHealthAttr.removeModifier(PLAYER_HEALTH_MODIFIER_UUID);
		FoodHistory foodHistory = ((IHungerManager) player.getHungerManager()).spiceOfFabric_getFoodHistory();
		maxHealthAttr.addPersistentModifier(createHealthModifier(foodHistory.getCarrotHealthOffset(player)));
	}

	public static void syncFoodHistory(ServerPlayerEntity serverPlayerEntity) {
		if (ServerPlayNetworking.canSend(serverPlayerEntity, SYNC_FOOD_HISTORY_S2C_PACKET)) {
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			((IHungerManager) serverPlayerEntity.getHungerManager()).spiceOfFabric_getFoodHistory().write(buffer);
			ServerPlayNetworking.send(serverPlayerEntity, SYNC_FOOD_HISTORY_S2C_PACKET, buffer);
		}
	}
}
