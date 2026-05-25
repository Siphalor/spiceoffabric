package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.networking.SOFCommonNetworking;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;
//- import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.EntityAnchorArgument;
//- import net.minecraft.core.BlockPos;
//- import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ServerPlayer.class, priority = 1100)
public abstract class MixinServerPlayerEntity extends Player implements IServerPlayerEntity {
	@Shadow
	@Override
	public abstract void lookAt(EntityAnchorArgument.Anchor anchorPoint, Vec3 target);

	@Unique
	protected boolean foodHistorySync = false;
	@Unique
	protected long lastContainerEatTime;

	protected MixinServerPlayerEntity(
			Level world,
			//# if MC_VERSION_NUMBER < 12106
			//- BlockPos pos,
			//- float yaw,
			//# end
			GameProfile gameProfile
	) {
		super(world, /*# if MC_VERSION_NUMBER < 12106 *//*- pos, yaw, *//*# end */ gameProfile);
	}

	@Override
	public void spiceOfFabric_scheduleFoodHistorySync() {
		foodHistorySync = true;
	}

	@Override
	public boolean spiceOfFabric_foodHistorySync() {
		boolean result = foodHistorySync;
		foodHistorySync = false;
		return result;
	}

	@Override
	public long spiceOfFabric_getLastContainerEatTime() {
		return lastContainerEatTime;
	}

	@Override
	public void spiceOfFabric_setLastContainerEatTime(long time) {
		lastContainerEatTime = time;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	public void onConstruct(
			MinecraftServer server,
			ServerLevel world,
			GameProfile profile,
			//# if MC_VERSION_NUMBER >= 12002
			ClientInformation clientOptions,
			//# end
			CallbackInfo ci
	) {
		((IHungerManager) foodData).spiceOfFabric_setPlayer((ServerPlayer) (Object) this);

		// Set the max health and health for new players
		// The max health for existing players will be overwritten when reading the nbt data
		SpiceOfFabric.updateMaxHealth((ServerPlayer) (Object) this, false, false);
		setHealth(getMaxHealth());
	}

	//# if MC_VERSION_NUMBER >= 12102
	@Inject(method = "restoreFrom", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/attributes/AttributeMap;assignPermanentModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeMap;)V"
	))
	public void onPlayerExactCopiedBeforePermanentModifiers(ServerPlayer reference, boolean exact, CallbackInfo ci) {
		// Vanilla throws an exception if a permanent modifier is already present when calling restoreFrom.
		// We need to set that modifier in the constructor though, because it has to apply to new players as well.
		SpiceOfFabric.removeMaxHealthModifierNoSync((ServerPlayer) (Object) this);
	}
	//# end

	@Inject(method = "restoreFrom", at = @At("RETURN"))
	public void onPlayerCopied(ServerPlayer reference, boolean exact, CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (exact) { // Teleporting back from the end
			((IHungerManager) foodData).spiceOfFabric_setPlayer(self);
			SpiceOfFabric.updateMaxHealth(self, false, false);
			setHealth(reference.getHealth());
		} else { // Respawning
			SOFConfig.Respawn respawnConfig = SpiceOfFabric.config.respawn;
			respawnConfig.prepareExpressions(
					reference.getFoodData().getFoodLevel(),
					reference.getFoodData().getSaturationLevel()
			);
			foodData.setFoodLevel((int) Math.max(
					respawnConfig.hunger.evaluate(),
					reference.getFoodData().getFoodLevel()
			));
			((IHungerManager) foodData).spiceOfFabric_setSaturationLevel((float) respawnConfig.saturation.evaluate());

			FoodHistory foodHistory = ((IHungerManager) reference.getFoodData()).spiceOfFabric_getFoodHistory();

			if (respawnConfig.resetHistory) {
				foodHistory.resetHistory();
			}
			if (SpiceOfFabric.config.carrot.enable && respawnConfig.resetCarrotMode) {
				foodHistory.resetUniqueFoodsEaten();
			}

			((IHungerManager) foodData).spiceOfFabric_setFoodHistory(foodHistory);

			SOFCommonNetworking.syncFoodHistory(self);
			SpiceOfFabric.updateMaxHealth(self, false, false);
			setHealth(getMaxHealth());
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void afterReadCustomDataFromNbt(
			//# if MC_VERSION_NUMBER >= 12106
			ValueInput valueInput,
			//# else
			//- CompoundTag nbt,
			//# end
			CallbackInfo ci
	) {
		// Update the max health. This overwrites the base definition in the constructor
		// and older data that has been read from the player nbt.
		SpiceOfFabric.updateMaxHealth((ServerPlayer) (Object) this, false, false);

		//# if MC_VERSION_NUMBER >= 12106
		valueInput.read("Health", Codec.FLOAT).ifPresent(this::setHealth);
		//# else
		//- if (nbt.contains("Health", 99)) {
		//- 	this.setHealth(nbt.getFloat("Health"));
		//- }
		//# end
	}
}
