package de.siphalor.spiceoffabric;

//- import com.google.gson.JsonElement;
//- import com.google.gson.JsonSyntaxException;
//- import com.mojang.serialization.Codec;
//- import com.mojang.serialization.MapCodec;
//- import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.siphalor.capsaicin.api.food.FoodContext;
import de.siphalor.capsaicin.api.food.FoodEvents;
import de.siphalor.capsaicin.api.food.FoodModifications;
import de.siphalor.capsaicin.api.food.PlayerFoodModifier;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.config.SOFTweedAttributes;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.item.FoodContainerItem;
import de.siphalor.spiceoffabric.networking.SOFCommonNetworking;
import de.siphalor.spiceoffabric.polymer.SOFPolymer;
import de.siphalor.spiceoffabric.recipe.FoodJournalRecipeSerializer;
import de.siphalor.spiceoffabric.resource_conditions.SOFResourceConditions;
import de.siphalor.spiceoffabric.server.SOFCommands;
import de.siphalor.spiceoffabric.util.FoodUtils;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.tweed5.attributesextension.api.serde.filter.AttributesReadWriteFilterExtension;
import de.siphalor.tweed5.coat.bridge.api.TweedCoatBridgeExtension;
import de.siphalor.tweed5.core.api.container.ConfigContainer;
import de.siphalor.tweed5.fabric.helper.api.FabricConfigCommentLoader;
import de.siphalor.tweed5.fabric.helper.api.FabricConfigContainerHelper;
import de.siphalor.tweed5.serde.hjson.HjsonCommentType;
import de.siphalor.tweed5.serde.hjson.HjsonSerde;
import de.siphalor.tweed5.serde.hjson.HjsonWriter;
import de.siphalor.tweed5.weaver.pojo.api.TweedPojoWeaver;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
//- import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
//- import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
//- import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
//- import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.network.FilteredText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
//- import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SpiceOfFabric implements ModInitializer {

	public static final String MOD_ID = "spiceoffabric";
	public static final String MOD_NAME = "Spice of Fabric";

	public static final String NBT_FOOD_HISTORY_ID = "spiceOfFabric_history";
	public static final String NBT_VERSION_ID = "spiceOfFabric_version";
	public static final int NBT_VERSION = 1;
	public static final String FOOD_JOURNAL_FLAG = MOD_ID + ":food_journal";

	//# if MC_VERSION_NUMBER >= 12100
	public static final ResourceLocation PLAYER_HEALTH_MODIFIER_ID = createId("main");
	//# end
	public static final UUID PLAYER_HEALTH_MODIFIER_UUID = UUID.nameUUIDFromBytes(MOD_ID.getBytes(StandardCharsets.UTF_8));

	public static final Logger LOGGER = LoggerFactory.getLogger(SpiceOfFabric.class);
	private static final FoodProperties EMPTY_FOOD_COMPONENT = new FoodProperties.Builder().build();

	public static FabricConfigContainerHelper<SOFConfig> configContainerHelper;
	public static SOFConfig globalConfig;
	public static SOFConfig config;

	public static Item[] foodContainerItems = {};

	@Override
	public void onInitialize() {
		initConfig();
		loadGlobalConfig();

		SOFCommands.register();

		SOFResourceConditions.init();

		initRecipes();

		initFoodEvents();

		if (config.items.usePolymer) {
			if (!FabricLoader.getInstance().isModLoaded("polymer-bundled")) {
				LOGGER.error("Polymer is not installed, but Polymer usage is enabled in the Spice of Fabric config!");
				System.exit(1);
			}
			SOFPolymer.init();
		} else {
			initNativeFoodContainerItems();
		}

		initItemGroups();
	}

	private static void initConfig() {
		TweedPojoWeaver<SOFConfig> weaver = TweedPojoWeaver.forClass(SOFConfig.class);
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			weaver.withExtension(TweedCoatBridgeExtension.class);
		}

		ConfigContainer<SOFConfig> configContainer = weaver.weave();
		FabricConfigCommentLoader.builder()
				.configContainer(configContainer)
				.modId(MOD_ID)
				.prefix(MOD_ID + ".config")
				.suffix(".description")
				.build()
				.loadCommentsFromLanguageFile("en_us");
		AttributesReadWriteFilterExtension filterExtension = configContainer
				.extension(AttributesReadWriteFilterExtension.class)
				.orElseThrow(IllegalStateException::new);
		filterExtension.markAttributeForFiltering(SOFTweedAttributes.SCOPE);
		filterExtension.markAttributeForFiltering(SOFTweedAttributes.SYNCED);

		configContainer.initialize();
		configContainerHelper = FabricConfigContainerHelper.create(
				configContainer,
				new HjsonSerde(
						new HjsonWriter.Options()
								.multilineCommentType(HjsonCommentType.HASH)
				),
				MOD_ID
		);
	}

	private static void loadGlobalConfig() {
		globalConfig = configContainerHelper.loadAndUpdateInConfigDirectory();
		config = globalConfig;
	}

	private static void initRecipes() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, createId("food_journal"), new FoodJournalRecipeSerializer());
	}

	private static void initNativeFoodContainerItems() {
		List<Item> foodContainerItems = new ArrayList<>(4);
		if (config.items.enablePaperBag) {
			foodContainerItems.add(Registry.register(
					BuiltInRegistries.ITEM, createId("paper_bag"),
					new FoodContainerItem(new Item.Properties().stacksTo(1).food(EMPTY_FOOD_COMPONENT), 5, MenuType.HOPPER)
			));
		}
		if (config.items.enableLunchBox) {
			foodContainerItems.add(Registry.register(
					BuiltInRegistries.ITEM, createId("lunch_box"),
					new FoodContainerItem(new Item.Properties().stacksTo(1).food(EMPTY_FOOD_COMPONENT), 9, MenuType.GENERIC_3x3)
			));
		}
		if (config.items.enablePicnicBasket) {
			foodContainerItems.add(Registry.register(
					BuiltInRegistries.ITEM, createId("picnic_basket"),
					new FoodContainerItem(new Item.Properties().stacksTo(1).food(EMPTY_FOOD_COMPONENT), 9, MenuType.GENERIC_3x3)
			));
		}
		SpiceOfFabric.foodContainerItems = foodContainerItems.toArray(new Item[0]);
	}

	private static void initItemGroups() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			entries.accept(createFoodJournalStack());
			for (Item item : foodContainerItems) {
				entries.accept(item);
			}
		});
	}

	private static void initFoodEvents() {
		FoodEvents.EATEN.on(SpiceOfFabric::onFoodEaten);
		//# if MC_VERSION_NUMBER >= 12006
		FoodModifications.EATING_TIME_SECONDS_MODIFIERS
				.register((PlayerFoodModifier<Float>) SpiceOfFabric::modifyEatingTime, createId("config_expression"));
		//# else
		//- FoodModifications.EATING_TIME_MODIFIERS
		//- 		.register((PlayerFoodModifier<Integer>) SpiceOfFabric::modifyEatingTime, createId("config_expression"));
		//# end
		FoodModifications.PROPERTIES_MODIFIERS.register(SpiceOfFabric::modifyFoodProperties, createId("config_expression"));
	}


	private static void onFoodEaten(FoodEvents.Eaten event) {
		FoodContext context = event.context();
		if (context.user() instanceof ServerPlayer player) {
			FoodHistory foodHistory = FoodHistory.get(player);
			ItemStack foodStack = FoodUtils.getFoodStack(context);
			foodHistory.addFood(foodStack, player);
			SOFConfig.Carrot carrotConfig = SpiceOfFabric.config.carrot;
			if (carrotConfig.enable && (player.getMaxHealth() < carrotConfig.maxHealth || carrotConfig.maxHealth == -1)) {
				SpiceOfFabric.updateMaxHealth(player, true, true);
			}
		}
	}

	//# if MC_VERSION_NUMBER >= 12006
	private static Float modifyEatingTime(Float eatingTime, FoodContext context, Player player) {
	//# else
	//- private static Integer modifyEatingTime(Integer eatingTime, FoodContext context, Player player) {
	//# end
		FoodHistory foodHistory = FoodHistory.get(player);
		if (foodHistory == null) {
			return eatingTime;
		}

		config.food.prepareConsumeDurationExpression(
				foodHistory.getTimesRecentlyEaten(FoodUtils.getFoodStack(context)),
				context.originalFoodHunger(),
				context.originalFoodSaturationModifier(),
				eatingTime
		);
		return config.food.evaluateConsumeDurationExpression();
	}

	private static de.siphalor.capsaicin.api.food.FoodProperties modifyFoodProperties(de.siphalor.capsaicin.api.food.FoodProperties foodProperties, FoodContext context) {
		int timesEaten;
		if (context.user() instanceof Player player) {
			FoodHistory foodHistory = FoodHistory.get(player);
			if (foodHistory != null) {
				timesEaten = foodHistory.getTimesRecentlyEaten(FoodUtils.getFoodStack(context));
			} else {
				timesEaten = 0;
			}
		} else {
			timesEaten = 0;
		}

		config.food.prepareHungerExpressions(
				timesEaten,
				foodProperties.getHunger(),
				foodProperties.getSaturationModifier(),
				0
		);
		foodProperties.setHunger(config.food.evaluateHungerExpression());
		foodProperties.setSaturationModifier(config.food.evaluateSaturationExpression());
		return foodProperties;
	}

	public static boolean hasClientMod(ServerPlayer player) {
		return SOFCommonNetworking.hasClientMod(player);
	}

	public static AttributeModifier createHealthModifier(double amount) {
		return new AttributeModifier(
				//# if MC_VERSION_NUMBER >= 12100
				PLAYER_HEALTH_MODIFIER_ID,
				//# else
				//- PLAYER_HEALTH_MODIFIER_UUID,
				//- MOD_ID,
				//# end
				amount,
				//# if MC_VERSION_NUMBER >= 12006
				AttributeModifier.Operation.ADD_VALUE
				//# else
				//- AttributeModifier.Operation.ADDITION
				//# end
		);
	}

	public static void updateMaxHealth(ServerPlayer player, boolean sync, boolean announce) {
		AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
		double oldValue = maxHealthAttr.getValue();
		//# if MC_VERSION_NUMBER >= 12100
		maxHealthAttr.removeModifier(PLAYER_HEALTH_MODIFIER_ID);
		//# else
		//- maxHealthAttr.removeModifier(PLAYER_HEALTH_MODIFIER_UUID);
		//# end

		if (config.carrot.enable) {
			FoodHistory foodHistory = ((IHungerManager) player.getFoodData()).spiceOfFabric_getFoodHistory();
			maxHealthAttr.addPermanentModifier(createHealthModifier(foodHistory.getCarrotHealthOffset(player)));
		}

		if (sync) {
			player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), Collections.singleton(maxHealthAttr)));
			player.connection.send(new ClientboundSetHealthPacket(player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
		}
		if (announce && maxHealthAttr.getValue() > oldValue) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1F, 1F);
		}
	}

	public static boolean isFoodJournal(ItemStack stack) {
		if (stack == null) {
			return false;
		}
		//# if MC_VERSION_NUMBER >= 12006
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		return customData != null && customData.contains(SpiceOfFabric.FOOD_JOURNAL_FLAG);
		//# else
		//- CompoundTag nbt = stack.getTag();
		//- return nbt != null && nbt.contains(FOOD_JOURNAL_FLAG, 1) && nbt.getBoolean(FOOD_JOURNAL_FLAG);
		//# end
	}

	public static ItemStack createFoodJournalStack() {
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		//# if MC_VERSION_NUMBER >= 12006
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
				Filterable.from(FilteredText.fullyFiltered("")),
				"Me",
				0,
				List.of(),
				true
		));
		stack.set(DataComponents.ITEM_NAME, Component.literal("Diet Journal").withStyle(ChatFormatting.BOLD));
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag ->
				tag.putBoolean(FOOD_JOURNAL_FLAG, true)
		));
		//# else
		//- CompoundTag compound = stack.getOrCreateTag();
		//- compound.putString("title", "");
		//- compound.putString("author", "Me");
		//- compound.putBoolean(SpiceOfFabric.FOOD_JOURNAL_FLAG, true);
		//- stack.getOrCreateTagElement("display").putString("Name", "{\"translate\":\"Diet Journal\",\"bold\":true}");
		//# end
		return stack;
	}

	public static ResourceLocation createId(String path) {
		//# if MC_VERSION_NUMBER >= 12100
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
		//# else
		//- return new ResourceLocation(MOD_ID, path);
		//# end
	}
}
