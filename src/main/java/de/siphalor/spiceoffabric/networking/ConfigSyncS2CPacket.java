package de.siphalor.spiceoffabric.networking;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.config.SOFTweedAttributes;
import de.siphalor.tweed5.attributesextension.api.serde.filter.AttributesReadWriteFilterExtension;
import de.siphalor.tweed5.core.api.container.ConfigContainer;
import de.siphalor.tweed5.defaultextensions.patch.api.PatchExtension;
import de.siphalor.tweed5.defaultextensions.patch.api.PatchInfo;
import de.siphalor.tweed5.minecraft.networking.api.ByteBufReader;
import de.siphalor.tweed5.minecraft.networking.api.SlightlyCompressedByteBufWriter;
import de.siphalor.tweed5.patchwork.api.Patchwork;
import de.siphalor.tweed5.serde.extension.api.ReadWriteExtension;
import de.siphalor.tweed5.serde.extension.api.TweedEntryReadException;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
//- import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

import static de.siphalor.tweed5.serde.extension.api.ReadWriteExtension.write;

@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ConfigSyncS2CPacket
	//# if MC_VERSION_NUMBER >= 12006
	implements CustomPacketPayload
	//# end
{
	//# if MC_VERSION_NUMBER >= 12111
	public static final Identifier PAYLOAD_ID =
	//# else
	//- public static final ResourceLocation PAYLOAD_ID =
	//# end
			SpiceOfFabric.createId("config_sync");
	//# if MC_VERSION_NUMBER >= 12006
	public static final Type<ConfigSyncS2CPacket> TYPE = new Type<>(PAYLOAD_ID);
	public static final StreamCodec<ByteBuf, ConfigSyncS2CPacket> STREAM_CODEC =
			StreamCodec.of(ConfigSyncS2CPacket::writeToBuf, ConfigSyncS2CPacket::readFromBuf);
	//# end

	public static @NonNull ConfigSyncS2CPacket readFromBuf(ByteBuf buf) {
		ConfigContainer<SOFConfig> configContainer = SpiceOfFabric.configContainerHelper.configContainer();
		ReadWriteExtension rwExtension = configContainer.extension(ReadWriteExtension.class).orElseThrow();
		AttributesReadWriteFilterExtension filterExtension = configContainer.extension(
				AttributesReadWriteFilterExtension.class).orElseThrow();
		PatchExtension patchExtension = configContainer.extension(PatchExtension.class).orElseThrow();

		Patchwork extensionsData = rwExtension.createReadWriteContextExtensionsData();
		PatchInfo patchInfo = patchExtension.collectPatchInfo(extensionsData);
		filterExtension.addFilter(extensionsData, SOFTweedAttributes.SYNCED, SOFTweedAttributes.SYNCED_S2C);

		try {
			var result = rwExtension.read(new ByteBufReader(buf), configContainer.rootEntry(), extensionsData);
			if (!result.hasValue()) {
				log.error("Failed to read config from server: {}", Arrays.toString(result.issues()));
			} else {
				SOFConfig config = result.value();
				return new ConfigSyncS2CPacket(config, patchInfo);
			}
		} catch (TweedEntryReadException e) {
			log.error("Failed to read config from server", e);
		}
		return new ConfigSyncS2CPacket(null);
	}

	public static void writeToBuf(@NonNull ByteBuf buf, @NonNull ConfigSyncS2CPacket packet) {
		ConfigContainer<SOFConfig> configContainer = SpiceOfFabric.configContainerHelper.configContainer();
		AttributesReadWriteFilterExtension filterExtension = configContainer.extension(
				AttributesReadWriteFilterExtension.class).orElseThrow();

		configContainer.rootEntry().apply(write(new SlightlyCompressedByteBufWriter(buf), packet.config, extensionsData ->
				filterExtension.addFilter(extensionsData, SOFTweedAttributes.SYNCED, SOFTweedAttributes.SYNCED_S2C)
		));
	}

	private final SOFConfig config;
	private PatchInfo patchInfo;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
