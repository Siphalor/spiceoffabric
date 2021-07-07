package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WrittenBookItem.class)
public class MixinWrittenBookItem {
	@Inject(method = "resolve", at = @At("HEAD"), cancellable = true)
	private static void resolve(ItemStack itemStack, ServerCommandSource serverCommandSource, PlayerEntity playerEntity, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
		NbtCompound compoundTag = itemStack.getTag();
		if(compoundTag != null && compoundTag.getBoolean(SpiceOfFabric.FOOD_JOURNAL_FLAG)) {
			FoodHistory foodHistory = ((IHungerManager) playerEntity.getHungerManager()).spiceOfFabric_getFoodHistory();

            compoundTag.put("pages", foodHistory.genJournalPages(playerEntity));
            callbackInfoReturnable.setReturnValue(true);
		}
	}
}
