package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.container.FoodJournalScreenHandler;
import de.siphalor.spiceoffabric.container.FoodJournalView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//- import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WrittenBookItem.class)
public class MixinWrittenBookItem {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	public void onUsed(
			Level world,
			Player user,
			InteractionHand hand,
			//# if MC_VERSION_NUMBER >= 12102
			CallbackInfoReturnable<InteractionResult> cir
			//# else
			//- CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
			//# end
	) {
		ItemStack stack = user.getItemInHand(hand);
		//# if MC_VERSION_NUMBER >= 12110
		if (!world.isClientSide() && SpiceOfFabric.isFoodJournal(stack)) {
		//# else
		//- if (!world.isClientSide && SpiceOfFabric.isFoodJournal(stack)) {
		//# end
			FoodJournalView defaultView = FoodJournalView.getDefault();
			if (defaultView == null) {
				return;
			}
			user.openMenu(new FoodJournalScreenHandler.Factory((ServerPlayer) user, defaultView));
			//# if MC_VERSION_NUMBER >= 12102
			cir.setReturnValue(InteractionResult.SUCCESS);
			//# else
			//- cir.setReturnValue(InteractionResultHolder.success(stack));
			//# end
		}
	}
}
