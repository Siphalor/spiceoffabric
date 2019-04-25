package de.siphalor.spiceoffabric;

import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public class Core implements ModInitializer {

	public static final String MODID = "spiceoffabric";
	public static final String FOOD_HISTORY_ID = "spiceOfFabric_history";
	public static final Identifier MOD_PRESENT_C2S_PACKET = new Identifier(MODID, "client_mod_present");
	public static final Identifier SYNC_FOOD_HISTORY_S2C_PACKET = new Identifier(MODID, "sync_food_history");
	public static final Identifier ADD_FOOD_S2C_PACKET = new Identifier(MODID, "add_food");

	@Override
	public void onInitialize() {
		Config.initialize();
		ServerSidePacketRegistry.INSTANCE.register(MOD_PRESENT_C2S_PACKET, (packetContext, packetByteBuf) -> {
			((IServerPlayerEntity) packetContext.getPlayer()).spiceOfFabric_setClientMod(true);
		});
	}

	public void syncFoodHistory(ServerPlayerEntity serverPlayerEntity) {
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		((IHungerManager) serverPlayerEntity.getHungerManager()).spiceOfFabric_getFoodHistory().write(buffer);
		ServerSidePacketRegistry.INSTANCE.sendToPlayer(serverPlayerEntity, SYNC_FOOD_HISTORY_S2C_PACKET, buffer);
	}
}
