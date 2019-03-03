package de.siphalor.fabricsol;

import de.siphalor.fabricsol.foodhistory.FoodHistory;
import me.elucent.earlgray.api.TraitRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class Core implements ModInitializer {

	public static final String MODID = "fabric-sol";

	@Override
	public void onInitialize() {
		System.out.println("Hello Spice!");

		TraitRegistry.register(new Identifier(MODID, "food_history"), FoodHistory.class);
	}
}
