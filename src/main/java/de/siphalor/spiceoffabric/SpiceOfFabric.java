package de.siphalor.spiceoffabric;

import de.siphalor.spiceoffabric.server.Commands;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SpiceOfFabric implements ModInitializer {

	public static final String MOD_ID = "spiceoffabric";
	public static final String FOOD_HISTORY_ID = "spiceOfFabric_history";
	public static final String FOOD_JOURNAL_FLAG = MOD_ID + ":food_journal";
	public static final Identifier MOD_PRESENT_C2S_PACKET = new Identifier(MOD_ID, "client_mod_present");
	public static final Identifier SYNC_FOOD_HISTORY_S2C_PACKET = new Identifier(MOD_ID, "sync_food_history");
	public static final Identifier ADD_FOOD_S2C_PACKET = new Identifier(MOD_ID, "add_food");
	public static final Identifier CLEAR_FOODS_S2C_PACKET = new Identifier(MOD_ID, "clear_foods");

	@Override
	public void onInitialize() {
		ServerSidePacketRegistry.INSTANCE.register(MOD_PRESENT_C2S_PACKET, (packetContext, packetByteBuf) -> {
			ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) packetContext.getPlayer();
			((IServerPlayerEntity) serverPlayerEntity).spiceOfFabric_setClientMod(true);
			if(((IServerPlayerEntity) serverPlayerEntity).spiceOfFabric_foodHistorySync()) {
				syncFoodHistory(serverPlayerEntity);
			}
		});

		Commands.register();
	}

	public static void syncFoodHistory(ServerPlayerEntity serverPlayerEntity) {
		if(!((IServerPlayerEntity) serverPlayerEntity).spiceOfFabric_hasClientMod()) return;
		PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
		((IHungerManager) serverPlayerEntity.getHungerManager()).spiceOfFabric_getFoodHistory().write(buffer);
		ServerSidePacketRegistry.INSTANCE.sendToPlayer(serverPlayerEntity, SYNC_FOOD_HISTORY_S2C_PACKET, buffer);
	}
}
