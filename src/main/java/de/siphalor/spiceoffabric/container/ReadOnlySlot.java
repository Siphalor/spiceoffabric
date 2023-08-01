package de.siphalor.spiceoffabric.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class ReadOnlySlot extends Slot {
	public ReadOnlySlot(Inventory inventory, int index, int x, int y) {
		super(inventory, index, x, y);
	}

	@Override
	public void onQuickTransfer(ItemStack newItem, ItemStack original) {
		// N/A
	}

	@Override
	public void setStack(ItemStack stack) {
		// N/A
	}

	@Override
	public void markDirty() {
		// N/A
	}

	@Override
	public ItemStack takeStack(int amount) {
		// N/A
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canTakeItems(PlayerEntity playerEntity) {
		return false;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return false;
	}
}
