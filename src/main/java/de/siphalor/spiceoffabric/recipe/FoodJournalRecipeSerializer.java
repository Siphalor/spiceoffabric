package de.siphalor.spiceoffabric.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;

public class FoodJournalRecipeSerializer implements RecipeSerializer<ShapelessRecipe> {
	@Override
	public ShapelessRecipe read(Identifier id, JsonObject json) {
		DefaultedList<Ingredient> ingredients = DefaultedList.of();
		for (JsonElement ingredientJson : JsonHelper.getArray(json, "ingredients")) {
			ingredients.add(Ingredient.fromJson(ingredientJson));
		}
		ItemStack foodJournal = SpiceOfFabric.createFoodJournalStack();
		return new ShapelessRecipe(id, "", foodJournal, ingredients);
	}

	@Override
	public ShapelessRecipe read(Identifier id, PacketByteBuf buf) {
		return null;
	}

	@Override
	public void write(PacketByteBuf buf, ShapelessRecipe recipe) {

	}
}
