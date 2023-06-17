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
			},
			new Function("power", 2) {
				@Override
				public double apply(double... args) {
					return Math.pow(args[0], args[1]);
				}
			}
	};

	@AConfigEntry(environment = ConfigEnvironment.SYNCED)
	public static ItemTipDisplayStyle showLastEatenTips = ItemTipDisplayStyle.NONE;

	public enum ItemTipDisplayStyle {
		NONE, SIMPLE, EXTENDED
	}

	public static Respawn respawn;

	@AConfigBackground("textures/block/red_wool.png")
	public static class Respawn {
		@AConfigEntry(
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = AFTER_DEATH_VARIABLES_JOINED)
		)
		public String hunger = "max(14, hunger)";
		@AConfigEntry(
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = AFTER_DEATH_VARIABLES_JOINED)
		)
		public String saturation = "saturation";
		public boolean resetHistory = false;
		public boolean resetCarrotMode = false;
	}

	@AConfigEntry(environment = ConfigEnvironment.SYNCED)
	public static Food food;

	@AConfigBackground("textures/block/melon_side.png")
	public static class Food {
		@AConfigEntry(
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = ITEM_VARIABLES_JOINED)
		)
		public String hunger = "hungerValue * power(0.7, timesEaten)";
		@AConfigEntry(
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = ITEM_VARIABLES_JOINED)
		)
		public String saturation = "saturationValue";
		@AConfigEntry(
				constraints = @AConfigConstraint(value = ExpressionConstraint.class, param = ITEM_VARIABLES_JOINED)
		)
		public String consumeDuration = "consumeDuration * power(1.3, timesEaten)";
		@AConfigEntry(
				constraints = @AConfigConstraint(value = RangeConstraint.class, param = "0..")
		)
		public int historyLength = 20;
	}

	public static Carrot carrot;

	@AConfigBackground("textures/block/orange_terracotta.png")
	public static class Carrot {
		public boolean enable = false;
		public String healthFormula = "0.6 * baseHealth + max(2 * floor(log2(uniqueFoodsEaten)), 0)";
		@AConfigEntry(
				constraints = @AConfigConstraint(value = RangeConstraint.class, param = "-1..200")
		)
		public int maxHealth = -1;
	}

	@AConfigEntry(scope = ConfigScope.GAME)
	public static Items items;

	@AConfigBackground("textures/block/beehive_end.png")
	public static class Items {
		public boolean usePolymer = false;
		public boolean enablePaperBag = false;
		public boolean enableLunchBox = false;
		public boolean enablePicnicBasket = false;
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
