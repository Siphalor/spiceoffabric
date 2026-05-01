package de.siphalor.spiceoffabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;

//# if MC_VERSION_NUMBER >= 12106
@Mixin(CustomData.class)
public interface CustomDataAccessor {
	@Accessor
	CompoundTag getTag();
}
//# end
