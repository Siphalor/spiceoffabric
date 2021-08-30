package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(HungerManager.class)
public abstract class MixinHungerManager implements IHungerManager {

	@Nullable
	protected ServerPlayerEntity spiceOfFabric_player = null;

	@Shadow public abstract void add(int int_1, float float_1);

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

	@Override
	public void spiceOfFabric_setFoodHistory(FoodHistory foodHistory) {
		spiceOfFabric_foodHistory = foodHistory;
	}

	@Inject(method = "eat", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/item/Item;getFoodComponent()Lnet/minecraft/item/FoodComponent;"), cancellable = true)
    public void onItemEat(Item item, ItemStack stack, CallbackInfo callbackInfo) {
		Config.setHungerExpressionValues(spiceOfFabric_foodHistory.getTimesEaten(stack), Objects.requireNonNull(item.getFoodComponent()).getHunger(), item.getFoodComponent().getSaturationModifier(), stack.getMaxUseTime());
		int hunger = Config.getHungerValue();
		float saturation = Config.getSaturationValue();
		add(hunger, saturation);
		if (spiceOfFabric_player != null) {
			SpiceOfFabric.onEaten(spiceOfFabric_player, spiceOfFabric_foodHistory, stack);
		}
		callbackInfo.cancel();
	}

	@Inject(method = "readNbt", at = @At("RETURN"))
	public void onDeserialize(NbtCompound data, CallbackInfo callbackInfo) {
		if(data.contains(SpiceOfFabric.NBT_FOOD_HISTORY_ID, 10)) {
			spiceOfFabric_foodHistory = FoodHistory.read(data.getCompound(SpiceOfFabric.NBT_FOOD_HISTORY_ID));

			if (spiceOfFabric_player != null) {
				EntityAttributeInstance healthAttribute = spiceOfFabric_player.getAttributeInstance(
						EntityAttributes.GENERIC_MAX_HEALTH
				);
				if (healthAttribute == null) {
					System.err.println("[SOF] Players must have a maximum health!");
					return;
				}
				if (data.contains(SpiceOfFabric.NBT_VERSION_ID)) {
					if (Config.carrot.enable) {
						EntityAttributeModifier modifier = healthAttribute.getModifier(SpiceOfFabric.PLAYER_HEALTH_MODIFIER_UUID);
						if (modifier == null) {
							SpiceOfFabric.updateMaxHealth(spiceOfFabric_player);
						}
					}
				} else { // Migrate from old system
					if (Config.carrot.enable) {
						healthAttribute.removeModifier(SpiceOfFabric.PLAYER_HEALTH_MODIFIER_UUID);
						healthAttribute.addPersistentModifier(new EntityAttributeModifier(
								SpiceOfFabric.PLAYER_HEALTH_MODIFIER_UUID,
								SpiceOfFabric.MOD_ID,
								spiceOfFabric_foodHistory.getCarrotHealthOffset(spiceOfFabric_player),
								EntityAttributeModifier.Operation.ADDITION
						));
						healthAttribute.setBaseValue(Config.carrot.oldStartHearts);
					}
				}
			}
		}

		if (spiceOfFabric_player != null) {
			((IServerPlayerEntity) spiceOfFabric_player).spiceOfFabric_scheduleFoodHistorySync();
		}
		/*if(Config.carrotEnabled.value && spiceOfFabric_foodHistory.carrotHistory == null) {
			spiceOfFabric_player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(Config.startHearts.value * 2);
			spiceOfFabric_foodHistory.carrotHistory = new HashSet<>();
		}*/
	}

	@Inject(method = "writeNbt", at = @At("RETURN"))
	public void onSerialize(NbtCompound data, CallbackInfo callbackInfo) {
		data.put(SpiceOfFabric.NBT_FOOD_HISTORY_ID, spiceOfFabric_foodHistory.write(new NbtCompound()));
		data.put(SpiceOfFabric.NBT_VERSION_ID, NbtInt.of(SpiceOfFabric.NBT_VERSION));
	}
}
