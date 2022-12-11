package de.siphalor.spiceoffabric.foodhistory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

	public MutableText getStackName() {
		return Text.translatable(getStack().getTranslationKey());
	}

	public ItemStack getStack() {
		ItemStack stack = new ItemStack(Registries.ITEM.get(itemId));
		stack.setNbt(data);
		return stack;
	}

	public String getItemStackSerialization() {
		return "{id:\"" + Registries.ITEM.getId(Registries.ITEM.get(itemId)) + "\",tag:" + data.asString() + ",Count:1}";
	}

	public static FoodHistoryEntry from(PacketByteBuf buffer) {
		FoodHistoryEntry entry = new FoodHistoryEntry();
		entry.itemId = buffer.readVarInt();
		return entry;
	}

	public NbtCompound write(NbtCompound compoundTag) {
        compoundTag.putString("item", Registries.ITEM.getId(Registries.ITEM.get(itemId)).toString());
        compoundTag.put("data", data);
		return compoundTag;
	}

	public FoodHistoryEntry read(NbtCompound compoundTag) {
		Optional<Item> item = Registries.ITEM.getOrEmpty(Identifier.tryParse(compoundTag.getString("item")));
		if (item.isEmpty()) {
			return null;
		}
		itemId = Registries.ITEM.getRawId(item.get());
		data = compoundTag.getCompound("data");
		return this;
	}

	public static FoodHistoryEntry fromItemStack(ItemStack stack) {
        FoodHistoryEntry entry = new FoodHistoryEntry();
        entry.itemId = Registries.ITEM.getRawId(stack.getItem());
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
