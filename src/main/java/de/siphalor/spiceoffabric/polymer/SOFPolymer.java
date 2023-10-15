package de.siphalor.spiceoffabric.polymer;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.util.FoodUtils;
import eu.pb4.polymer.api.item.PolymerItemUtils;
import eu.pb4.polymer.api.resourcepack.PolymerModelData;
import eu.pb4.polymer.api.resourcepack.PolymerRPUtils;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class SOFPolymer {
	private static final FoodComponent EMPTY_FOOD_COMPONENT = new FoodComponent.Builder().build();

	private SOFPolymer() {
	}

	public static void init() {
		PolymerItemUtils.ITEM_CHECK.register(ItemStack::isFood);

		PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, player) -> {
			if (!SpiceOfFabric.hasClientMod(player)) {
				FoodUtils.appendServerTooltips(player, client);
			}
			return client;
		});

		PolymerRPUtils.addAssetSource(SpiceOfFabric.MOD_ID);

		if (SOFConfig.items.enablePaperBag) {
			registerFoodContainer("paper_bag", Items.PAPER, Items.POTATO, 5, ScreenHandlerType.HOPPER);
		}
		if (SOFConfig.items.enableLunchBox) {
			registerFoodContainer("lunch_box", Items.DARK_OAK_BOAT, Items.COOKIE, 9, ScreenHandlerType.GENERIC_3X3);
		}
		if (SOFConfig.items.enablePicnicBasket) {
			registerFoodContainer("picnic_basket", Items.OAK_BOAT, Items.BREAD, 9, ScreenHandlerType.GENERIC_3X3);
		}
	}

	public static void registerFoodContainer(String idPath, Item emptyItem, Item filledItem, int slots, ScreenHandlerType<?> screenHandlerType) {
		Identifier id = new Identifier(SpiceOfFabric.MOD_ID, idPath);
		PolymerModelData emptyModelData = PolymerRPUtils.requestModel(emptyItem, new Identifier(id.getNamespace(), "item/" + id.getPath() + "_empty"));
		PolymerModelData filledModelData = PolymerRPUtils.requestModel(filledItem, new Identifier(id.getNamespace(), "item/" + id.getPath() + "_filled"));
		Registry.register(Registry.ITEM, id, new PolymerFoodContainerItem(
				new Item.Settings().maxCount(1).food(EMPTY_FOOD_COMPONENT).group(ItemGroup.TOOLS),
				slots, screenHandlerType,
				emptyItem, filledItem, emptyModelData.value(), filledModelData.value()
		));
	}
}
