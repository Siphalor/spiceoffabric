package de.siphalor.spiceoffabric.container;

//- import de.siphalor.spiceoffabric.item.FoodContainerItem;
import de.siphalor.spiceoffabric.mixin.CustomDataAccessor;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
//- import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.storage.TagValueInput;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemStackInventory implements Container {
	private final ItemStack containerStack;
	private final String nbtKey;
	private final int size;
	private final NonNullList<ItemStack> stacks;

	public static ItemStackInventory fromStack(
			ItemStack containerStack,
			String nbtKey,
			int size
			//# if MC_VERSION_NUMBER >= 12006
			, HolderLookup.Provider levelRegistry
			//# end
	) {
		NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
		//# if MC_VERSION_NUMBER >= 12006
		CustomData customData = containerStack.get(DataComponents.CUSTOM_DATA);
		//# if MC_VERSION_NUMBER >= 12106
		Optional<CompoundTag> oldCompound = Optional.ofNullable((CustomDataAccessor)(Object) customData)
				.map(CustomDataAccessor::getTag)
				.flatMap(tag -> tag.getCompound(nbtKey));
		if (oldCompound.isPresent()) {
			ContainerHelper.loadAllItems(
					TagValueInput.create(
							ProblemReporter.DISCARDING,
							levelRegistry,
							oldCompound.get()
					),
					stacks
			);
		//# else
		//- if (customData != null && customData.contains(nbtKey)) {
		//- 	ContainerHelper.loadAllItems(customData.getUnsafe().getCompound(nbtKey), stacks, levelRegistry);
		//# end
		} else {
			ItemContainerContents containerContents = containerStack.get(DataComponents.CONTAINER);
			if (containerContents != null) {
				containerContents.copyInto(stacks);
			}
		}
		//# else
		//- ContainerHelper.loadAllItems(containerStack.getOrCreateTagElement(nbtKey), stacks);
		//# end
		return new ItemStackInventory(containerStack, nbtKey, size, stacks);
	}

	public NonNullList<ItemStack> getContainedStacks() {
		return stacks;
	}

	@Override
	public int getContainerSize() {
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
	public ItemStack getItem(int slot) {
		return stacks.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack split = stacks.get(slot).split(amount);
		setChanged();
		return split;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack stack = stacks.get(slot);
		stacks.set(slot, ItemStack.EMPTY);
		setChanged();
		return stack;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		//# if MC_VERSION_NUMBER >= 12006
		return stack.has(DataComponents.FOOD);
		//# else
		//- return stack.isEdible() && !(stack.getItem() instanceof FoodContainerItem);
		//# end
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		stacks.set(slot, stack);
		setChanged();
	}

	@Override
	public void setChanged() {
		//# if MC_VERSION_NUMBER >= 12006
		CustomData customData = containerStack.get(DataComponents.CUSTOM_DATA);
		//# if MC_VERSION_NUMBER >= 12110
		if (customData != null && ((CustomDataAccessor)(Object) customData).getTag().contains(nbtKey)) {
		//# else
		//- if (customData != null && customData.contains(nbtKey)) {
		//# end
			customData.update(tag -> tag.remove(nbtKey));
		}
		containerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
		//# else
		//- ContainerHelper.saveAllItems(containerStack.getOrCreateTagElement(nbtKey), stacks);
		//# end
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		stacks.clear();
	}
}
