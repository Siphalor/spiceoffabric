package de.siphalor.spiceoffabric.config;

import de.siphalor.spiceoffabric.Core;
import de.siphalor.tweed.config.*;
import de.siphalor.tweed.config.constraints.IntRangeConstraint;
import de.siphalor.tweed.config.entry.BooleanEntry;
import de.siphalor.tweed.config.entry.IntEntry;
import de.siphalor.tweed.config.entry.StringEntry;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

public class Config {
	static final String[] itemVariables = new String[]{"timesEaten", "hungerValue", "saturationValue"};
	public static Expression hungerExpression;
	public static Expression saturationExpression;

	static final String[] afterDeathVariables = new String[]{"hunger", "saturation"};
	public static Expression hungerAfterDeathExpression;
	public static Expression saturationAfterDeathExpression;

	public static final Function[] customExpFunctions = new Function[]{
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

	private static ConfigFile file = TweedRegistry.registerConfigFile(Core.MODID).setReloadListener(Config::reload);

	public static IntEntry historyLength =
		file.register("history-length", new IntEntry(20))
			.addConstraint(new IntRangeConstraint(1, Integer.MAX_VALUE))
			.setComment("Sets the amount of eaten foods to keep in the history");

	public static ConfigCategory respawnCategory =
		file.register("respawn", new ConfigCategory())
			.setComment("Here can you edit the used expressions used for calculating the player stats after respawning." + System.lineSeparator() +

					"Expressions are simple mathematical terms with the following variables:" + System.lineSeparator() +
					"\thunger is the amount of hunger the player had when dying" + System.lineSeparator() +
					"\tsaturation is the amount of hunger the player had when dying"
				);
	public static StringEntry hungerAfterDeathConfig =
		respawnCategory.register("hunger", new StringEntry("max(14, hunger)"))
			.addConstraint(new ExpressionConstraint(afterDeathVariables, customExpFunctions))
			.setComment("An expression to calculate the hunger value after a fresh respawn");
	public static StringEntry saturationAfterDeathConfig =
		respawnCategory.register("saturation", new StringEntry("saturation"))
			.addConstraint(new ExpressionConstraint(afterDeathVariables, customExpFunctions))
			.setComment("An expression to calculate the saturation value after a fresh respawn");
	public static BooleanEntry resetHistoryAtDeath =
		respawnCategory.register("reset-history", new BooleanEntry(false))
			.setComment("Sets whether the food history should be cleaned at death");

	public static ConfigCategory expressionCategory =
		file.register("food", new ConfigCategory())
			.setComment("Here can you edit the used expressions used for calculating the food stats." + System.lineSeparator() +
				"Expressions are simple mathematical terms with the following variables:" + System.lineSeparator() +
				"\ttimesEaten is the number of times the current food " + System.lineSeparator() +
				"\thungerValue is the game defined hunger value for the current item" + System.lineSeparator() +
				"\tsaturationValue is the saturation modifier defined for the current item"
			);
	public static StringEntry hungerConfig =
		expressionCategory.register("hunger", new StringEntry("hungerValue * 0.7 ^ timesEaten"))
			.addConstraint(new ExpressionConstraint(itemVariables, customExpFunctions))
			.setComment("Calculates the food level bonus to earn from eating a food item");
	public static StringEntry saturationConfig =
		expressionCategory.register("saturation", new StringEntry("saturationValue"))
			.addConstraint(new ExpressionConstraint(itemVariables, customExpFunctions))
			.setComment("Calculates the saturation modifier for a food item");


	public static void initialize() {
		reload(ConfigEnvironment.UNIVERSAL, ConfigScope.GAME);
	}

	@SuppressWarnings("unused")
	public static void reload(ConfigEnvironment environment, ConfigScope scope) {
		hungerExpression = new ExpressionBuilder(hungerConfig.value).variables(itemVariables).functions(customExpFunctions).build();
		saturationExpression = new ExpressionBuilder(saturationConfig.value).variables(itemVariables).functions(customExpFunctions).build();

		hungerAfterDeathExpression = new ExpressionBuilder(hungerAfterDeathConfig.value).variables(afterDeathVariables).functions(customExpFunctions).build();
		saturationAfterDeathExpression = new ExpressionBuilder(saturationAfterDeathConfig.value).variables(afterDeathVariables).functions(customExpFunctions).build();
	}

	public static void setHungerExpressionValues(int timesEaten, int hungerValue, float saturationValue) {
		hungerExpression.setVariable("timesEaten", timesEaten);
		hungerExpression.setVariable("hungerValue", hungerValue);
		hungerExpression.setVariable("saturationValue", saturationValue);

		saturationExpression.setVariable("timesEaten", timesEaten);
		saturationExpression.setVariable("hungerValue", hungerValue);
		saturationExpression.setVariable("saturationValue", saturationValue);
	}

	public static void setAfterDeathExpressionValues(int hunger, float saturation) {
		hungerAfterDeathExpression.setVariable("hunger", hunger);
		hungerAfterDeathExpression.setVariable("saturation", saturation);

		saturationAfterDeathExpression.setVariable("hunger", hunger);
		saturationAfterDeathExpression.setVariable("saturation", saturation);
	}

	public static int getHungerValue() {
		return (int) Math.max(Math.round(hungerExpression.evaluate()), 0L);
	}

	public static float getSaturationValue() {
		return (float) Math.max(saturationExpression.evaluate(), 0D);
	}
}
