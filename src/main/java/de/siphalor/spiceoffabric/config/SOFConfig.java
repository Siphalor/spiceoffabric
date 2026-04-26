package de.siphalor.spiceoffabric.config;

import de.siphalor.tweed5.coat.bridge.api.TweedCoatAttributes;
import de.siphalor.tweed5.commentloaderextension.api.CommentLoaderExtension;
import de.siphalor.tweed5.defaultextensions.patch.api.PatchExtension;
import de.siphalor.tweed5.fabric.helper.api.DefaultTweedMinecraftWeaving;
import de.siphalor.tweed5.weaver.pojo.api.annotation.CompoundWeaving;
import de.siphalor.tweed5.weaver.pojo.api.annotation.TweedExtension;
import de.siphalor.tweed5.weaver.pojoext.attributes.api.Attribute;
import de.siphalor.tweed5.weaver.pojoext.attributes.api.AttributeDefault;
import de.siphalor.tweed5.weaver.pojoext.serde.api.EntryReadWriteConfig;
import de.siphalor.tweed5.weaver.pojoext.validation.api.Validator;
import de.siphalor.tweed5.weaver.pojoext.validation.api.validators.WeavableNumberRangeValidator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@DefaultTweedMinecraftWeaving
@TweedExtension(PatchExtension.class)
@TweedExtension(CommentLoaderExtension.class)
@CompoundWeaving(namingFormat = "kebab_case")
@AttributeDefault(key = TweedCoatAttributes.BACKGROUND_TEXTURE, defaultValue = "textures/block/green_concrete_powder.png")
@AttributeDefault(key = SOFTweedAttributes.SCOPE, defaultValue = SOFTweedAttributes.SCOPE_ANY)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class SOFConfig {
	@Attribute(key = SOFTweedAttributes.SYNCED, value = SOFTweedAttributes.SYNCED_S2C)
	public ItemTipDisplayStyle showLastEatenTips = ItemTipDisplayStyle.NONE;

	@Attribute(key = SOFTweedAttributes.SCOPE, value = SOFTweedAttributes.SCOPE_GAME)
	public boolean enableJournalCommand = false;

	public enum ItemTipDisplayStyle {
		NONE, SIMPLE, EXTENDED
	}

	@CompoundWeaving
	@Attribute(key = TweedCoatAttributes.BACKGROUND_TEXTURE, value = "textures/block/red_wool.png")
	public Respawn respawn = new Respawn();

	public static class Respawn {
		@Validator(SOFExpression.Validator.class)
		@EntryReadWriteConfig(SOFExpression.ReaderWriter.AFTER_DEATH_NAME)
		public SOFExpression hunger = SOFExpression.parse("max(14, hunger)", SOFExpression.Config.AFTER_DEATH);

		@Validator(SOFExpression.Validator.class)
		@EntryReadWriteConfig(SOFExpression.ReaderWriter.AFTER_DEATH_NAME)
		public SOFExpression saturation = SOFExpression.parse("saturation", SOFExpression.Config.AFTER_DEATH);

		public boolean resetHistory = false;

		public boolean resetCarrotMode = false;

		public void prepareExpressions(int hunger, float saturation) {
			this.hunger.setVariable("hunger", hunger);
			this.hunger.setVariable("saturation", saturation);

			this.saturation.setVariable("hunger", hunger);
			this.saturation.setVariable("saturation", saturation);
		}
	}

	@CompoundWeaving
	@Attribute(key = TweedCoatAttributes.BACKGROUND_TEXTURE, value = "textures/block/melon_side.png")
	@AttributeDefault(key = SOFTweedAttributes.SYNCED, defaultValue = SOFTweedAttributes.SYNCED_S2C)
	public Food food = new Food();

	public static class Food {
		@Validator(SOFExpression.Validator.class)
		@EntryReadWriteConfig(SOFExpression.ReaderWriter.ITEM_NAME)
		public SOFExpression hunger = SOFExpression.parse(
				"hungerValue * power(0.7, timesEaten)",
				SOFExpression.Config.ITEM
		);

		@Validator(SOFExpression.Validator.class)
		@EntryReadWriteConfig(SOFExpression.ReaderWriter.ITEM_NAME)
		public SOFExpression saturation = SOFExpression.parse("saturationValue", SOFExpression.Config.ITEM);

		@Validator(SOFExpression.Validator.class)
		@EntryReadWriteConfig(SOFExpression.ReaderWriter.ITEM_NAME)
		public SOFExpression consumeDuration = SOFExpression.parse(
				"consumeDuration * power(1.3, timesEaten)",
				SOFExpression.Config.ITEM
		);

		@Validator(value = WeavableNumberRangeValidator.class, config = "0=..")
		public int historyLength = 20;

		public void prepareHungerExpressions(
				int timesEaten,
				int hungerValue,
				float saturationValue,
				int consumeDuration
		) {
			this.hunger.setVariable("timesEaten", timesEaten);
			this.hunger.setVariable("hungerValue", hungerValue);
			this.hunger.setVariable("saturationValue", saturationValue);
			this.hunger.setVariable("consumeDuration", consumeDuration);

			this.saturation.setVariable("timesEaten", timesEaten);
			this.saturation.setVariable("hungerValue", hungerValue);
			this.saturation.setVariable("saturationValue", saturationValue);
			this.saturation.setVariable("consumeDuration", consumeDuration);
		}

		public int evaluateHungerExpression() {
			return (int) Math.max(Math.round(hunger.evaluate()), 0L);
		}

		public float evaluateSaturationExpression() {
			return (float) Math.max(saturation.evaluate(), 0D);
		}

		public void prepareConsumeDurationExpression(
				int timesEaten,
				int hungerValue,
				float saturationValue,
				//# if MC_VERSION_NUMBER >= 12006
				float consumeDuration
				//# else
				//- int consumeDuration
				//# end
		) {
			this.consumeDuration.setVariable("timesEaten", timesEaten);
			this.consumeDuration.setVariable("hungerValue", hungerValue);
			this.consumeDuration.setVariable("saturationValue", saturationValue);
			this.consumeDuration.setVariable("consumeDuration", consumeDuration);
		}

		//# if MC_VERSION_NUMBER >= 12006
		public float evaluateConsumeDurationExpression() {
			return (float) consumeDuration.evaluate();
		}
		//# else
		//- public int evaluateConsumeDurationExpression() {
		//- 	return (int) Math.max(Math.round(consumeDuration.evaluate()), 0L);
		//- }
		//# end
	}

	@CompoundWeaving
	@Attribute(key = TweedCoatAttributes.BACKGROUND_TEXTURE, value = "textures/block/orange_terracotta.png")
	public Carrot carrot = new Carrot();

	public static class Carrot {
		public boolean enable = false;

		@Validator(SOFExpression.Validator.class)
		@EntryReadWriteConfig(SOFExpression.ReaderWriter.HEALTH_FORMULA_NAME)
		public SOFExpression healthFormula = SOFExpression.parse(
				"0.6 * baseHealth + max(2 * floor(log2(uniqueFoodsEaten)), 0)",
				SOFExpression.Config.HEALTH_FORMULA
		);

		@Validator(value = WeavableNumberRangeValidator.class, config = "-1=..=200")
		public int maxHealth = -1;

		public boolean uneatenInJournal = true;

		public void prepareExpressions(int uniqueFoods, int baseHealth) {
			this.healthFormula.setVariable("uniqueFoodsEaten", uniqueFoods);
			this.healthFormula.setVariable("baseHealth", baseHealth);
		}
	}

	@CompoundWeaving
	@Attribute(key = TweedCoatAttributes.BACKGROUND_TEXTURE, value = "textures/block/beehive_end.png")
	@AttributeDefault(key = SOFTweedAttributes.SCOPE, defaultValue = SOFTweedAttributes.SCOPE_GAME)
	public Items items = new Items();

	public static class Items {
		public boolean usePolymer = false;
		public boolean enablePaperBag = false;
		public boolean enableLunchBox = false;
		public boolean enablePicnicBasket = false;
	}

	/*
	@AConfigFixer("food")
	public static <V extends DataValue<V, L, O>, L extends DataList<V, L, O>, O extends DataObject<V, L, O>>
	void fixFood(O foodObject, O root) {
		if (foodObject.has("increase-eating-time")) {
			V dataValue = foodObject.get("increase-eating-time");
			if (dataValue.isBoolean()) {
				if (dataValue.asBoolean()) {
					foodObject.set("consume-duration", "consumeDuration * timesEaten");
				} else {
					foodObject.set("consume-duration", "consumeDuration");
				}
			}
			foodObject.remove("increase-eating-time");
		}
	}

	@AConfigFixer("carrot")
	public static <V extends DataValue<V, L, O>, L extends DataList<V, L, O>, O extends DataObject<V, L, O>>
	void fixCarrot(O carrotObject, O root) {
		boolean oldFormulaFound = false;
		if (carrotObject.has("start-hearts")) {
			carrotObject.set("old-start-hearts", carrotObject.get("start-hearts"));
			carrotObject.remove("start-hearts");
			oldFormulaFound = true;
		}
		if (carrotObject.has("unlock-rule")) {
			carrotObject.set("old-unlock-rule", carrotObject.get("unlock-rule"));
			carrotObject.remove("unlock-rule");
			oldFormulaFound = true;
		}
		if (carrotObject.hasInt("max-hearts")) {
			int maxHearts = carrotObject.getInt("max-hearts", -1);
			if (maxHearts < 0) {
				carrotObject.set("max-health", -1);
			} else {
				carrotObject.set("max-health", maxHearts * 2);
			}
			carrotObject.remove("max-hearts");
		}
		if (carrotObject.getBoolean("enable", false) && oldFormulaFound) {
			System.err.println("[Spice of Fabric] Found old carrot configuration! You'll need to fix the config manually since formulas changed drastically");
		}
	}
	 */
}
