package de.siphalor.spiceoffabric.polymer;

import de.siphalor.spiceoffabric.item.FoodContainerItem;
import eu.pb4.polymer.api.item.PolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class PolymerFoodContainerItem extends FoodContainerItem implements PolymerItem {
	private final Item emptyPolymerItem;
	private final Item filledPolymerItem;
	private final int emptyCmd;
	private final int filledCmd;

	public PolymerFoodContainerItem(Settings settings, int size, ScreenHandlerType<?> screenHandlerType, Item emptyPolymerItem, Item filledPolymerItem, int emptyCmd, int filledCmd) {
		super(settings, size, screenHandlerType);
		this.emptyPolymerItem = emptyPolymerItem;
		this.filledPolymerItem = filledPolymerItem;
		this.emptyCmd = emptyCmd;
		this.filledCmd = filledCmd;
	}

	@Override
	public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
		return getInventory(itemStack).isEmpty() ? emptyPolymerItem : filledPolymerItem;
	}

	@Override
	public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
		return getInventory(itemStack).isEmpty() ? emptyCmd : filledCmd;
	}
}
