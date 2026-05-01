package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.SpiceOfFabric;
//- import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//- import net.minecraft.nbt.CompoundTag;
//- import net.minecraft.nbt.IntTag;
import net.minecraft.resources.Identifier;
//- import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
//- import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@Mixin(FoodData.class)
public abstract class MixinHungerManager implements IHungerManager {

	@Shadow public abstract void eat(int int_1, float float_1);

	@Shadow private float saturationLevel;

	@Unique
	@Nullable
	protected ServerPlayer player = null;

	@Unique
	protected FoodHistory foodHistory = new FoodHistory();

	@Override
	public void spiceOfFabric_setPlayer(ServerPlayer serverPlayerEntity) {
		player = serverPlayerEntity;
	}

	@Override
	public void spiceOfFabric_clearHistory() {
		foodHistory.reset();
	}

	@Override
	public void spiceOfFabric_setSaturationLevel(float level) {
		saturationLevel = level;
	}

	@Override
	public FoodHistory spiceOfFabric_getFoodHistory() {
		return foodHistory;
	}

	@Override
	public void spiceOfFabric_setFoodHistory(FoodHistory foodHistory) {
		this.foodHistory = foodHistory;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
	public void onDeserialize(
			//# if MC_VERSION_NUMBER >= 12106
			ValueInput valueInput,
			//# else
			//- CompoundTag data,
			//# end
			CallbackInfo callbackInfo
	) {
		//# if MC_VERSION_NUMBER >= 12106
		if (valueInput.contains(SpiceOfFabric.NBT_FOOD_HISTORY_ID)) {
			foodHistory = FoodHistory.read(valueInput.childOrEmpty(SpiceOfFabric.NBT_FOOD_HISTORY_ID));
		//# else
		//- if (data.contains(SpiceOfFabric.NBT_FOOD_HISTORY_ID, 10)) {
		//- 	foodHistory = FoodHistory.read(data.getCompound(SpiceOfFabric.NBT_FOOD_HISTORY_ID));
		//# end

			if (player != null && SpiceOfFabric.config.carrot.enable) {
				AttributeInstance healthAttribute = player.getAttribute(
						Attributes.MAX_HEALTH
				);
				if (healthAttribute == null) {
					SpiceOfFabric.LOGGER.error("Players must have a maximum health!");
					return;
				}
				//# if MC_VERSION_NUMBER >= 12100
				if (healthAttribute.removeModifier(
						//# if MC_VERSION_NUMBER >= 12111
						Identifier.withDefaultNamespace(SpiceOfFabric.PLAYER_HEALTH_MODIFIER_UUID.toString())
						//# else
						//- ResourceLocation.withDefaultNamespace(SpiceOfFabric.PLAYER_HEALTH_MODIFIER_UUID.toString())
						//# end
				)) {
					SpiceOfFabric.updateMaxHealth(player, false, false);
				}
				//# else
				//- if (data.contains(SpiceOfFabric.NBT_VERSION_ID)) {
				//- 	AttributeModifier modifier = healthAttribute.getModifier(SpiceOfFabric.PLAYER_HEALTH_MODIFIER_UUID);
				//- 	if (modifier == null) {
				//- 		SpiceOfFabric.updateMaxHealth(player, false, false);
				//- 	}
				//- } else { // Migrate from old system
				//- 	healthAttribute.removeModifier(SpiceOfFabric.PLAYER_HEALTH_MODIFIER_UUID);
				//- 	healthAttribute.setBaseValue(20D);
				//- 	healthAttribute.addPermanentModifier(SpiceOfFabric.createHealthModifier(
				//- 			foodHistory.getCarrotHealthOffset(player)
				//- 	));
				//- }
				//# end
			}
		}

		if (player != null) {
			((IServerPlayerEntity) player).spiceOfFabric_scheduleFoodHistorySync();
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	public void onSerialize(
			//# if MC_VERSION_NUMBER >= 12106
			ValueOutput valueOutput,
			//# else
			//- CompoundTag data,
			//# end
			CallbackInfo callbackInfo
	) {
		//# if MC_VERSION_NUMBER >= 12106
		foodHistory.write(valueOutput.child(SpiceOfFabric.NBT_FOOD_HISTORY_ID));
		valueOutput.putInt(SpiceOfFabric.NBT_VERSION_ID, SpiceOfFabric.NBT_VERSION);
		//# else
		//- data.put(SpiceOfFabric.NBT_FOOD_HISTORY_ID, foodHistory.write(new CompoundTag()));
		//- data.put(SpiceOfFabric.NBT_VERSION_ID, IntTag.valueOf(SpiceOfFabric.NBT_VERSION));
		//# end
	}
}
