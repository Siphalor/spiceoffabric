package de.siphalor.spiceoffabric.client.rei;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import me.shedaniel.rei.api.ItemRegistry;
import me.shedaniel.rei.api.REIPluginEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

public class REIPlugin implements REIPluginEntry {
	@Override
	public Identifier getPluginIdentifier() {
		return new Identifier(SpiceOfFabric.MOD_ID, "rei_plugin");
	}

	@Override
	public void registerItems(ItemRegistry itemRegistry) {
		ItemStack itemStack = new ItemStack(Items.WRITTEN_BOOK);
		CompoundTag compoundTag = itemStack.getOrCreateTag();
		compoundTag.putString("title", "");
		compoundTag.putString("author", "Me");
		compoundTag.putBoolean(SpiceOfFabric.FOOD_JOURNAL_FLAG, true);
		itemStack.getOrCreateSubTag("display").putString("Name", "{\"translate\":\"Diet Journal\",\"bold\":true}");
		itemRegistry.registerItemStack(itemStack);
	}
}
