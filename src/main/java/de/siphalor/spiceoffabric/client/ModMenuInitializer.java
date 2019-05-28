package de.siphalor.spiceoffabric.client;

import de.siphalor.spiceoffabric.Core;
import de.siphalor.spiceoffabric.config.Config;
import io.github.prospector.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

import java.util.Optional;
import java.util.function.Supplier;

public class ModMenuInitializer implements ModMenuApi {
	@Override
	public String getModId() {
        return Core.MODID;
	}

	@Override
	public Optional<Supplier<Screen>> getConfigScreen(Screen screen) {
		return Optional.of(() -> Config.tweedClothBridge.open());
	}
}
