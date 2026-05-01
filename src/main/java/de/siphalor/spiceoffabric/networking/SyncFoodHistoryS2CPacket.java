package de.siphalor.spiceoffabric.networking;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.queue.ArrayFixedLengthIntFIFOQueue;
import de.siphalor.spiceoffabric.util.queue.FixedLengthIntFIFOQueueWithStats;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
//- import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public class SyncFoodHistoryS2CPacket
	//# if MC_VERSION_NUMBER >= 12006
	implements CustomPacketPayload
	//# end
{
	//# if MC_VERSION_NUMBER >= 12111
	public static final Identifier PAYLOAD_ID =
	//# else
	//- public static final ResourceLocation PAYLOAD_ID =
	//# end
			SpiceOfFabric.createId("sync_food_history");
	//# if MC_VERSION_NUMBER >= 12006
	public static final Type<SyncFoodHistoryS2CPacket> TYPE = new CustomPacketPayload.Type<>(PAYLOAD_ID);
	public static final StreamCodec<FriendlyByteBuf, SyncFoodHistoryS2CPacket> STREAM_CODEC =
			StreamCodec.of(SyncFoodHistoryS2CPacket::writeToBuf, SyncFoodHistoryS2CPacket::readFromBuf);
	//# end

	public static @NonNull SyncFoodHistoryS2CPacket readFromBuf(FriendlyByteBuf buf) {
		BiMap<Integer, FoodHistoryEntry> dictionary = HashBiMap.create();
		FixedLengthIntFIFOQueueWithStats recentlyEaten = new FixedLengthIntFIFOQueueWithStats(
				new ArrayFixedLengthIntFIFOQueue(SpiceOfFabric.config.food.historyLength)
		);
		Set<FoodHistoryEntry> uniqueFoodsEaten = new HashSet<>();

		for (int l = buf.readVarInt(), i = 0; i < l; i++) {
			dictionary.put(buf.readVarInt(), FoodHistoryEntry.read(buf));
		}
		for (int l = buf.readVarInt(), i = 0; i < l; i++) {
			// Using forceEnqueue here to make sure we're not running out of space and throwing an exception
			// just because of a small desync of the history length ;)
			recentlyEaten.forceEnqueue(buf.readVarInt());
		}

		if (buf.readBoolean()) {
			final int length = buf.readVarInt();
			for (int i = 0; i < length; i++) {
				uniqueFoodsEaten.add(FoodHistoryEntry.read(buf));
			}
		}

		return new SyncFoodHistoryS2CPacket(dictionary, recentlyEaten, uniqueFoodsEaten);
	}

	public static void writeToBuf(@NonNull FriendlyByteBuf buf, @NonNull SyncFoodHistoryS2CPacket packet) {
		buf.writeVarInt(packet.dictionary.size());
		for (Map.Entry<Integer, FoodHistoryEntry> entry : packet.dictionary.entrySet()) {
			buf.writeVarInt(entry.getKey());
			entry.getValue().write(buf);
		}
		buf.writeVarInt(packet.recentlyEaten.size());
		for (int integer : packet.recentlyEaten) {
			buf.writeVarInt(integer);
		}
		if (SpiceOfFabric.config.carrot.enable) {
			buf.writeBoolean(true);
			buf.writeVarInt(packet.uniqueFoodsEaten.size());
			for (FoodHistoryEntry entry : packet.uniqueFoodsEaten) {
				entry.write(buf);
			}
		} else {
			buf.writeBoolean(false);
		}
	}

	private final BiMap<Integer, FoodHistoryEntry> dictionary;
	private final FixedLengthIntFIFOQueueWithStats recentlyEaten;
	private final Set<FoodHistoryEntry> uniqueFoodsEaten;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
