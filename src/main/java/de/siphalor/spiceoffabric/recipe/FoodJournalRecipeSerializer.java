package de.siphalor.spiceoffabric.recipe;

//- import de.siphalor.spiceoffabric.SpiceOfFabric;

//- import com.mojang.serialization.Codec;
//- import com.mojang.serialization.DataResult;
//- import com.mojang.serialization.MapCodec;
//- import com.mojang.serialization.codecs.RecordCodecBuilder;
//- import net.minecraft.core.NonNullList;
//- import net.minecraft.network.FriendlyByteBuf;
//- import net.minecraft.network.RegistryFriendlyByteBuf;
//- import net.minecraft.network.codec.StreamCodec;
//- import net.minecraft.util.ExtraCodecs;
//- import net.minecraft.world.item.crafting.CraftingBookCategory;
//- import net.minecraft.world.item.crafting.Ingredient;
//- import net.minecraft.world.item.crafting.RecipeSerializer;
//- import net.minecraft.world.item.crafting.ShapelessRecipe;

//# if MC_VERSION_NUMBER < 12100
//- public class FoodJournalRecipeSerializer implements RecipeSerializer<ShapelessRecipe> {
//- 	//# if MC_VERSION_NUMBER >= 12006
//- 	private static final MapCodec<ShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec((instance) ->
//- 	//# else
//- 	private static final Codec<ShapelessRecipe> CODEC = RecordCodecBuilder.create((instance) ->
//- 	//# end
//- 			instance.group(
//- 					//# if MC_VERSION_NUMBER >= 12006
//- 					Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
//- 					//# else
//- 					ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(ShapelessRecipe::getGroup),
//- 					//# end
//- 					CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
//- 					RecordCodecBuilder.point(SpiceOfFabric.createFoodJournalStack()),
//- 					Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
//- 							.flatXmap(ingredients -> DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients.toArray(Ingredient[]::new))), DataResult::success)
//- 							.forGetter(ShapelessRecipe::getIngredients)
//- 			).apply(instance, ShapelessRecipe::new)
//- 	);

//- 	//# if MC_VERSION_NUMBER >= 12006
//- 	private static final StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> STREAM_CODEC = StreamCodec.unit(null);

//- 	@Override
//- 	public MapCodec<ShapelessRecipe> codec() {
//- 		return CODEC;
//- 	}

//- 	@Override
//- 	public StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> streamCodec() {
//- 		return STREAM_CODEC;
//- 	}
//- 	//# else
//- 	@Override
//- 	public Codec<ShapelessRecipe> codec() {
//- 		return CODEC;
//- 	}

//- 	@Override
//- 	public ShapelessRecipe fromNetwork(FriendlyByteBuf buf) {
//- 		return null;
//- 	}

//- 	@Override
//- 	public void toNetwork(FriendlyByteBuf buffer, ShapelessRecipe recipe) {

//- 	}
//- 	//# end
//- }
//# end
