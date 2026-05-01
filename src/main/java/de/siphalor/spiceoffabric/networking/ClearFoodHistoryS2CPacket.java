package de.siphalor.spiceoffabric.networking;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
//- import net.minecraft.resources.ResourceLocation;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ClearFoodHistoryS2CPacket
	//# if MC_VERSION_NUMBER >= 12006
	implements CustomPacketPayload
	//# end
{
	public static final ClearFoodHistoryS2CPacket INSTANCE = new ClearFoodHistoryS2CPacket();

	//# if MC_VERSION_NUMBER >= 12111
	public static final Identifier PAYLOAD_ID =
	//# else
	//- public static final ResourceLocation PAYLOAD_ID =
	//# end
			SpiceOfFabric.createId("clear_food_history");
	//# if MC_VERSION_NUMBER >= 12006
	public static final Type<ClearFoodHistoryS2CPacket> TYPE = new CustomPacketPayload.Type<>(PAYLOAD_ID);
	public static final StreamCodec<FriendlyByteBuf, ClearFoodHistoryS2CPacket> STREAM_CODEC =
			StreamCodec.unit(INSTANCE);
	//# end


	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
