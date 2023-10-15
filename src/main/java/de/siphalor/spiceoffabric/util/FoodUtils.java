package de.siphalor.spiceoffabric.util;

import de.siphalor.capsaicin.api.food.FoodContext;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.item.FoodContainerItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FoodUtils {
	private static final String LAST_EATEN_BASE_TRANSLATION_KEY = SpiceOfFabric.MOD_ID + ".item.tooltip.last_eaten";
	private static final TranslatableText NEVER_EATEN_TOOLTIP = new TranslatableText(SpiceOfFabric.MOD_ID + ".item.tooltip.never_eaten");

	private FoodUtils() {
	}

	public static boolean isFood(ItemStack stack) {
		Item item = stack.getItem();
		if (item instanceof FoodContainerItem) {
			return false;
		}
		if (stack.isFood()) {
			return true;
		}
		if (item instanceof BlockItem blockItem) {
			return blockItem.getBlock() instanceof CakeBlock;
		}
		return false;
	}

	public static boolean isFood(Item item) {
		if (item instanceof FoodContainerItem) {
			return false;
		}
		if (item.isFood()) {
			return true;
		}
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

	public static void appendServerTooltips(PlayerEntity player, ItemStack stack) {
		if (!isFood(stack)) {
			return;
		}
		FoodHistory foodHistory = FoodHistory.get(player);
		if (foodHistory == null) {
			return;
		}

		var additions = new ArrayList<Text>();
		appendCarrotTooltip(additions, stack, foodHistory);
		if (additions.isEmpty()) {
			return;
		}

		NbtCompound displayNbt = stack.getOrCreateSubNbt(ItemStack.DISPLAY_KEY);
		NbtList loreNbt;
		if (displayNbt.contains(ItemStack.LORE_KEY, 9)) {
			loreNbt = displayNbt.getList(ItemStack.LORE_KEY, 8);
		} else {
			loreNbt = new NbtList();
			displayNbt.put(ItemStack.LORE_KEY, loreNbt);
		}

		for (Text addition : additions) {
			loreNbt.add(NbtString.of(Text.Serializer.toJson(addition)));
		}
	}

	public static List<Text> getClientTooltipAdditions(PlayerEntity player, ItemStack stack) {
		if (!isFood(stack)) {
			return Collections.emptyList();
		}
		FoodHistory foodHistory = FoodHistory.get(player);
		if (foodHistory == null) {
			return Collections.emptyList();
		}

		var additions = new ArrayList<Text>();
		appendCarrotTooltip(additions, stack, foodHistory);
		appendLastEatenTooltip(additions, stack, foodHistory);

		return additions;
	}

	private static void appendCarrotTooltip(List<Text> base, ItemStack stack, FoodHistory foodHistory) {
		if (SOFConfig.carrot.enable && !foodHistory.isInUniqueEaten(stack)) {
			base.add(NEVER_EATEN_TOOLTIP);
		}
	}

	private static void appendLastEatenTooltip(List<Text> base, ItemStack stack, FoodHistory foodHistory) {
		if (SOFConfig.showLastEatenTips == SOFConfig.ItemTipDisplayStyle.NONE || SOFConfig.food.historyLength <= 0) {
			return;
		}
		int lastEaten = foodHistory.getFoodCountSinceLastEaten(stack);
		if (lastEaten < 0) {
			return;
		}

		BaseText text;
		if (lastEaten == 0) {
			text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".simple.last", lastEaten);
		} else if (lastEaten == 1) {
			text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".simple.one", lastEaten);
		} else {
			text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".simple", lastEaten);
		}

		if (SOFConfig.showLastEatenTips == SOFConfig.ItemTipDisplayStyle.EXTENDED) {
			int left = SOFConfig.food.historyLength - lastEaten;
			if (left == 1) {
				text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".extended.one", text, SOFConfig.food.historyLength - lastEaten);
			} else {
				text = new TranslatableText(LAST_EATEN_BASE_TRANSLATION_KEY + ".extended", text, SOFConfig.food.historyLength - lastEaten);
			}
		}

		for (String line : StringUtils.split(text.getString(), '\n')) {
			base.add(new LiteralText(line).styled(style -> style.withColor(Formatting.GRAY).withItalic(true)));
		}
	}
}
