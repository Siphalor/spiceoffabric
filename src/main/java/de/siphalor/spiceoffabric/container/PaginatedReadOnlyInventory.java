package de.siphalor.spiceoffabric.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import java.util.List;

public class PaginatedReadOnlyInventory implements Inventory {
	private final List<ItemStack> stacks;
	private final int viewSize;
	private int page;

	public PaginatedReadOnlyInventory(int viewSize, List<ItemStack> stacks) {
		this.viewSize = viewSize;
		this.stacks = stacks;
	}

	public int getPageCount() {
		int count = (stacks.size() - 1) / size() + 1;
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
	public int size() {
		return viewSize;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public ItemStack getStack(int slot) {
		int index = page * size() + slot;
		if (index < 0 || index >= stacks.size()) {
			return ItemStack.EMPTY;
		}
		return stacks.get(index);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return ItemStack.EMPTY; // N/A
	}

	@Override
	public ItemStack removeStack(int slot) {
		return ItemStack.EMPTY; // N/A
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		// N/A
	}

	@Override
	public void markDirty() {
		// N/A
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void clear() {
		// N/A
	}
}
