package de.siphalor.spiceoffabric.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {
	@Inject(method = "handleRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setId(I)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
	public void onRespawned(
			ClientboundRespawnPacket packet,
			CallbackInfo ci,
			@Local(ordinal = 0)
			LocalPlayer oldPlayer,
			@Local(ordinal = 1)
			LocalPlayer newPlayer
	) {
		((IHungerManager) newPlayer.getFoodData())
				.spiceOfFabric_setFoodHistory(((IHungerManager) oldPlayer.getFoodData()).spiceOfFabric_getFoodHistory());
	}
}
