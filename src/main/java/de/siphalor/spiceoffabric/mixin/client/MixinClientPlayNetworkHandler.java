package de.siphalor.spiceoffabric.mixin.client;

import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.CommonPlayerSpawnInfo;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
	@Inject(method = "onPlayerRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setId(I)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
	public void onRespawned(PlayerRespawnS2CPacket packet, CallbackInfo ci, CommonPlayerSpawnInfo commonPlayerSpawnInfo, RegistryKey<DimensionType> dimensionKey, RegistryEntry<DimensionType> dimensionEntry, ClientPlayerEntity oldPlayer, ClientPlayerEntity newPlayer) {
		((IHungerManager) newPlayer.getHungerManager()).spiceOfFabric_setFoodHistory(((IHungerManager) oldPlayer.getHungerManager()).spiceOfFabric_getFoodHistory());
	}
}
