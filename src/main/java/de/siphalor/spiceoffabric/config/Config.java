package de.siphalor.spiceoffabric.config;

import com.google.common.base.CaseFormat;
import com.mojang.datafixers.util.Pair;
import de.siphalor.tweed4.annotated.*;
import de.siphalor.tweed4.config.ConfigEnvironment;
import de.siphalor.tweed4.config.ConfigScope;
import de.siphalor.tweed4.config.constraints.RangeConstraint;
import de.siphalor.tweed4.data.DataList;
import de.siphalor.tweed4.data.DataObject;
import de.siphalor.tweed4.data.DataValue;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.apache.commons.lang3.StringUtils;

@ATweedConfig(
		scope = ConfigScope.SMALLEST,
		environment = ConfigEnvironment.SERVER,
		casing = CaseFormat.LOWER_HYPHEN,
		tailors = "tweed4:coat"
)
public class Config {
	@AConfigExclude
	private static final String ITEM_VARIABLES_JOINED = "timesEaten,hungerValue,saturationValue,consumeDuration";
	@AConfigExclude
	private static final String[] itemVariables = StringUtils.split(ITEM_VARIABLES_JOINED, ',');
	@AConfigExclude
	public static Expression hungerExpression;
	@AConfigExclude
	public static Expression saturationExpression;
	@AConfigExclude
	public static Expression consumeDurationExpression;

	@AConfigExclude
	private static final String AFTER_DEATH_VARIABLES_JOINED = "hunger,saturation";
	@AConfigExclude
	static final String[] afterDeathVariables = StringUtils.split(AFTER_DEATH_VARIABLES_JOINED, ',');
	@AConfigExclude
	public static Expression hungerAfterDeathExpression;
	@AConfigExclude
	public static Expression saturationAfterDeathExpression;

	@AConfigExclude
	static final String[] healthFormulaVariables = new String[]{"uniqueFoodsEaten", "baseHealth"};
	@AConfigExclude
	public static Expression healthFormulaExpression;

	@AConfigExclude
	static final Function[] customExpFunctions = new Function[]{
		new Function("max", 2) {
			@Override
			public double apply(double... args) {
				return Math.max(args[0], args[1]);
			}
		},
		new Function("min", 2) {
			@Override
			public double apply(double... args) {
				return Math.min(args[0], args[1]);
			}
		}
	};

	@AConfigEntry(
			environment = ConfigEnvironment.SYNCED,
			comment = """
					Must be set on the server!
					Whether the players will be able to see how many foods ago they ate an item directly on the item.
					When set to SIMPLE, just this information will be shown.
					If set to EXTENDED, there'll also be information on how many other foods the player needs to eat to restore the nutrition value.
					With NONE this tooltip is hidden."""
	)
	public static ITEM_TIP_DISPLAY showLastEatenTips = ITEM_TIP_DISPLAY.NONE;

	public enum ITEM_TIP_DISPLAY {
		NONE, SIMPLE, EXTENDED
	}

	@AConfigEntry(
			comment = """
					Edit the expressions that are used for calculating the player stats after respawning.
					Expressions are mathematical terms with the following variables:
					\thunger is the amount of hunger the player had when dying
					\tsaturation is the amount of hunger the player had when dying"""
	)
	public static Respawn respawn;
	@AConfigBackground("textures/block/red_wool.png")
	public static class Respawn {
		@AConfigEntry(
				comment = "Expression that determines the hunger level after a fresh respawn",
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = AFTER_DEATH_VARIABLES_JOINED)
		)
		public String hunger = "max(14, hunger)";

		@AConfigEntry(
				comment = "Expression that determines the saturation level after a fresh respawn",
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = AFTER_DEATH_VARIABLES_JOINED)
		)
		public String saturation = "saturation";

		@AConfigEntry(comment = "Sets whether the food history should be cleaned at death")
		public boolean resetHistory = false;

		@AConfigEntry(comment = "Sets whether the player's maximum hearts should be reset in carrot mode after death")
		public boolean resetCarrotMode = false;
	}

	@AConfigEntry(
			comment = """
					Change the expressions used for calculating the various food properties.
					Expressions are mathematical terms with the following variables:
					\ttimesEaten is the number of times the current food
					\thungerValue is the game defined hunger value for the current item
					\tsaturationValue is the saturation modifier defined for the current item
					\tconsumeDuration is the time in ticks it takes the player to consume the current item""",
			environment = ConfigEnvironment.SYNCED
	)
	public static Food food;
	@AConfigBackground("textures/block/melon_side.png")
	public static class Food {
		@AConfigEntry(
				comment = "Expression that determines the food level to restore when eating a food item",
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = ITEM_VARIABLES_JOINED)
		)
		public String hunger = "hungerValue * 0.7 ^ timesEaten";

		@AConfigEntry(
				comment = "Expression that determines the saturation modifier for a food item",
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = ITEM_VARIABLES_JOINED)
		)
		public String saturation = "saturationValue";

		@AConfigEntry(
				comment = "Expression that determines the time requited to consume an item, given in ticks",
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = ITEM_VARIABLES_JOINED)
		)
		public String consumeDuration = "consumeDuration * 1.3 ^ timesEaten";

		@AConfigEntry(
				comment = "Sets the amount of last eaten foods to use for the calculations in this category",
				constraints = @AConfigConstraint(value = RangeConstraint.class, param = "0..")
		)
		public int historyLength = 20;
	}

	@AConfigEntry(
			comment = """
					The good ol' carrot style.
					Carrot style means, that you start with a certain amount of hearts and gain more by eating unique foods."""
	)
	public static Carrot carrot;
	@AConfigBackground("textures/block/orange_terracotta.png")
	public static class Carrot {
		@AConfigEntry(comment = "Enables the carrot style module.")
		public boolean enable = false;

		@AConfigEntry(
				comment = """
						Specifies an offset in health points (half hearts) from default health.
						Default health in vanilla is 20 but that may change through mods like Origins.
						The resulting value will be floored before use."""
		)
		public String healthFormula = "(0.6 * baseHealth) + max(2 * log2(uniqueFoodsEaten), 0)";

		@AConfigEntry(
				comment = """
						Specifies the maximum number of health points (half hearts) a player can get to through carrot mode.
						When 0, carrot mode is effectively disabled. (Why would you do this? :P)
						When -1, you can gain a basically infinite amount of hearts.""",
				constraints = @AConfigConstraint(value = RangeConstraint.class, param = "-1..200")
		)
		public int maxHealth = -1;
	}

	@AConfigListener
	public static void reload() {
		hungerExpression = new ExpressionBuilder(food.hunger).variables(itemVariables).functions(customExpFunctions).build();
		saturationExpression = new ExpressionBuilder(food.saturation).variables(itemVariables).functions(customExpFunctions).build();
		consumeDurationExpression = new ExpressionBuilder(food.consumeDuration).variables(itemVariables).functions(customExpFunctions).build();

		hungerAfterDeathExpression = new ExpressionBuilder(respawn.hunger).variables(afterDeathVariables).functions(customExpFunctions).build();
		saturationAfterDeathExpression = new ExpressionBuilder(respawn.saturation).variables(afterDeathVariables).functions(customExpFunctions).build();

		healthFormulaExpression = new ExpressionBuilder(carrot.healthFormula).variables(healthFormulaVariables).functions(customExpFunctions).build();
	}

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

	public static void setHungerExpressionValues(int timesEaten, int hungerValue, float saturationValue, int consumeDuration) {
		hungerExpression.setVariable("timesEaten", timesEaten);
		hungerExpression.setVariable("hungerValue", hungerValue);
		hungerExpression.setVariable("saturationValue", saturationValue);
		hungerExpression.setVariable("consumeDuration", consumeDuration);

		saturationExpression.setVariable("timesEaten", timesEaten);
		saturationExpression.setVariable("hungerValue", hungerValue);
		saturationExpression.setVariable("saturationValue", saturationValue);
		saturationExpression.setVariable("consumeDuration", consumeDuration);
	}

	public static void setConsumeDurationValues(int timesEaten, int hungerValue, float saturationValue, int consumeDuration) {
		consumeDurationExpression.setVariable("timesEaten", timesEaten);
		consumeDurationExpression.setVariable("hungerValue", hungerValue);
		consumeDurationExpression.setVariable("saturationValue", saturationValue);
		consumeDurationExpression.setVariable("consumeDuration", consumeDuration);
	}

	public static Pair<Double, Double> getRespawnHunger(int hunger, float saturation) {
		hungerAfterDeathExpression.setVariable("hunger", hunger);
		hungerAfterDeathExpression.setVariable("saturation", saturation);

		saturationAfterDeathExpression.setVariable("hunger", hunger);
		saturationAfterDeathExpression.setVariable("saturation", saturation);

		return Pair.of(hungerAfterDeathExpression.evaluate(), saturationAfterDeathExpression.evaluate());
	}

	public static void setHealthFormulaExpressionValues(int uniqueFoods, int baseHealth) {
		healthFormulaExpression.setVariable("uniqueFoodsEaten", uniqueFoods);
		healthFormulaExpression.setVariable("baseHealth", baseHealth);
	}

	public static int getHungerValue() {
		return (int) Math.max(Math.round(hungerExpression.evaluate()), 0L);
	}

	public static float getSaturationValue() {
		return (float) Math.max(saturationExpression.evaluate(), 0D);
	}
}
