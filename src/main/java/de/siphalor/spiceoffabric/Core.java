package de.siphalor.spiceoffabric;

import net.fabricmc.api.ModInitializer;

public class Core implements ModInitializer {

	public static final String MODID = "spiceoffabric";
	public static final String FOOD_HISTORY_ID = "spiceOfFabric_history";

	@Override
	public void onInitialize() {
		Config.initialize();
	}
}
