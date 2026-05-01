package de.siphalor.spiceoffabric.container;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record FoodContainerTooltip(
		int maxSlots,
		int filledSlots,
		int itemCount
) implements TooltipComponent {
}
