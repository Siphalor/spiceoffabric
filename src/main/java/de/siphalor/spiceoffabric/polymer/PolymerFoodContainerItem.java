package de.siphalor.spiceoffabric.polymer;

import de.siphalor.spiceoffabric.item.FoodContainerItem;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PolymerFoodContainerItem extends FoodContainerItem implements PolymerItem {
	private final Item emptyPolymerItem;
	private final Item filledPolymerItem;
	private final int emptyCmd;
	private final int filledCmd;

	public PolymerFoodContainerItem(Properties settings, int size, MenuType<?> screenHandlerType, Item emptyPolymerItem, Item filledPolymerItem, int emptyCmd, int filledCmd) {
		super(settings, size, screenHandlerType);
		this.emptyPolymerItem = emptyPolymerItem;
		this.filledPolymerItem = filledPolymerItem;
		this.emptyCmd = emptyCmd;
		this.filledCmd = filledCmd;
	}

	@Override
	public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayer player) {
		//# if MC_VERSION_NUMBER >= 12006
		return player != null && getInventory(itemStack, player.registryAccess()).isEmpty()
				? emptyPolymerItem
				: filledPolymerItem;
		//# else
		//- return getInventory(itemStack).isEmpty() ? emptyPolymerItem : filledPolymerItem;
		//# end
	}

	@Override
	public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayer player) {
		//# if MC_VERSION_NUMBER >= 12006
		return player != null && getInventory(itemStack, player.registryAccess()).isEmpty() ? emptyCmd : filledCmd;
		//# else
		//- return getInventory(itemStack).isEmpty() ? emptyCmd : filledCmd;
		//# end
	}
}
