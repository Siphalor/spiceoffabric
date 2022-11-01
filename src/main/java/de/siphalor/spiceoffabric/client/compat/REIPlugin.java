package de.siphalor.spiceoffabric.client.compat;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class REIPlugin implements REIClientPlugin {

	@Override
	public void registerEntries(EntryRegistry entryRegistry) {
		entryRegistry.addEntries(EntryStacks.of(SpiceOfFabric.createFoodJournalStack()));
	}
}
