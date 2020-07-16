package de.siphalor.spiceoffabric.client.compat.mixin;

import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import squeek.appleskin.helpers.FoodHelper;

@Mixin(value = FoodHelper.class, remap = false)
public abstract class MixinFoodHelper {
	@Shadow
	public static FoodHelper.BasicFoodValues getDefaultFoodValues(ItemStack itemStack) {
		return null;
	}

	@Inject(method = "getModifiedFoodValues", at = @At("TAIL"), cancellable = true)
	private static void getModifiedFoodValuesDefault(ItemStack stack, PlayerEntity playerEntity, CallbackInfoReturnable<FoodHelper.BasicFoodValues> callbackInfoReturnable) {
		FoodHelper.BasicFoodValues foodValues = getDefaultFoodValues(stack);
		//noinspection ConstantConditions
		Config.setHungerExpressionValues(((IHungerManager) playerEntity.getHungerManager()).spiceOfFabric_getFoodHistory().getTimesEaten(stack), foodValues.hunger, foodValues.saturationModifier);
        FoodHelper.BasicFoodValues result = new FoodHelper.BasicFoodValues(Config.getHungerValue(), Config.getSaturationValue());
		callbackInfoReturnable.setReturnValue(result);
	}
}
