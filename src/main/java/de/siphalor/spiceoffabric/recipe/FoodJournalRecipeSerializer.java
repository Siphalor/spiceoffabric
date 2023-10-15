package de.siphalor.spiceoffabric.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;

public class FoodJournalRecipeSerializer implements RecipeSerializer<ShapelessRecipe> {
	private static final Codec<ShapelessRecipe> CODEC = RecordCodecBuilder.create((instance) ->
			instance.group(
					Codecs.createStrictOptionalFieldCodec(Codec.STRING, "group", "").forGetter(ShapelessRecipe::getGroup),
					CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapelessRecipe::getCategory),
					RecordCodecBuilder.point(SpiceOfFabric.createFoodJournalStack()),
					Ingredient.DISALLOW_EMPTY_CODEC.listOf().fieldOf("ingredients")
							.flatXmap(ingredients -> DataResult.success(DefaultedList.copyOf(Ingredient.EMPTY, ingredients.toArray(Ingredient[]::new))), DataResult::success)
							.forGetter(ShapelessRecipe::getIngredients)
			).apply(instance, ShapelessRecipe::new)
	);

	@Override
	public Codec<ShapelessRecipe> codec() {
		return CODEC;
	}

	@Override
	public ShapelessRecipe read(PacketByteBuf buf) {
		return null;
	}

	@Override
	public void write(PacketByteBuf buf, ShapelessRecipe recipe) {

	}
}
