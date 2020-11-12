package de.siphalor.spiceoffabric.mixin.client;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(Item.class)
public abstract class MixinItem {
	@Shadow public abstract boolean isFood();

	@Inject(method = "appendTooltip", at = @At("HEAD"))
	public void appendTooltip(ItemStack stack, World world, List<Text> texts, TooltipContext tooltipContext, CallbackInfo callbackInfo) {
		PlayerEntity playerEntity = MinecraftClient.getInstance().player;
		//noinspection ConstantConditions
		if((isFood() || (Object) this == Items.CAKE)
				&& Config.carrot.enable
				&& playerEntity != null
				&& playerEntity.getHungerManager() != null
				&& ((IHungerManager) playerEntity.getHungerManager()).spiceOfFabric_getFoodHistory() != null
				&& ((IHungerManager) playerEntity.getHungerManager()).spiceOfFabric_getFoodHistory().carrotHistory != null
				&& !((IHungerManager) playerEntity.getHungerManager()).spiceOfFabric_getFoodHistory().carrotHistory.contains(FoodHistoryEntry.fromItemStack(stack))) {
        	texts.add(new TranslatableText(SpiceOfFabric.MOD_ID + ".item.tooltip.never_eaten"));
		}
	}
}
