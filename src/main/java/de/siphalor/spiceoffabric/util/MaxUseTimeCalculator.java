package de.siphalor.spiceoffabric.util;

import de.siphalor.spiceoffabric.config.Config;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;

import java.util.Objects;

public class MaxUseTimeCalculator {
	public static PlayerEntity currentPlayer;

	public static int getMaxUseTime(ItemStack stack) {
		if(currentPlayer != null) {
			FoodComponent foodComponent = stack.getItem().getFoodComponent();
			Config.setConsumeDurationValues(((IHungerManager) currentPlayer.getHungerManager()).spiceOfFabric_getFoodHistory().getTimesEaten(stack), Objects.requireNonNull(foodComponent).getHunger(), foodComponent.getSaturationModifier(), stack.getItem().getMaxUseTime(stack));
			return (int) Config.consumeDurationExpression.evaluate();
		}
		return stack.getMaxUseTime();
	}
}
