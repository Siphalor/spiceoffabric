package de.siphalor.spiceoffabric.util;

import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import net.minecraft.server.network.ServerPlayerEntity;

public interface IHungerManager {
	FoodHistory spiceOfFabric_getFoodHistory();
	void spiceOfFabric_setPlayer(ServerPlayerEntity serverPlayerEntity);
	void spiceOfFabric_clearHistory();
	void spiceOfFabric_setSaturationLevel(float level);
}
