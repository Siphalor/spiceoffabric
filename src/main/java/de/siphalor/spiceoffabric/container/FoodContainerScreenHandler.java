package de.siphalor.spiceoffabric.container;

import de.siphalor.spiceoffabric.item.FoodContainerItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FoodContainerScreenHandler extends AbstractContainerMenu {
	private final FoodContainerItem foodContainerItem;

	public FoodContainerScreenHandler(
			FoodContainerItem foodContainerItem,
			int syncId,
			Inventory playerInventory,
			ItemStack containerStack
	) {
		super(foodContainerItem.getScreenHandlerType(), syncId);
		this.foodContainerItem = foodContainerItem;

		//# if MC_VERSION_NUMBER >= 12006
		ItemStackInventory inventory = foodContainerItem.getInventory(
				containerStack,
				playerInventory.player.registryAccess()
		);
		//# else
		//- ItemStackInventory inventory = foodContainerItem.getInventory(containerStack);
		//# end
		for (int i = 0; i < foodContainerItem.getSize(); i++) {
			addSlot(new FoodSlot(inventory, i, 0, 0));
		}
		for (int i = 9; i < 36; i++) {
			addSlot(new Slot(playerInventory, i, 0, 0));
		}
		for (int i = 0; i < 9; i++) {
			addSlot(new Slot(playerInventory, i, 0, 0));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			ItemStack moveStack = slot.getItem();
			result = moveStack.copy();
			if (index < foodContainerItem.getSize()) {
				if (!this.moveItemStackTo(moveStack, foodContainerItem.getSize(), this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				if (!this.moveItemStackTo(moveStack, 0, foodContainerItem.getSize(), false)) {
					return ItemStack.EMPTY;
				}
			}
			if (moveStack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return result;
	}

	private static class FoodSlot extends Slot {
		public FoodSlot(Container inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return container.canPlaceItem(this.getContainerSlot(), stack);
		}
	}

	public static class Factory implements MenuProvider {
		private final ItemStack containerStack;
		private final FoodContainerItem foodContainerItem;

		public Factory(FoodContainerItem foodContainerItem, ItemStack containerStack) {
			this.containerStack = containerStack;
			this.foodContainerItem = foodContainerItem;
		}

		@Override
		public Component getDisplayName() {
			return containerStack.getHoverName();
		}

		@Nullable
		@Override
		public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
			return new FoodContainerScreenHandler(foodContainerItem, syncId, playerInventory, containerStack);
		}
	}
}
