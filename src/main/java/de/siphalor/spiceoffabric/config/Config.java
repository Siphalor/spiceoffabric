package de.siphalor.spiceoffabric.config;

import com.google.common.base.CaseFormat;
import com.mojang.datafixers.util.Pair;
import de.siphalor.tweed4.annotated.*;
import de.siphalor.tweed4.config.ConfigEnvironment;
import de.siphalor.tweed4.config.ConfigScope;
import de.siphalor.tweed4.config.constraints.RangeConstraint;
import de.siphalor.tweed4.data.DataObject;
import de.siphalor.tweed4.data.DataValue;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

@ATweedConfig(
		scope = ConfigScope.SMALLEST,
		environment = ConfigEnvironment.SERVER,
		casing = CaseFormat.LOWER_HYPHEN,
		tailors = "tweed4:cloth"
)
public class Config {
	@AConfigExclude
	private static final String[] itemVariables = new String[]{"timesEaten", "hungerValue", "saturationValue", "consumeDuration"};
	@AConfigExclude
	public static Expression hungerExpression;
	@AConfigExclude
	public static Expression saturationExpression;
	@AConfigExclude
	public static Expression consumeDurationExpression;

	@AConfigExclude
	static final String[] afterDeathVariables = new String[]{"hunger", "saturation"};
	@AConfigExclude
	public static Expression hungerAfterDeathExpression;
	@AConfigExclude
	public static Expression saturationAfterDeathExpression;

	@AConfigExclude
	static final String[] heartUnlockVariables = new String[]{"uniqueFoodsEaten", "heartAmount"};
	@AConfigExclude
	public static Expression heartUnlockExpression;

	@AConfigExclude
	private static final Function[] customExpFunctions = new Function[]{
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
			comment = """
					Here can you edit the used expressions used for calculating the player stats after respawning.
					Expressions are simple mathematical terms with the following variables:
					\thunger is the amount of hunger the player had when dying
					\tsaturation is the amount of hunger the player had when dying"""
	)
	public static Respawn respawn;
	public static class Respawn {
		@AConfigEntry(comment = "An expression to calculate the hunger value after a fresh respawn")
		public String hunger = "max(14, hunger)";

		@AConfigEntry(comment = "An expression to calculate the saturation value after a fresh respawn")
		public String saturation = "saturation";

		@AConfigEntry(comment = "Sets whether the food history should be cleaned at death")
		public boolean resetHistory = false;

		@AConfigEntry(comment = "Sets whether the player's maximum hearts should be reset in carrot mode after death")
		public boolean resetCarrotMode = false;
	}

	@AConfigEntry(
			comment = """
					Here can you edit the used expressions used for calculating the food stats.
					Expressions are simple mathematical terms with the following variables:
					\ttimesEaten is the number of times the current food
					\thungerValue is the game defined hunger value for the current item
					\tsaturationValue is the saturation modifier defined for the current item
					\tconsumeDuration is the time in ticks it takes the player to consume the current item""",
			environment = ConfigEnvironment.SYNCED
	)
	public static Food food;
	public static class Food {
		@AConfigEntry(comment = "Calculates the food level bonus to earn from eating a food item")
		public String hunger = "hungerValue * 0.7 ^ timesEaten";

		@AConfigEntry(comment = "Calculates the saturation modifier for a food item")
		public String saturation = "saturationValue";

		@AConfigEntry(comment = "Calculates the time to consume an item in ticks")
		public String consumeDuration = "consumeDuration * 1.3 ^ timesEaten";

		@AConfigEntry(
				comment = "Sets the amount of eaten foods to keep in the history",
				constraints = @AConfigConstraint(value = RangeConstraint.class, param = "0..")
		)
		public int historyLength = 20;
	}

	@AConfigEntry(
			comment = """
					Here can you enable the good ol' carrot style.
					This means you start with a set amount of hearts and extend it by eating unique foods"""
	)
	public static Carrot carrot;
	public static class Carrot {
		@AConfigEntry(comment = "Enables the carrot style module.")
		public boolean enable = false;

		@AConfigEntry(
				comment = "Sets the amount of hearts with which a new player spawns",
				constraints = @AConfigConstraint(value = RangeConstraint.class, param = "1..100")
		)
		public int startHearts = 6;

		@AConfigEntry(
				comment = """
						Specifies an expression for how many foods a player needs to eat to earn the next heart.
						The result resembles the absolute amount of unique food."""
		)
		public String unlockRule = "2*pow(2, heartAmount - 6)";

		@AConfigEntry(
				comment = """
						Specifies a maximum number of hearts a player can get to through this carrot mode.
						When 0, carrot mode is effectively disabled. (Why should you do this? :P)
						When -1, you can gain a basically infinite amount of hearts.""",
				constraints = @AConfigConstraint(value = RangeConstraint.class, param = "-1..100")
		)
		public int maxHearts = -1;
	}

	@AConfigListener
	public static void reload() {
		hungerExpression = new ExpressionBuilder(food.hunger).variables(itemVariables).functions(customExpFunctions).build();
		saturationExpression = new ExpressionBuilder(food.saturation).variables(itemVariables).functions(customExpFunctions).build();
		consumeDurationExpression = new ExpressionBuilder(food.consumeDuration).variables(itemVariables).functions(customExpFunctions).build();

		hungerAfterDeathExpression = new ExpressionBuilder(respawn.hunger).variables(afterDeathVariables).functions(customExpFunctions).build();
		saturationAfterDeathExpression = new ExpressionBuilder(respawn.saturation).variables(afterDeathVariables).functions(customExpFunctions).build();

		heartUnlockExpression = new ExpressionBuilder(carrot.unlockRule).variables(heartUnlockVariables).functions(customExpFunctions).build();
	}

	@AConfigFixer("food")
	public static void fixFood(DataObject<?> foodObject, DataObject<?> root) {
		if (foodObject.has("increase-eating-time")) {
			DataValue<?> dataValue = foodObject.get("increase-eating-time");
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

	public static void setHeartUnlockExpressionValues(int uniqueFoods, int heartAmount) {
		heartUnlockExpression.setVariable("uniqueFoodsEaten", uniqueFoods);
		heartUnlockExpression.setVariable("heartAmount", heartAmount);
	}

	public static int getHungerValue() {
		return (int) Math.max(Math.round(hungerExpression.evaluate()), 0L);
	}

	public static float getSaturationValue() {
		return (float) Math.max(saturationExpression.evaluate(), 0D);
	}
}
