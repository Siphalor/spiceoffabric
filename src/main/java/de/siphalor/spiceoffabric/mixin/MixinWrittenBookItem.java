package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.container.FoodJournalScreenHandler;
import de.siphalor.spiceoffabric.container.FoodJournalView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WrittenBookItem.class)
public class MixinWrittenBookItem {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	public void onUsed(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
		ItemStack stack = user.getStackInHand(hand);
		if (!world.isClient && SpiceOfFabric.isFoodJournal(stack)) {
			FoodJournalView defaultView = FoodJournalView.getDefault();
			if (defaultView == null) {
				return;
			}
			user.openHandledScreen(new FoodJournalScreenHandler.Factory((ServerPlayerEntity) user, defaultView));
			cir.setReturnValue(TypedActionResult.success(stack));
		}
	}
}
