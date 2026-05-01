package de.siphalor.spiceoffabric.resource_conditions;

//- import com.google.gson.JsonElement;
//- import com.google.gson.JsonSyntaxException;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import java.util.List;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
//- import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//- import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
//- import net.minecraft.resources.ResourceLocation;
//- import net.minecraft.util.GsonHelper;

public class SOFResourceConditions {
	//# if MC_VERSION_NUMBER >= 12111
	public static final Identifier REGISTRY_POPULATED_ID =
	//# else
	//- public static final ResourceLocation REGISTRY_POPULATED_ID =
	//# end
			SpiceOfFabric.createId("registry_populated");

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
			//# if MC_VERSION_NUMBER >= 12111
			List<Identifier> ids
			//# else
			//- List<ResourceLocation> ids
			//# end
	) implements ResourceCondition {
		@SuppressWarnings("unchecked")
		public static MapCodec<RegistryPopulated> MAP_CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						((Codec<Registry<?>>) BuiltInRegistries.REGISTRY.byNameCodec())
								.fieldOf("registry")
								.forGetter(RegistryPopulated::registry),
						//# if MC_VERSION_NUMBER >= 12111
						Codec.list(Identifier.CODEC)
						//# else
						//- Codec.list(ResourceLocation.CODEC)
						//# end
								.fieldOf("ids").forGetter(RegistryPopulated::ids)
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
			//# if MC_VERSION_NUMBER >= 12111
			for (Identifier id : ids) {
			//# else
			//- for (ResourceLocation id : ids) {
			//# end
				if (!registry.containsKey(id)) {
					return false;
				}
			}
			return true;
		}
	}
	//# end
}
