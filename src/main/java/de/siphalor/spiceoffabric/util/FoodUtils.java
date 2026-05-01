package de.siphalor.spiceoffabric.util;

import de.siphalor.capsaicin.api.food.CamoFoodItem;
import de.siphalor.capsaicin.api.food.FoodContext;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.item.FoodContainerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//- import net.minecraft.nbt.CompoundTag;
//- import net.minecraft.nbt.ListTag;
//- import net.minecraft.nbt.StringTag;

public class FoodUtils {
	private static final String LAST_EATEN_BASE_TRANSLATION_KEY = SpiceOfFabric.MOD_ID + ".item.tooltip.last_eaten";
	private static final Component NEVER_EATEN_TOOLTIP = Component.translatable(SpiceOfFabric.MOD_ID + ".item.tooltip.never_eaten");

	private FoodUtils() {
	}

	public static boolean isFood(ItemStack stack) {
		Item item = stack.getItem();
		if (item instanceof CamoFoodItem) {
			return false;
		}
		//# if MC_VERSION_NUMBER >= 12006
		if (stack.has(DataComponents.FOOD)) {
			return true;
		}
		//# else
		//- if (stack.isEdible()) {
		//- 	return true;
		//- }
		//# end
		if (item instanceof BlockItem blockItem) {
			return blockItem.getBlock() instanceof CakeBlock;
		}
		return false;
	}

	public static boolean isFood(Item item) {
		if (item instanceof FoodContainerItem) {
			return false;
		}
		//# if MC_VERSION_NUMBER >= 12006
		if (item.components().has(DataComponents.FOOD)) {
			//# if MC_VERSION_NUMBER >= 12102
			return item.components().has(DataComponents.CONSUMABLE);
			//# else
			//- return true;
			//# end
		}
		//# else
		//- if (item.isEdible()) {
		//- 	return true;
		//- }
		//# end
		if (item instanceof BlockItem blockItem) {
			return blockItem.getBlock() instanceof CakeBlock;
		}
		return false;
	}

	public static @Nullable ItemStack getFoodStack(FoodContext context) {
		ItemStack stack = context.stack();
		if (stack != null) {
			return stack;
		}
		BlockState blockState = context.blockState();
		if (blockState != null) {
			Item item = blockState.getBlock().asItem();
			if (item != null) {
				return new ItemStack(item);
			}
		}
		return null;
	}

	public static void appendServerTooltips(Player player, ItemStack stack) {
		if (!isFood(stack)) {
			return;
		}
		FoodHistory foodHistory = FoodHistory.get(player);
		if (foodHistory == null) {
			return;
		}

		var additions = new ArrayList<Component>();
		appendCarrotTooltip(additions, stack, foodHistory);
		if (additions.isEmpty()) {
			return;
		}

		//# if MC_VERSION_NUMBER >= 12006
		stack.update(DataComponents.LORE, ItemLore.EMPTY, itemLore -> {
			for (Component addition : additions) {
				itemLore = itemLore.withLineAdded(addition);
			}
			return itemLore;
		});
		//# else
		//- CompoundTag displayNbt = stack.getOrCreateTagElement(ItemStack.TAG_DISPLAY);
		//- ListTag loreNbt;
		//- if (displayNbt.contains(ItemStack.TAG_LORE, 9)) {
		//- 	loreNbt = displayNbt.getList(ItemStack.TAG_LORE, 8);
		//- } else {
		//- 	loreNbt = new ListTag();
		//- 	displayNbt.put(ItemStack.TAG_LORE, loreNbt);
		//- }

		//- for (Component addition : additions) {
		//- 	loreNbt.add(StringTag.valueOf(Component.Serializer.toJson(addition)));
		//- }
		//# end
	}

	public static List<Component> getClientTooltipAdditions(Player player, ItemStack stack) {
		if (!isFood(stack)) {
			return Collections.emptyList();
		}
		FoodHistory foodHistory = FoodHistory.get(player);
		if (foodHistory == null) {
			return Collections.emptyList();
		}

		var additions = new ArrayList<Component>();
		appendCarrotTooltip(additions, stack, foodHistory);
		appendLastEatenTooltip(additions, stack, foodHistory);

		return additions;
	}

	private static void appendCarrotTooltip(List<Component> base, ItemStack stack, FoodHistory foodHistory) {
		if (SpiceOfFabric.config.carrot.enable && !foodHistory.isInUniqueEaten(stack)) {
			base.add(NEVER_EATEN_TOOLTIP);
		}
	}

	private static void appendLastEatenTooltip(List<Component> base, ItemStack stack, FoodHistory foodHistory) {
		int historyLength = SpiceOfFabric.config.food.historyLength;
		if (SpiceOfFabric.config.showLastEatenTips == SOFConfig.ItemTipDisplayStyle.NONE
				|| historyLength <= 0) {
			return;
		}
		int lastEaten = foodHistory.getFoodCountSinceLastEaten(stack);
		if (lastEaten < 0) {
			return;
		}

		Component text;
		if (lastEaten == 0) {
			text = Component.translatable(LAST_EATEN_BASE_TRANSLATION_KEY + ".simple.last", lastEaten);
		} else if (lastEaten == 1) {
			text = Component.translatable(LAST_EATEN_BASE_TRANSLATION_KEY + ".simple.one", lastEaten);
		} else {
			text = Component.translatable(LAST_EATEN_BASE_TRANSLATION_KEY + ".simple", lastEaten);
		}

		if (SpiceOfFabric.config.showLastEatenTips == SOFConfig.ItemTipDisplayStyle.EXTENDED) {
			int left = historyLength - lastEaten;
			if (left == 1) {
				text = Component.translatable(LAST_EATEN_BASE_TRANSLATION_KEY + ".extended.one", text, 1);
			} else {
				text = Component.translatable(LAST_EATEN_BASE_TRANSLATION_KEY + ".extended", text, historyLength - lastEaten);
			}
		}

		for (String line : StringUtils.split(text.getString(), '\n')) {
			base.add(Component.literal(line).withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
		}
	}
}
