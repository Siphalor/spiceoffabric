package de.siphalor.spiceoffabric.polymer;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.FoodUtils;
import eu.pb4.polymer.core.api.client.ClientPolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.impl.client.InternalClientRegistry;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class SoFPolymer {
	private static final FoodComponent EMPTY_FOOD_COMPONENT = new FoodComponent.Builder().build();

	public static void init() {
		PolymerItemUtils.ITEM_CHECK.register(ItemStack::isFood);

		PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, player) -> {
			if (!SpiceOfFabric.hasMod(player)) {
				FoodUtils.appendServerTooltips(player, client);
			}
			return client;
		});

		PolymerResourcePackUtils.addModAssets(SpiceOfFabric.MOD_ID);

		if (Config.items.enablePaperBag) {
			registerFoodContainer("paper_bag", Items.PAPER, Items.POTATO, 5, ScreenHandlerType.HOPPER);
		}
		if (Config.items.enableLunchBox) {
			registerFoodContainer("lunch_box", Items.DARK_OAK_BOAT, Items.COOKIE, 9, ScreenHandlerType.GENERIC_3X3);
		}
		if (Config.items.enablePicnicBasket) {
			registerFoodContainer("picnic_basket", Items.OAK_BOAT, Items.BREAD, 9, ScreenHandlerType.GENERIC_3X3);
		}
	}

	public static void registerFoodContainer(String idPath, Item emptyItem, Item filledItem, int slots, ScreenHandlerType<?> screenHandlerType) {
		Identifier id = new Identifier(SpiceOfFabric.MOD_ID, idPath);
		PolymerModelData emptyModelData = PolymerResourcePackUtils.requestModel(emptyItem, new Identifier(id.getNamespace(), "item/" + id.getPath() + "_empty"));
		PolymerModelData filledModelData = PolymerResourcePackUtils.requestModel(filledItem, new Identifier(id.getNamespace(), "item/" + id.getPath() + "_filled"));
		PolymerFoodContainerItem item = Registry.register(Registries.ITEM, id, new PolymerFoodContainerItem(
				new Item.Settings().maxCount(1).food(EMPTY_FOOD_COMPONENT),
				slots, screenHandlerType,
				emptyItem, filledItem, emptyModelData.value(), filledModelData.value()
		));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> entries.add(item));
	}
}
