package de.siphalor.spiceoffabric.container;

import de.siphalor.spiceoffabric.SpiceOfFabric;
//- import de.siphalor.spiceoffabric.config.SOFConfig;
import java.util.Locale;

import net.minecraft.network.chat.Component;

public enum FoodJournalView {
	HISTORY("Least to most recently eaten"),
	CARROT("All unique foods eaten so far"),
	CARROT_UNEATEN("Unique foods that have not been eaten yet")
	;

	private final Component translatableName;
	private final String literalName;

	public static FoodJournalView getDefault() {
		if (HISTORY.isAvailable()) {
			return HISTORY;
		}
		if (CARROT.isAvailable()) {
			return CARROT;
		}
		return null;
	}

	FoodJournalView(String literalName) {
		this.translatableName = Component.translatable(SpiceOfFabric.MOD_ID + ".journal.screen.view." + name().toLowerCase(Locale.ROOT))
				.withStyle(style -> style.withItalic(false));
		this.literalName = literalName;
	}

	public Component getTranslatableName() {
		return translatableName;
	}

	public String getLiteralName() {
		return literalName;
	}

	public boolean isAvailable() {
		return switch (this) {
			case HISTORY -> SpiceOfFabric.config.food.historyLength > 0;
			case CARROT -> SpiceOfFabric.config.carrot.enable;
			case CARROT_UNEATEN -> SpiceOfFabric.config.carrot.enable && SpiceOfFabric.config.carrot.uneatenInJournal;
		};
	}
}
