package de.siphalor.spiceoffabric;

import de.siphalor.tweed.config.*;
import de.siphalor.tweed.config.entry.IntConfig;
import de.siphalor.tweed.config.entry.StringConfig;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.Arrays;
import java.util.HashSet;

public class Config {
	private static ConfigFile file = ConfigRegistry.registerConfigFile(Core.MODID).setReloadListener(Config::reload);

	public static IntConfig historyLength = file.register("history-length", new IntConfig(20));

	public static ConfigCategory expressionCategory = file.category("expression");
	public static StringConfig hungerConfig = expressionCategory.register("hunger-expression", new StringConfig("ceil(hungerValue * (1 - timesEaten / 3))"));
	public static StringConfig saturationConfig = expressionCategory.register("saturation-expression", new StringConfig("saturationValue * (1 - timesEaten / 3)"));

	private static final HashSet<String> variables = new HashSet<>(Arrays.asList("timesEaten", "hungerValue", "saturationValue"));
	public static Expression hungerExpression = new ExpressionBuilder(hungerConfig.value).variables(variables).build();
	public static Expression saturationExpression = new ExpressionBuilder(saturationConfig.value).variables(variables).build();

	public static void initialize() {}

	public static void reload(ConfigEnvironment environment, ConfigDefinitionScope scope) {
	}

	public static void setExpressionValues(int timesEaten, int hungerValue, float saturationValue) {
		hungerExpression.setVariable("timesEaten", timesEaten);
		hungerExpression.setVariable("hungerValue", hungerValue);
		hungerExpression.setVariable("saturationValue", saturationValue);

		saturationExpression.setVariable("timesEaten", timesEaten);
		saturationExpression.setVariable("hungerValue", hungerValue);
		saturationExpression.setVariable("saturationValue", saturationValue);
	}

	public static int getHungerValue() {
		return (int) Math.max(Math.round(hungerExpression.evaluate()), 0L);
	}

	public static float getSaturationValue() {
		return (float) Math.max(saturationExpression.evaluate(), 0D);
	}
}
