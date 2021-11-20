package de.siphalor.spiceoffabric.client.compat;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

@Environment(EnvType.CLIENT)
public class REIPlugin implements REIClientPlugin {

	@Override
	public void registerEntries(EntryRegistry entryRegistry) {
		ItemStack itemStack = new ItemStack(Items.WRITTEN_BOOK);
		NbtCompound compound = itemStack.getOrCreateNbt();
		compound.putString("title", "");
		compound.putString("author", "Me");
		compound.putBoolean(SpiceOfFabric.FOOD_JOURNAL_FLAG, true);
		itemStack.getOrCreateSubNbt("display").putString("Name", "{\"translate\":\"Diet Journal\",\"bold\":true}");
		entryRegistry.addEntries(EntryStacks.of(itemStack));
	}
}
