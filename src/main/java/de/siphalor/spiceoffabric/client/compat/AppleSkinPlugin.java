package de.siphalor.spiceoffabric.client.compat;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.item.FoodContainerItem;
import de.siphalor.spiceoffabric.polymer.SoFPolymer;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import squeek.appleskin.api.AppleSkinApi;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.food.FoodValues;

public class AppleSkinPlugin implements AppleSkinApi {
	@Override
	public void registerEvents() {
		FoodValuesEvent.EVENT.register(event -> {
			if (event.player != null) {
				ItemStack stack = event.itemStack;
				if (FabricLoader.getInstance().isModLoaded("polymer")) {
					Identifier polymerId = SoFPolymer.getPolymerItemId(event.itemStack);
					if (polymerId != null && polymerId.getNamespace().equals(SpiceOfFabric.MOD_ID)) {
						Item item = SoFPolymer.getPolymerItem(polymerId);
						if (item instanceof FoodContainerItem containerItem) {
							NbtCompound nbt = SoFPolymer.getPolymerTag(stack.getOrCreateNbt());
							stack = new ItemStack(item);
							stack.setNbt(nbt);
							stack = processFoodContainer(event, stack, containerItem);
						}
					}
				}

				if (stack.getItem() instanceof FoodContainerItem containerItem) {
					stack = processFoodContainer(event, stack, containerItem);
				}

				int hungerValue = event.modifiedFoodValues.hunger;
				float saturationValue = event.modifiedFoodValues.saturationModifier;
				Config.setHungerExpressionValues(
						((IHungerManager) event.player.getHungerManager()).spiceOfFabric_getFoodHistory().getTimesEaten(stack),
						hungerValue, saturationValue, event.itemStack.getMaxUseTime()
				);
				event.modifiedFoodValues = new FoodValues(Config.getHungerValue(), Config.getSaturationValue());
			}
		});
	}

	protected ItemStack processFoodContainer(FoodValuesEvent event, ItemStack stack, FoodContainerItem containerItem) {
		if (!stack.hasNbt()) return stack;

		stack = containerItem.getNextFoodStack(stack, MinecraftClient.getInstance().player);
		FoodComponent foodComponent = stack.getItem().getFoodComponent();
		if (foodComponent != null) {
			event.defaultFoodValues = new FoodValues(foodComponent.getHunger(), foodComponent.getSaturationModifier());
			event.modifiedFoodValues = new FoodValues(foodComponent.getHunger(), foodComponent.getSaturationModifier());
		}
		return stack;
	}
}
