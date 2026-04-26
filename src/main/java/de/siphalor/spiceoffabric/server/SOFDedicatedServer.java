package de.siphalor.spiceoffabric.server;

import de.siphalor.spiceoffabric.networking.SOFCommonNetworking;
import net.fabricmc.api.DedicatedServerModInitializer;

public class SOFDedicatedServer implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		SOFCommonNetworking.init();
	}
}
