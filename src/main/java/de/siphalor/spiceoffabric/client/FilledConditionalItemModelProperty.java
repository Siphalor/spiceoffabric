package de.siphalor.spiceoffabric.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

//# if MC_VERSION_NUMBER >= 12104
public record FilledConditionalItemModelProperty() implements ConditionalItemModelProperty {
	private static final FilledConditionalItemModelProperty INSTANCE = new FilledConditionalItemModelProperty();
	public static final MapCodec<FilledConditionalItemModelProperty> CODEC =
			MapCodec.unit(INSTANCE);

	@Override
	public boolean get(
			ItemStack stack,
			@Nullable ClientLevel level,
			@Nullable LivingEntity entity,
			int seed,
			ItemDisplayContext displayContext
	) {
		ItemContainerContents container = stack.get(DataComponents.CONTAINER);
		if (container == null) {
			return false;
		}
		return container.nonEmptyItems().iterator().hasNext();
	}

	@Override
	public MapCodec<? extends ConditionalItemModelProperty> type() {
		return CODEC;
	}
}
//# end
