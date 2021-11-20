package de.siphalor.spiceoffabric.foodhistory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.BaseText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FoodHistoryEntry {
	private int itemId;
	private NbtCompound data;

	public FoodHistoryEntry() {
		itemId = 0;
		data = new NbtCompound();
	}

	public void write(PacketByteBuf buffer) {
		buffer.writeVarInt(itemId);
	}

	public BaseText getStackName() {
		return new TranslatableText(getStack().getTranslationKey());
	}

	public ItemStack getStack() {
		ItemStack stack = new ItemStack(Registry.ITEM.get(itemId));
		stack.setNbt(data);
		return stack;
	}

	public String getItemStackSerialization() {
		return "{id:\"" + Registry.ITEM.getId(Registry.ITEM.get(itemId)) + "\",tag:" + data.asString() + ",Count:1}";
	}

	public static FoodHistoryEntry from(PacketByteBuf buffer) {
		FoodHistoryEntry entry = new FoodHistoryEntry();
		entry.itemId = buffer.readVarInt();
		return entry;
	}

	public NbtCompound write(NbtCompound compoundTag) {
        compoundTag.putString("item", Registry.ITEM.getId(Registry.ITEM.get(itemId)).toString());
        compoundTag.put("data", data);
		return compoundTag;
	}

	public FoodHistoryEntry read(NbtCompound compoundTag) {
		Optional<Item> item = Registry.ITEM.getOrEmpty(Identifier.tryParse(compoundTag.getString("item")));
		if (item.isEmpty()) {
			return null;
		}
		itemId = Registry.ITEM.getRawId(item.get());
		data = compoundTag.getCompound("data");
		return this;
	}

	public static FoodHistoryEntry fromItemStack(ItemStack stack) {
        FoodHistoryEntry entry = new FoodHistoryEntry();
        entry.itemId = Registry.ITEM.getRawId(stack.getItem());
		return entry;
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof FoodHistoryEntry)
			return ((FoodHistoryEntry) other).itemId == itemId && ((FoodHistoryEntry) other).data.equals(data);
        return super.equals(other);
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(itemId).hashCode();
	}
}
