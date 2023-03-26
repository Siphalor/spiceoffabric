package de.siphalor.spiceoffabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import de.siphalor.capsaicin.api.food.FoodContext;
import de.siphalor.capsaicin.api.food.FoodEvents;
import de.siphalor.capsaicin.api.food.FoodModifications;
import de.siphalor.capsaicin.api.food.PlayerFoodModifier;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.item.FoodContainerItem;
import de.siphalor.spiceoffabric.polymer.SoFPolymer;
import de.siphalor.spiceoffabric.recipe.FoodJournalRecipeSerializer;
import de.siphalor.spiceoffabric.server.Commands;
import de.siphalor.spiceoffabric.util.FoodUtils;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.tweed4.Tweed;
import de.siphalor.tweed4.config.ConfigEnvironment;
import de.siphalor.tweed4.config.ConfigLoader;
import de.siphalor.tweed4.config.TweedRegistry;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

	public static final Logger LOGGER = LoggerFactory.getLogger(SpiceOfFabric.class);
	private static final FoodComponent EMPTY_FOOD_COMPONENT = new FoodComponent.Builder().build();

	public static Item[] foodContainerItems;

	@Override
	public void onInitialize() {
		Tweed.runEntryPoints();
		FabricLoader loader = FabricLoader.getInstance();
		ConfigLoader.initialReload(
				TweedRegistry.getConfigFile(MOD_ID),
				loader.getEnvironmentType() == EnvType.SERVER ? ConfigEnvironment.SERVER : ConfigEnvironment.UNIVERSAL
		);

		Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(MOD_ID, "food_journal"), new FoodJournalRecipeSerializer());

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncFoodHistory(handler.player));

		Commands.register();

		// FOOD EVENTS

		FoodEvents.EATEN.on(event -> {
			FoodContext context = event.context();
			if (context.user() instanceof ServerPlayerEntity player) {
				FoodHistory foodHistory = FoodHistory.get(player);
				ItemStack foodStack = FoodUtils.getFoodStack(context);
				foodHistory.addFood(foodStack, player);
				if (Config.carrot.enable && (player.getMaxHealth() < Config.carrot.maxHealth || Config.carrot.maxHealth == -1)) {
					SpiceOfFabric.updateMaxHealth(player, true, true);
				}
			}
		});

		FoodModifications.EATING_TIME_MODIFIERS.register((PlayerFoodModifier<Integer>) (eatingTime, context, player) -> {
			FoodHistory foodHistory = FoodHistory.get(player);
			if (foodHistory == null) {
				return eatingTime;
			}

			Config.setConsumeDurationValues(foodHistory.getTimesEaten(FoodUtils.getFoodStack(context)), context.originalFoodHunger(), context.originalFoodSaturationModifier(), eatingTime);
			return (int) Config.consumeDurationExpression.evaluate();
		}, new Identifier(MOD_ID, "config_expression"));

		FoodModifications.PROPERTIES_MODIFIERS.register((foodProperties, context) -> {
			int timesEaten;
			if (context.user() instanceof PlayerEntity player) {
				FoodHistory foodHistory = FoodHistory.get(player);
				if (foodHistory != null) {
					timesEaten = foodHistory.getTimesEaten(FoodUtils.getFoodStack(context));
				} else {
					timesEaten = 0;
				}
			} else {
				timesEaten = 0;
			}

			Config.setHungerExpressionValues(timesEaten, foodProperties.getHunger(), foodProperties.getSaturationModifier(), 0);
			foodProperties.setHunger(Config.getHungerValue());
			foodProperties.setSaturationModifier(Config.getSaturationValue());
			return foodProperties;
		}, new Identifier(MOD_ID, "config_expression"));

		// POLYMER AND FOOD CONTAINERS

		if (Config.items.usePolymer) {
			if (!FabricLoader.getInstance().isModLoaded("polymer")) {
				LOGGER.error("Polymer is not installed, but Polymer usage is enabled in the Spice of Fabric config!");
				System.exit(1);
			}
			SoFPolymer.init();
		} else {
			List<Item> foodContainerItems = new ArrayList<>(4);
			if (Config.items.enablePaperBag) {
				foodContainerItems.add(Registry.register(
						Registries.ITEM, new Identifier(MOD_ID, "paper_bag"),
						new FoodContainerItem(new Item.Settings().maxCount(1).food(EMPTY_FOOD_COMPONENT), 5, ScreenHandlerType.HOPPER)
				));
			}
			if (Config.items.enableLunchBox) {
				foodContainerItems.add(Registry.register(
						Registries.ITEM, new Identifier(MOD_ID, "lunch_box"),
						new FoodContainerItem(new Item.Settings().maxCount(1).food(EMPTY_FOOD_COMPONENT), 9, ScreenHandlerType.GENERIC_3X3)
				));
			}
			if (Config.items.enablePicnicBasket) {
				foodContainerItems.add(Registry.register(
						Registries.ITEM, new Identifier(MOD_ID, "picnic_basket"),
						new FoodContainerItem(new Item.Settings().maxCount(1).food(EMPTY_FOOD_COMPONENT), 9, ScreenHandlerType.GENERIC_3X3)
				));
			}
			SpiceOfFabric.foodContainerItems = foodContainerItems.toArray(new Item[0]);
		}

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
			entries.add(createFoodJournalStack());
			for (Item item : foodContainerItems) {
				entries.add(item);
			}
		});

		ResourceConditions.register(new Identifier(MOD_ID, "registry_populated"), optionsJson -> {
			Identifier id = new Identifier(JsonHelper.getString(optionsJson, "registry"));
			Registry<?> registry = Registries.REGISTRIES.get(id);
			if (registry == null) {
				throw new JsonSyntaxException(id + " is not a valid registry!");
			}
			for (JsonElement elementJson : JsonHelper.getArray(optionsJson, "ids")) {
				Identifier elementId = new Identifier(JsonHelper.asString(elementJson, "id"));
				if (!registry.containsId(elementId)) {
					return false;
				}
			}
			return true;
		});
	}

	public static boolean hasMod(ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}
		return ServerPlayNetworking.canSend(player, SYNC_FOOD_HISTORY_S2C_PACKET);
	}

	public static EntityAttributeModifier createHealthModifier(double amount) {
		return new EntityAttributeModifier(
				PLAYER_HEALTH_MODIFIER_UUID,
				MOD_ID,
				amount,
				EntityAttributeModifier.Operation.ADDITION
		);
	}

	public static void updateMaxHealth(ServerPlayerEntity player, boolean sync, boolean announce) {
		EntityAttributeInstance maxHealthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		double oldValue = maxHealthAttr.getValue();
		maxHealthAttr.removeModifier(PLAYER_HEALTH_MODIFIER_UUID);

		if (Config.carrot.enable) {
			FoodHistory foodHistory = ((IHungerManager) player.getHungerManager()).spiceOfFabric_getFoodHistory();
			maxHealthAttr.addPersistentModifier(createHealthModifier(foodHistory.getCarrotHealthOffset(player)));
		}

		if (sync) {
			player.networkHandler.sendPacket(new EntityAttributesS2CPacket(player.getId(), Collections.singleton(maxHealthAttr)));
			player.networkHandler.sendPacket(new HealthUpdateS2CPacket(player.getHealth(), player.getHungerManager().getFoodLevel(), player.getHungerManager().getSaturationLevel()));
		}
		if (announce && maxHealthAttr.getValue() > oldValue) {
			player.world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1F, 1F);
		}
	}

	public static void syncFoodHistory(ServerPlayerEntity serverPlayerEntity) {
		if (ServerPlayNetworking.canSend(serverPlayerEntity, SYNC_FOOD_HISTORY_S2C_PACKET)) {
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			((IHungerManager) serverPlayerEntity.getHungerManager()).spiceOfFabric_getFoodHistory().write(buffer);
			ServerPlayNetworking.send(serverPlayerEntity, SYNC_FOOD_HISTORY_S2C_PACKET, buffer);
		}
	}

	public static ItemStack createFoodJournalStack() {
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		NbtCompound compound = stack.getOrCreateNbt();
		compound.putString("title", "");
		compound.putString("author", "Me");
		compound.putBoolean(SpiceOfFabric.FOOD_JOURNAL_FLAG, true);
		stack.getOrCreateSubNbt("display").putString("Name", "{\"translate\":\"Diet Journal\",\"bold\":true}");
		return stack;
	}
}
