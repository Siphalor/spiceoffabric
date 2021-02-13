package de.siphalor.spiceoffabric.mixin.client;

import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
	@Inject(method = "onPlayerRespawn", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;createPlayer(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/stat/StatHandler;Lnet/minecraft/client/recipebook/ClientRecipeBook;ZZ)Lnet/minecraft/client/network/ClientPlayerEntity;"), locals = LocalCapture.CAPTURE_FAILSOFT)
	public void onRespawned(PlayerRespawnS2CPacket packet, CallbackInfo callbackInfo, RegistryKey<?> key, DimensionType dimensionType, ClientPlayerEntity oldEntity, int i, String brand, ClientPlayerEntity newPlayer) {
		((IHungerManager) newPlayer.getHungerManager()).spiceOfFabric_setFoodHistory(((IHungerManager) oldEntity.getHungerManager()).spiceOfFabric_getFoodHistory());
	}
}
