package de.siphalor.spiceoffabric.polymer;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.util.FoodUtils;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
//- import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
//- import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
//- import net.minecraft.core.Registry;
//- import net.minecraft.core.component.DataComponents;
//- import net.minecraft.core.registries.BuiltInRegistries;
//- import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.resources.Identifier;
//- import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SOFPolymer {
	private static final FoodProperties EMPTY_FOOD_COMPONENT = new FoodProperties.Builder().build();

	private SOFPolymer() {
	}

	public static void init() {
		//# if MC_VERSION_NUMBER >= 260100
		PolymerItemUtils.CONTEXT_ITEM_CHECK.register((itemInstance, _) -> {
			if (itemInstance instanceof ItemStack stack) {
				return FoodUtils.isFood(stack);
			} else {
				return FoodUtils.isFood(itemInstance.typeHolder().value());
			}
		});
		//# elif MC_VERSION_NUMBER >= 12110
		//- PolymerItemUtils.CONTEXT_ITEM_CHECK.register((stack, context) -> FoodUtils.isFood(stack));
		//# else
		//- PolymerItemUtils.ITEM_CHECK.register(FoodUtils::isFood);
		//# end

		//# if MC_VERSION_NUMBER >= 12102
		PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, context) -> {
			//# if MC_VERSION_NUMBER >= 260100
			ServerPlayer player = PolymerCommonUtils.getPlayer(context);
			//# else
			//- ServerPlayer player = context.getPlayer();
			//# end
			if (!SpiceOfFabric.hasClientMod(player)) {
				FoodUtils.appendServerTooltips(player, client);
			}
			return client;
		});
		//# else
		//- PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, player) -> {
		//- 	if (!SpiceOfFabric.hasClientMod(player)) {
		//- 		FoodUtils.appendServerTooltips(player, client);
		//- 	}
		//- 	return client;
		//- });
		//# end

		PolymerResourcePackUtils.addModAssets(SpiceOfFabric.MOD_ID);

		SOFConfig.Items itemsConfig = SpiceOfFabric.config.items;
		if (itemsConfig.enablePaperBag) {
			registerFoodContainer("paper_bag", Items.PAPER, Items.POTATO, 5, MenuType.HOPPER);
		}
		if (itemsConfig.enableLunchBox) {
			registerFoodContainer("lunch_box", Items.DARK_OAK_BOAT, Items.COOKIE, 9, MenuType.GENERIC_3x3);
		}
		if (itemsConfig.enablePicnicBasket) {
			registerFoodContainer("picnic_basket", Items.OAK_BOAT, Items.BREAD, 9, MenuType.GENERIC_3x3);
		}
	}

	public static void registerFoodContainer(String idPath, Item emptyItem, Item filledItem, int slots, MenuType<?> screenHandlerType) {
		//# if MC_VERSION_NUMBER >= 12111
		Identifier id = SpiceOfFabric.createId(idPath);
		//# else
		//- ResourceLocation id = SpiceOfFabric.createId(idPath);
		//# end
		//# if MC_VERSION_NUMBER < 12102
		//- PolymerModelData emptyModelData = PolymerResourcePackUtils.requestModel(emptyItem, SpiceOfFabric.createId("item/" + id.getPath() + "_empty"));
		//- PolymerModelData filledModelData = PolymerResourcePackUtils.requestModel(filledItem, SpiceOfFabric.createId("item/" + id.getPath() + "_filled"));
		//# end
		PolymerFoodContainerItem item = SpiceOfFabric.registerItem(idPath, props -> new PolymerFoodContainerItem(
				props,
				slots,
				screenHandlerType,
				emptyItem, filledItem
				//# if MC_VERSION_NUMBER >= 12102
				, SpiceOfFabric.createId(id.getPath() + "_empty")
				, SpiceOfFabric.createId(id.getPath() + "_filled")
				//# else
				//- , emptyModelData.value(), filledModelData.value()
				//# end
		), new Item.Properties().stacksTo(1).food(EMPTY_FOOD_COMPONENT));
		//# if MC_VERSION_NUMBER >= 260100
		CreativeModeTabEvents.modifyOutputEvent
		//# else
		//- ItemGroupEvents.modifyEntriesEvent
		//# end
				(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> entries.accept(item));
	}
}
