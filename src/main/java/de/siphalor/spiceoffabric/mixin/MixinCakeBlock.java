package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CakeBlock.class)
public class MixinCakeBlock {
	@Redirect(method = "tryEat", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/HungerManager;add(IF)V"))
	private static void eat(HungerManager hungerManager, int baseHunger, float baseSaturation, WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
		ItemStack stack = new ItemStack(Items.CAKE);
		FoodHistory foodHistory = ((IHungerManager) hungerManager).spiceOfFabric_getFoodHistory();
		Config.setHungerExpressionValues(foodHistory.getTimesEaten(stack), baseHunger, baseSaturation, stack.getMaxUseTime());
		hungerManager.add(Config.getHungerValue(), Config.getSaturationValue());

		if (!world.isClient() && player instanceof ServerPlayerEntity) {
			SpiceOfFabric.onEaten((ServerPlayerEntity) player, foodHistory, stack);
		}
	}
}
