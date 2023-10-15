package de.siphalor.spiceoffabric.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.networking.SOFCommonNetworking;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayerEntity.class, priority = 1100)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements IServerPlayerEntity {
	@Shadow
	@Override
	public abstract void lookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target);

	@Unique
	protected boolean foodHistorySync = false;
	@Unique
	protected long lastContainerEatTime;

	protected MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
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
	public void onConstruct(MinecraftServer server, ServerWorld world, GameProfile profile, CallbackInfo ci) {
		((IHungerManager) hungerManager).spiceOfFabric_setPlayer((ServerPlayerEntity) (Object) this);

		// Set the max health and health for new players
		// The max health for existing players will be overwritten when reading the nbt data
		SpiceOfFabric.updateMaxHealth((ServerPlayerEntity) (Object) this, false, false);
		setHealth(getMaxHealth());
	}

	@Inject(method = "copyFrom", at = @At("RETURN"))
	public void onPlayerCopied(ServerPlayerEntity reference, boolean exact, CallbackInfo callbackInfo) {
		if (exact) { // Teleporting back from the end
			((IHungerManager) hungerManager).spiceOfFabric_setPlayer((ServerPlayerEntity) (Object) this);
			SpiceOfFabric.updateMaxHealth((ServerPlayerEntity) (Object) this, false, false);
			setHealth(reference.getHealth());
		} else { // Respawning
			Pair<Double, Double> respawnHunger = SOFConfig.getRespawnHunger(reference.getHungerManager().getFoodLevel(), reference.getHungerManager().getSaturationLevel());
			hungerManager.setFoodLevel((int) Math.max(respawnHunger.getFirst(), reference.getHungerManager().getFoodLevel()));
			((IHungerManager) hungerManager).spiceOfFabric_setSaturationLevel((float) (double) respawnHunger.getSecond());

			FoodHistory foodHistory = ((IHungerManager) reference.getHungerManager()).spiceOfFabric_getFoodHistory();

			if (SOFConfig.respawn.resetHistory) {
				foodHistory.resetHistory();
			}
			if (SOFConfig.carrot.enable && SOFConfig.respawn.resetCarrotMode) {
				foodHistory.resetUniqueFoodsEaten();
			}

			((IHungerManager) hungerManager).spiceOfFabric_setFoodHistory(foodHistory);

			SOFCommonNetworking.syncFoodHistory((ServerPlayerEntity) (Object) this);
			SpiceOfFabric.updateMaxHealth((ServerPlayerEntity) (Object) this, false, false);
			setHealth(getMaxHealth());
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	public void afterReadCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
		// Update the max health. This overwrites the base definition in the constructor
		// and older data that has been read from the player nbt.
		SpiceOfFabric.updateMaxHealth((ServerPlayerEntity) (Object) this, false, false);

		if (nbt.contains("Health", 99)) {
			this.setHealth(nbt.getFloat("Health"));
		}
	}
}
