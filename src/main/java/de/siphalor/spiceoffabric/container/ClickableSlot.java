package de.siphalor.spiceoffabric.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class ClickableSlot extends ReadOnlySlot {
	private final Runnable callback;

	public ClickableSlot(Inventory inventory, int index, int x, int y, Runnable callback) {
		super(inventory, index, x, y);
		this.callback = callback;
	}

	@Override
	public boolean canTakeItems(PlayerEntity playerEntity) {
		return true;
	}

	@Override
	public ItemStack takeStack(int amount) {
		callback.run();
		return ItemStack.EMPTY;
	}
}
