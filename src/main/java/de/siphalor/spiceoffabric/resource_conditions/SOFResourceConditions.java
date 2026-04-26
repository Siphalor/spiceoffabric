package de.siphalor.spiceoffabric.resource_conditions;

//- import com.google.gson.JsonElement;
//- import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
//- import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
//- import net.minecraft.util.GsonHelper;
//- import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SOFResourceConditions {
	public static final ResourceLocation REGISTRY_POPULATED_ID = SpiceOfFabric.createId("registry_populated");

	public static void init() {
		//# if MC_VERSION_NUMBER >= 12006
		ResourceConditions.register(RegistryPopulated.TYPE);
		//# else
		//- ResourceConditions.register(REGISTRY_POPULATED_ID, optionsJson -> {
		//- 	ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(optionsJson, "registry"));
		//- 	Registry<?> registry = BuiltInRegistries.REGISTRY.get(id);
		//- 	if (registry == null) {
		//- 		throw new JsonSyntaxException(id + " is not a valid registry!");
		//- 	}
		//- 	for (JsonElement elementJson : GsonHelper.getAsJsonArray(optionsJson, "ids")) {
		//- 		ResourceLocation elementId = new ResourceLocation(GsonHelper.convertToString(elementJson, "id"));
		//- 		if (!registry.containsKey(elementId)) {
		//- 			return false;
		//- 		}
		//- 	}
		//- 	return true;
		//- });
		//# end
	}

	//# if MC_VERSION_NUMBER >= 12006
	private record RegistryPopulated(
			Registry<?> registry,
			List<ResourceLocation> ids
	) implements ResourceCondition {
		@SuppressWarnings("unchecked")
		public static MapCodec<RegistryPopulated> MAP_CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						((Codec<Registry<?>>) BuiltInRegistries.REGISTRY.byNameCodec())
								.fieldOf("registry")
								.forGetter(RegistryPopulated::registry),
						Codec.list(ResourceLocation.CODEC).fieldOf("ids").forGetter(RegistryPopulated::ids)
				).apply(instance, RegistryPopulated::new)
		);

		public static final ResourceConditionType<RegistryPopulated> TYPE =
				ResourceConditionType.create(REGISTRY_POPULATED_ID, MAP_CODEC);

		@Override
		public ResourceConditionType<?> getType() {
			return TYPE;
		}

		@Override
		//# if MC_VERSION_NUMBER >= 12102
		public boolean test(RegistryOps.RegistryInfoLookup registryLookup) {
		//# else
		//- public boolean test(HolderLookup.@Nullable Provider registryLookup) {
		//# end
			for (ResourceLocation id : ids) {
				if (!registry.containsKey(id)) {
					return false;
				}
			}
			return true;
		}
	}
	//# end
}
