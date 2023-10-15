package de.siphalor.spiceoffabric.container;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.Locale;

public enum FoodJournalView {
	HISTORY("Least to most recently eaten"),
	CARROT("All unique foods eaten so far"),
	CARROT_UNEATEN("Unique foods that have not been eaten yet")
	;

	private final Text translatableName;
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
		this.translatableName = new TranslatableText(SpiceOfFabric.MOD_ID + ".journal.screen.view." + name().toLowerCase(Locale.ROOT))
				.styled(style -> style.withItalic(false));
		this.literalName = literalName;
	}

	public Text getTranslatableName() {
		return translatableName;
	}

	public String getLiteralName() {
		return literalName;
	}

	public boolean isAvailable() {
		return switch (this) {
			case HISTORY -> SOFConfig.food.historyLength > 0;
			case CARROT -> SOFConfig.carrot.enable;
			case CARROT_UNEATEN -> SOFConfig.carrot.enable && SOFConfig.carrot.uneatenInJournal;
		};
	}
}
