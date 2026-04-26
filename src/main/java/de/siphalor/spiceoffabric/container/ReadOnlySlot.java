package de.siphalor.spiceoffabric.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ReadOnlySlot extends Slot {
	public ReadOnlySlot(Container inventory, int index, int x, int y) {
		super(inventory, index, x, y);
	}

	@Override
	public void onQuickCraft(ItemStack newItem, ItemStack original) {
		// N/A
	}

	@Override
	public void setByPlayer(ItemStack stack) {
		// N/A
	}

	@Override
	public void setChanged() {
		// N/A
	}

	@Override
	public ItemStack remove(int amount) {
		// N/A
		return ItemStack.EMPTY;
	}

	@Override
	public boolean mayPickup(Player playerEntity) {
		return false;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}
}
