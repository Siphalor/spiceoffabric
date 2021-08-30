package de.siphalor.spiceoffabric.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayerEntity.class, priority = 1100)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements IServerPlayerEntity {
	protected boolean spiceOfFabric_foodHistorySync = false;

	public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile) {
		super(world, pos, yaw, profile);
	}

	@Override
	public void spiceOfFabric_scheduleFoodHistorySync() {
		spiceOfFabric_foodHistorySync = true;
	}

	@Override
	public boolean spiceOfFabric_foodHistorySync() {
		boolean result = spiceOfFabric_foodHistorySync;
		spiceOfFabric_foodHistorySync = false;
		return result;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	public void onConstruct(MinecraftServer server, ServerWorld world, GameProfile profile, CallbackInfo ci) {
		((IHungerManager) hungerManager).spiceOfFabric_setPlayer((ServerPlayerEntity) (Object) this);

		if (Config.carrot.enable) {
			SpiceOfFabric.updateMaxHealth((ServerPlayerEntity)(Object) this);
		}
	}

	@Inject(method = "copyFrom", at = @At("RETURN"))
	public void onPlayerCopied(ServerPlayerEntity reference, boolean exact, CallbackInfo callbackInfo) {
		if (!exact) {
			Pair<Double, Double> respawnHunger = Config.getRespawnHunger(reference.getHungerManager().getFoodLevel(), reference.getHungerManager().getSaturationLevel());
			hungerManager.setFoodLevel((int) Math.max(respawnHunger.getFirst(), reference.getHungerManager().getFoodLevel()));
			((IHungerManager) hungerManager).spiceOfFabric_setSaturationLevel((float) (double) respawnHunger.getSecond());

			FoodHistory foodHistory = ((IHungerManager) reference.getHungerManager()).spiceOfFabric_getFoodHistory();

			if (Config.respawn.resetHistory) {
				foodHistory.resetHistory();
			}
			if (Config.respawn.resetCarrotMode) {
				foodHistory.resetCarrotHistory();
				EntityAttributeInstance maxHealthAttr = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
				maxHealthAttr.removeModifier(SpiceOfFabric.PLAYER_HEALTH_MODIFIER_UUID);
				maxHealthAttr.addPersistentModifier(
						SpiceOfFabric.createHealthModifier(foodHistory.getCarrotHealthOffset(((ServerPlayerEntity)(Object) this)))
				);
			}

			((IHungerManager) hungerManager).spiceOfFabric_setFoodHistory(foodHistory);

			SpiceOfFabric.syncFoodHistory((ServerPlayerEntity) (Object) this);
		}
	}
}
