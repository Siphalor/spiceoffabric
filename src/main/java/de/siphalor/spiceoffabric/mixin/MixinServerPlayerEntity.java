package de.siphalor.spiceoffabric.mixin;

import com.mojang.authlib.GameProfile;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity {

	public MixinServerPlayerEntity(World world_1, GameProfile gameProfile_1) {
		super(world_1, gameProfile_1);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	public void onConstruct(MinecraftServer server, ServerWorld world, GameProfile profile, ServerPlayerInteractionManager interactionManager, CallbackInfo callbackInfo) {
		((IHungerManager) hungerManager).spiceOfFabric_setPlayer((ServerPlayerEntity)(Object) this);
	}

	@Inject(method = "copyFrom", at = @At("RETURN"))
	public void onPlayerCopied(ServerPlayerEntity reference, boolean exact, CallbackInfo callbackInfo) {
		if(!exact) {
			Config.setAfterDeathExpressionValues(reference.getHungerManager().getFoodLevel(), reference.getHungerManager().getSaturationLevel());
			hungerManager.setFoodLevel((int) Math.max(Config.hungerAfterDeathExpression.evaluate(), reference.getHungerManager().getFoodLevel()));
			((IHungerManager) hungerManager).spiceOfFabric_setSaturationLevel((float) Config.saturationAfterDeathExpression.evaluate());
			if(Config.resetHistoryAtDeath.value)
				((IHungerManager) hungerManager).spiceOfFabric_clearHistory();
		}
	}
}
