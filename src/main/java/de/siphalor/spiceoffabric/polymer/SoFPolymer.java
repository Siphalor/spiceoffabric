package de.siphalor.spiceoffabric.polymer;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.FoodUtils;
import eu.pb4.polymer.api.client.registry.ClientPolymerItem;
import eu.pb4.polymer.api.item.PolymerItemUtils;
import eu.pb4.polymer.api.resourcepack.PolymerModelData;
import eu.pb4.polymer.api.resourcepack.PolymerRPUtils;
import eu.pb4.polymer.impl.client.InternalClientRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class SoFPolymer {
	public static void init() {
		PolymerItemUtils.ITEM_CHECK.register(ItemStack::isFood);

		PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, player) -> {
			if (!SpiceOfFabric.hasMod(player)) {
				FoodUtils.appendServerTooltips(player, client);
			}
			return client;
		});

		PolymerRPUtils.addAssetSource(SpiceOfFabric.MOD_ID);

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
		PolymerModelData emptyModelData = PolymerRPUtils.requestModel(emptyItem, new Identifier(id.getNamespace(), "item/" + id.getPath() + "_empty"));
		PolymerModelData filledModelData = PolymerRPUtils.requestModel(filledItem, new Identifier(id.getNamespace(), "item/" + id.getPath() + "_filled"));
		Registry.register(Registry.ITEM, id, new PolymerFoodContainerItem(
				new Item.Settings().maxCount(1).group(ItemGroup.TOOLS),
				slots, screenHandlerType,
				emptyItem, filledItem, emptyModelData.value(), filledModelData.value()
		));
	}

	public static Identifier getPolymerItemId(ItemStack stack) {
		return PolymerItemUtils.getPolymerIdentifier(stack);
	}

	@SuppressWarnings("UnstableApiUsage")
	public static Item getPolymerItem(Identifier id) {
		ClientPolymerItem clientPolymerItem = InternalClientRegistry.ITEMS.get(id);
		return clientPolymerItem == null ? null : clientPolymerItem.registryEntry();
	}

	public static NbtCompound getPolymerTag(NbtCompound nbt) {
		return nbt.getCompound(PolymerItemUtils.REAL_TAG);
	}
}
