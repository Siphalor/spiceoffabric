package de.siphalor.spiceoffabric.foodhistory;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class FoodHistoryEntry {
	private final int itemId;

	public static FoodHistoryEntry fromItemStack(ItemStack stack) {
		int itemId = BuiltInRegistries.ITEM.getId(stack.getItem());
		return new FoodHistoryEntry(itemId);
	}

	public static FoodHistoryEntry read(FriendlyByteBuf buffer) {
		int itemId = buffer.readVarInt();
		return new FoodHistoryEntry(itemId);
	}

	public static FoodHistoryEntry read(CompoundTag compoundTag) {
		Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(compoundTag.getString("item")));
		if (item.isEmpty()) {
			return null;
		}
		int itemId = BuiltInRegistries.ITEM.getId(item.get());
		return new FoodHistoryEntry(itemId);
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(itemId);
	}

	public CompoundTag write(CompoundTag compoundTag) {
		compoundTag.putString("item", BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.byId(itemId)).toString());
		return compoundTag;
	}

	public MutableComponent getStackName() {
		return Component.translatable(getStack().getDescriptionId());
	}

	public ItemStack getStack() {
		return new ItemStack(BuiltInRegistries.ITEM.byId(itemId));
	}
}
