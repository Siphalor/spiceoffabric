package de.siphalor.spiceoffabric.util;

import net.minecraft.server.network.ServerPlayerEntity;

public interface IHungerManager {
	void spiceOfFabric_setPlayer(ServerPlayerEntity serverPlayerEntity);
	void spiceOfFabric_clearHistory();
	void spiceOfFabric_setSaturationLevel(float level);
}
