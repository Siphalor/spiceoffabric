package de.siphalor.spiceoffabric.mixin.compat.fabric_registry_sync_v0;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.siphalor.spiceoffabric.recipe.FoodJournalRecipeSerializer;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RegistrySyncManager.class)
public class MixinRegistrySyncManager {
	@WrapOperation(method = "createAndPopulateRegistryMap", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/Registry;getKey(Ljava/lang/Object;)Lnet/minecraft/resources/ResourceLocation;"
	))
	private static ResourceLocation resolveRegistryKeyForSync(
			Registry<?> instance,
			Object value,
			Operation<ResourceLocation> original
	) {
		if (value instanceof FoodJournalRecipeSerializer) {
			// returning null here will skip the sync of this registry entry
			return null;
		} else {
			return original.call(instance, value);
		}
	}
}
