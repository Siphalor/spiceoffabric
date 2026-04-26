package de.siphalor.spiceoffabric.networking;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NonNull;

@RequiredArgsConstructor
@Getter
public class AddFoodToHistoryS2CPacket
	//# if MC_VERSION_NUMBER >= 12006
	implements CustomPacketPayload
	//# end
{
	public static final ResourceLocation PAYLOAD_ID = SpiceOfFabric.createId("add_food_to_history");
	//# if MC_VERSION_NUMBER >= 12006
	public static final Type<AddFoodToHistoryS2CPacket> TYPE = new CustomPacketPayload.Type<>(PAYLOAD_ID);
	public static final StreamCodec<FriendlyByteBuf, AddFoodToHistoryS2CPacket> STREAM_CODEC =
			StreamCodec.of(AddFoodToHistoryS2CPacket::writeToBuf, AddFoodToHistoryS2CPacket::readFromBuf);
	//# end

	public static @NonNull AddFoodToHistoryS2CPacket readFromBuf(@NonNull FriendlyByteBuf buf) {
		FoodHistoryEntry entry = FoodHistoryEntry.read(buf);
		return new AddFoodToHistoryS2CPacket(entry);
	}

	public static void writeToBuf(@NonNull FriendlyByteBuf buf, @NonNull AddFoodToHistoryS2CPacket packet) {
		packet.entry.write(buf);
	}

	private final FoodHistoryEntry entry;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
