package de.siphalor.spiceoffabric.util;

import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import net.minecraft.server.level.ServerPlayer;

public interface IHungerManager {
	FoodHistory spiceOfFabric_getFoodHistory();
	void spiceOfFabric_setFoodHistory(FoodHistory foodHistory);
	void spiceOfFabric_setPlayer(ServerPlayer serverPlayerEntity);
	void spiceOfFabric_clearHistory();
	void spiceOfFabric_setSaturationLevel(float level);
}
