package de.siphalor.spiceoffabric.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PaginatedReadOnlyInventory implements Container {
	private final List<ItemStack> stacks;
	private final int viewSize;
	private int page;

	public PaginatedReadOnlyInventory(int viewSize, List<ItemStack> stacks) {
		this.viewSize = viewSize;
		this.stacks = stacks;
	}

	public int getPageCount() {
		int count = (stacks.size() - 1) / getContainerSize() + 1;
		if (count <= 0) {
			return 1;
		}
		return count;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	@Override
	public int getContainerSize() {
		return viewSize;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public ItemStack getItem(int slot) {
		int index = page * getContainerSize() + slot;
		if (index < 0 || index >= stacks.size()) {
			return ItemStack.EMPTY;
		}
		return stacks.get(index);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return ItemStack.EMPTY; // N/A
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY; // N/A
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		// N/A
	}

	@Override
	public void setChanged() {
		// N/A
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		// N/A
	}
}
