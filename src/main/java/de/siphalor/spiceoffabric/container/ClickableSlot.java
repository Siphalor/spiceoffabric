package de.siphalor.spiceoffabric.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ClickableSlot extends ReadOnlySlot {
	private final Runnable callback;

	public ClickableSlot(Container inventory, int index, int x, int y, Runnable callback) {
		super(inventory, index, x, y);
		this.callback = callback;
	}

	@Override
	public boolean mayPickup(Player playerEntity) {
		return true;
	}

	@Override
	public ItemStack remove(int amount) {
		callback.run();
		return ItemStack.EMPTY;
	}
}
