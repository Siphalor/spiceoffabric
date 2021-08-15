package de.siphalor.spiceoffabric.client.compat;

import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.IHungerManager;
import squeek.appleskin.api.AppleSkinApi;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.food.FoodValues;

public class AppleSkinPlugin implements AppleSkinApi {
	@Override
	public void registerEvents() {
		FoodValuesEvent.EVENT.register(event -> {
			if (event.player != null) {
				Config.setHungerExpressionValues(
						((IHungerManager) event.player.getHungerManager()).spiceOfFabric_getFoodHistory().getTimesEaten(event.itemStack),
						event.modifiedFoodValues.hunger, event.modifiedFoodValues.saturationModifier, event.itemStack.getMaxUseTime()
				);
				event.modifiedFoodValues = new FoodValues(Config.getHungerValue(), Config.getSaturationValue());
			}
		});
	}
}
