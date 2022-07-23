package de.siphalor.spiceoffabric.util;

import de.siphalor.spiceoffabric.config.Config;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;

import java.util.Objects;

public class MaxUseTimeCalculator {
	public static PlayerEntity currentPlayer;

	public static int getMaxUseTime(ItemStack stack, int maxUseTime) {
		if (currentPlayer == null) {
			return maxUseTime;
		}
		if (!stack.isFood()) {
			return maxUseTime;
		}
		FoodComponent foodComponent = stack.getItem().getFoodComponent();
		Config.setConsumeDurationValues(((IHungerManager) currentPlayer.getHungerManager()).spiceOfFabric_getFoodHistory().getTimesEaten(stack), Objects.requireNonNull(foodComponent).getHunger(), foodComponent.getSaturationModifier(), maxUseTime);
		return (int) Config.consumeDurationExpression.evaluate();
	}
}
