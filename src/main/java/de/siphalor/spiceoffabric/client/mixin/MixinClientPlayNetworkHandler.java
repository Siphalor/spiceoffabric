package de.siphalor.spiceoffabric.client.mixin;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.util.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {
	@Inject(method = "onGameJoin", at = @At("RETURN"))
	public void onGameJoin(GameJoinS2CPacket packet, CallbackInfo callbackInfo) {
		ClientSidePacketRegistry.INSTANCE.sendToServer(SpiceOfFabric.MOD_PRESENT_C2S_PACKET, new PacketByteBuf(Unpooled.buffer()));
	}
}
