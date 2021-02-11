package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.MaxUseTimeCalculator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {
	@Shadow public abstract boolean isFood();

	@Shadow public abstract Item getItem();

	@Inject(method = "getMaxUseTime", at = @At("RETURN"), cancellable = true)
	public void getMaxUseTime(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
		if(isFood()) {
            callbackInfoReturnable.setReturnValue(MaxUseTimeCalculator.getMaxUseTime((ItemStack)(Object) this));
		}
	}
}
