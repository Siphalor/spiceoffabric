package de.siphalor.spiceoffabric.container;

import de.siphalor.spiceoffabric.item.FoodContainerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class FoodContainerScreenHandler extends ScreenHandler {
	private final FoodContainerItem foodContainerItem;

	public FoodContainerScreenHandler(FoodContainerItem foodContainerItem, int syncId, PlayerInventory playerInventory, ItemStack containerStack) {
		super(foodContainerItem.getScreenHandlerType(), syncId);
		this.foodContainerItem = foodContainerItem;

		ItemStackInventory inventory = foodContainerItem.getInventory(containerStack);
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
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasStack()) {
			ItemStack moveStack = slot.getStack();
			result = moveStack.copy();
			if (index < foodContainerItem.getSize()) {
				if (!this.insertItem(moveStack, foodContainerItem.getSize(), this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				if (!this.insertItem(moveStack, 0, foodContainerItem.getSize(), false)) {
					return ItemStack.EMPTY;
				}
			}
			if (moveStack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}
		}
		return result;
	}

	private static class FoodSlot extends Slot {
		public FoodSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return inventory.isValid(this.getIndex(), stack);
		}
	}

	public static class Factory implements NamedScreenHandlerFactory {
		private final ItemStack containerStack;
		private final FoodContainerItem foodContainerItem;

		public Factory(FoodContainerItem foodContainerItem, ItemStack containerStack) {
			this.containerStack = containerStack;
			this.foodContainerItem = foodContainerItem;
		}

		@Override
		public Text getDisplayName() {
			return containerStack.getName();
		}

		@Nullable
		@Override
		public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
			return new FoodContainerScreenHandler(foodContainerItem, syncId, playerInventory, containerStack);
		}
	}
}
