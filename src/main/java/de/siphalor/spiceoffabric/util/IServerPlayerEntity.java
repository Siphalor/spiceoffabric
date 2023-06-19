package de.siphalor.spiceoffabric.util;

public interface IServerPlayerEntity {

	void spiceOfFabric_scheduleFoodHistorySync();
	boolean spiceOfFabric_foodHistorySync();

	long spiceOfFabric_getLastContainerEatTime();
	void spiceOfFabric_setLastContainerEatTime(long time);
}
