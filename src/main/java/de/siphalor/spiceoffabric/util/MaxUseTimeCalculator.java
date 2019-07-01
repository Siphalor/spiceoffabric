package de.siphalor.spiceoffabric.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class MaxUseTimeCalculator {
	public static PlayerEntity currentPlayer;

	public static int getMaxUseTime(ItemStack itemStack) {
		if(currentPlayer != null) {
			return itemStack.getItem().getMaxUseTime(itemStack) * (((IHungerManager) currentPlayer.getHungerManager()).spiceOfFabric_getFoodHistory().getTimesEaten(itemStack) + 1);
		}
		return 0;
	}
}
