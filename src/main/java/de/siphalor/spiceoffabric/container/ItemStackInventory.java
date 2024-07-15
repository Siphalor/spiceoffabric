package de.siphalor.spiceoffabric.container;

import de.siphalor.spiceoffabric.item.FoodContainerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class ItemStackInventory implements Inventory {
	private final ItemStack containerStack;
	private final int size;
	private final DefaultedList<ItemStack> stacks;

	public ItemStackInventory(ItemStack containerStack, int size) {
		this.size = size;
		this.containerStack = containerStack;
		stacks = DefaultedList.ofSize(size, ItemStack.EMPTY);
		containerStack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(stacks);
	}

	public DefaultedList<ItemStack> getContainedStacks() {
		return stacks;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return stacks.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack split = stacks.get(slot).split(amount);
		markDirty();
		return split;
	}

	@Override
	public ItemStack removeStack(int slot) {
		ItemStack stack = stacks.get(slot);
		stacks.set(slot, ItemStack.EMPTY);
		markDirty();
		return stack;
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		return stack.contains(DataComponentTypes.FOOD) && !(stack.getItem() instanceof FoodContainerItem);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		stacks.set(slot, stack);
		markDirty();
	}

	@Override
	public void markDirty() {
		containerStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void clear() {
		stacks.clear();
	}
}
