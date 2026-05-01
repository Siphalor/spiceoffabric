package de.siphalor.spiceoffabric.foodhistory;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.core.registries.BuiltInRegistries;
//- import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
//- import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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

	//# if MC_VERSION_NUMBER >= 12106
	public static Optional<FoodHistoryEntry> read(ValueInput valueInput) {
		Optional<Item> item = valueInput.getString("item")
				//# if MC_VERSION_NUMBER >= 12111
				.flatMap(id -> Identifier.read(id).result())
				//# else
				//- .flatMap(id -> ResourceLocation.read(id).result())
				//# end
				.flatMap(BuiltInRegistries.ITEM::getOptional);
	//# else
	//- public static Optional<FoodHistoryEntry> read(CompoundTag compoundTag) {
	//- 	Optional<Item> item = BuiltInRegistries.ITEM.getOptional(
	//- 			ResourceLocation.tryParse(compoundTag.getString("item"))
	//- 	);
	//# end
		if (item.isEmpty()) {
			return Optional.empty();
		}
		int itemId = BuiltInRegistries.ITEM.getId(item.get());
		return Optional.of(new FoodHistoryEntry(itemId));
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(itemId);
	}

	//# if MC_VERSION_NUMBER >= 12106
	public void write(ValueOutput valueOutput) {
		valueOutput.putString("item", BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.byId(itemId)).toString());
	}
	//# else
	//- public CompoundTag write(CompoundTag compoundTag) {
	//- 	compoundTag.putString("item", BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.byId(itemId)).toString());
	//- 	return compoundTag;
	//- }
	//# end

	public ItemStack getStack() {
		return new ItemStack(BuiltInRegistries.ITEM.byId(itemId));
	}
}
