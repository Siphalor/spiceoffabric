package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.Core;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.minecraft.client.network.packet.HealthUpdateS2CPacket;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public abstract class MixinHungerManager implements IHungerManager {

	protected ServerPlayerEntity spiceOfFabric_player = null;

	@Shadow public abstract void add(int int_1, float float_1);
	@Shadow public abstract float getSaturationLevel();
	@Shadow public abstract int getFoodLevel();

	@Shadow private float foodSaturationLevel;

	protected FoodHistory spiceOfFabric_foodHistory = new FoodHistory();

	@Override
	public void spiceOfFabric_setPlayer(ServerPlayerEntity serverPlayerEntity) {
		spiceOfFabric_player = serverPlayerEntity;
	}

	@Override
	public void spiceOfFabric_clearHistory() {
		spiceOfFabric_foodHistory.reset();
	}

	@Override
	public void spiceOfFabric_setSaturationLevel(float level) {
		foodSaturationLevel = level;
	}

	@Override
	public FoodHistory spiceOfFabric_getFoodHistory() {
		return spiceOfFabric_foodHistory;
	}

	@Inject(method = "eat", at = @At(value = "INVOKE_ASSIGN", target = "net/minecraft/item/Item.getFoodSetting()Lnet/minecraft/item/FoodItemSetting;"), cancellable = true)
    public void onItemEat(Item item, ItemStack stack, CallbackInfo callbackInfo) {
		Config.setHungerExpressionValues(spiceOfFabric_foodHistory.getTimesEaten(stack), item.getFoodSetting().getHunger(), item.getFoodSetting().getSaturationModifier());
		int hunger = Config.getHungerValue();
		float saturation = Config.getSaturationValue();
		add(hunger, saturation);
		spiceOfFabric_foodHistory.addFood(stack, spiceOfFabric_player);
		if(spiceOfFabric_player != null) {
			spiceOfFabric_player.networkHandler.sendPacket(new HealthUpdateS2CPacket(spiceOfFabric_player.getHealth(), this.getFoodLevel(), this.getSaturationLevel()));
		}
		callbackInfo.cancel();
	}

	@Inject(method = "deserialize", at = @At("RETURN"))
	public void onDeserialize(CompoundTag compoundTag, CallbackInfo callbackInfo) {
		if(compoundTag.containsKey(Core.FOOD_HISTORY_ID, 10)) {
			spiceOfFabric_foodHistory = FoodHistory.read(compoundTag.getCompound(Core.FOOD_HISTORY_ID));
		}
	}

	@Inject(method = "serialize", at = @At("RETURN"))
	public void onSerialize(CompoundTag compoundTag, CallbackInfo callbackInfo) {
		compoundTag.put(Core.FOOD_HISTORY_ID, spiceOfFabric_foodHistory.write(new CompoundTag()));
	}
}
