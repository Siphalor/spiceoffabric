package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.util.MaxUseTimeCalculator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
	@Shadow protected ItemStack activeItemStack;

	@Inject(method = "getStackInHand", at = @At("HEAD"))
	public void onGetStackInHand(CallbackInfoReturnable<ItemStack> callbackInfoReturnable) {
		if((Object) this instanceof PlayerEntity) {
			MaxUseTimeCalculator.currentPlayer = (PlayerEntity)(Object) this;
		}
	}

	@Inject(method = "getItemUseTimeLeft", at = @At("HEAD"))
	public void onGetItemUseTimeLeft(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
		if((Object) this instanceof PlayerEntity) {
			MaxUseTimeCalculator.currentPlayer = (PlayerEntity)(Object) this;
		}
	}

    @ModifyConstant(method = "tickActiveItemStack")
	private int getConsumeEffectBeginTime(int old) {
		return (int) (activeItemStack.getMaxUseTime() * 0.75F);
	}
}
