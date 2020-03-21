package de.siphalor.spiceoffabric.client.rei;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import me.shedaniel.rei.api.EntryRegistry;
import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.api.plugins.REIPluginV0;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.util.version.VersionParsingException;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

public class REIPlugin implements REIPluginV0 {
	@Override
	public Identifier getPluginIdentifier() {
		return new Identifier(SpiceOfFabric.MOD_ID, "rei_plugin");
	}

	@Override
	public void registerEntries(EntryRegistry entryRegistry) {
		ItemStack itemStack = new ItemStack(Items.WRITTEN_BOOK);
		CompoundTag compoundTag = itemStack.getOrCreateTag();
		compoundTag.putString("title", "");
		compoundTag.putString("author", "Me");
		compoundTag.putBoolean(SpiceOfFabric.FOOD_JOURNAL_FLAG, true);
		itemStack.getOrCreateSubTag("display").putString("Name", "{\"translate\":\"Diet Journal\",\"bold\":true}");
		entryRegistry.registerEntries(EntryStack.create(itemStack));
	}

	@Override
	public SemanticVersion getMinimumVersion() throws VersionParsingException {
		return SemanticVersion.parse("3.0-pre");
	}
}
