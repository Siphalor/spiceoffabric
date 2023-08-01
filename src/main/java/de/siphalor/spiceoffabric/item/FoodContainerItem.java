package de.siphalor.spiceoffabric.item;

import com.mojang.datafixers.util.Pair;
import de.siphalor.capsaicin.api.food.CamoFoodContext;
import de.siphalor.capsaicin.api.food.CamoFoodItem;
import de.siphalor.capsaicin.api.food.DynamicFoodPropertiesAccess;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.container.FoodContainerScreenHandler;
import de.siphalor.spiceoffabric.container.ItemStackInventory;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import de.siphalor.spiceoffabric.util.IndexedValue;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FoodContainerItem extends Item implements CamoFoodItem {
	private static final String INVENTORY_NBT_KEY = "inventory";
	private static final Style LORE_STYLE = Style.EMPTY.withColor(Formatting.GRAY).withItalic(false);
	private static final Text LORE_EMPTY = Text.translatable(SpiceOfFabric.MOD_ID + ".food_container.lore.empty").setStyle(LORE_STYLE);
	private static final String LORE_GENERAL_KEY = SpiceOfFabric.MOD_ID + ".food_container.lore.general";
	private static final IndexedValue<ItemStack> NO_STACK = new IndexedValue<>(-1, ItemStack.EMPTY);

	private final ScreenHandlerType<?> screenHandlerType;
	private final int size;

	public FoodContainerItem(Settings settings, int size, ScreenHandlerType<?> screenHandlerType) {
		super(settings);
		this.screenHandlerType = screenHandlerType;
		this.size = size;
	}

	public ScreenHandlerType<?> getScreenHandlerType() {
		return screenHandlerType;
	}

	public int getSize() {
		return size;
	}

	public ItemStack getNextFoodStack(ItemStack stack, PlayerEntity player) {
		return getNextFoodStack(getInventory(stack), player).value();
	}

	private IndexedValue<ItemStack> getNextFoodStack(ItemStackInventory inventory, PlayerEntity player) {
		FoodHistory foodHistory = FoodHistory.get(player);
		if (foodHistory == null) {
			return NO_STACK;
		}

		var filteredInv = new ArrayList<IndexedValue<Pair<ItemStack, FoodComponent>>>(inventory.size());
		var foodPropertiesAccess = DynamicFoodPropertiesAccess.create();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isEmpty()) {
				continue;
			}
			FoodComponent foodComponent = foodPropertiesAccess.withStack(stack).getModifiedFoodComponent();
			if (foodComponent == null) {
				SpiceOfFabric.LOGGER.warn("Non-food stack " + stack + " found in food container " + this);
				continue;
			}
			if (stack.isEmpty() || !player.canConsume(foodComponent.isAlwaysEdible())) {
				continue;
			}

			filteredInv.add(new IndexedValue<>(i, Pair.of(stack, foodComponent)));
		}
		if (filteredInv.isEmpty()) {
			return NO_STACK;
		}

		int requiredFood = 20 - player.getHungerManager().getFoodLevel();
		return findMostAppropriateFood(filteredInv, requiredFood);
	}

	private IndexedValue<ItemStack> findMostAppropriateFood(List<IndexedValue<Pair<ItemStack, FoodComponent>>> foods, int requiredFood) {
		var bestStack = NO_STACK;
		int bestDelta = Integer.MAX_VALUE;
		int bestConsumeTime = Integer.MAX_VALUE;
		for (var value : foods) {
			ItemStack stack = value.value().getFirst();
			int delta = requiredFood - value.value().getSecond().getHunger();
			int consumeTime = stack.getMaxUseTime();
			if (delta <= 0) {
				if (delta > bestDelta || bestDelta > 0 || (delta == bestDelta && consumeTime < bestConsumeTime)) {
					bestDelta = delta;
					bestConsumeTime = consumeTime;
					bestStack = new IndexedValue<>(value.index(), stack);
				}
			} else if (bestDelta > 0 && (delta < bestDelta || delta == bestDelta && consumeTime < bestConsumeTime)) {
				bestDelta = delta;
				bestConsumeTime = consumeTime;
				bestStack = new IndexedValue<>(value.index(), stack);
			}
		}
		return bestStack;
	}

	public boolean isInventoryEmpty(ItemStack stack) {
		return getInventory(stack).isEmpty();
	}

	public ItemStackInventory getInventory(ItemStack stack) {
		return new ItemStackInventory(stack, INVENTORY_NBT_KEY, size);
	}

	@Override
	public void onItemEntityDestroyed(ItemEntity entity) {
		ItemStackInventory inventory = getInventory(entity.getStack());
		ItemUsage.spawnItemContents(entity, inventory.getContainedStacks().stream());
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		ItemStackInventory inventory = getInventory(stack);
		if (inventory.isEmpty()) {
			tooltip.add(LORE_EMPTY);
		} else {
			int count = 0;
			int filled = 0;
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack invStack = inventory.getStack(i);
				if (!invStack.isEmpty()) {
					count += invStack.getCount();
					filled++;
				}
			}
			tooltip.add(Text.translatable(LORE_GENERAL_KEY, filled, size, count).setStyle(LORE_STYLE));
		}
	}

	@Override
	public boolean isFood() {
		return true;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stackInHand = user.getStackInHand(hand);
		ItemStack nextFoodItem = getNextFoodStack(stackInHand, user);
		if (nextFoodItem.isEmpty()) {
			openScreen(stackInHand, user, hand == Hand.MAIN_HAND ? user.getInventory().selectedSlot : PlayerInventory.OFF_HAND_SLOT);
			return TypedActionResult.success(stackInHand);
		} else if (nextFoodItem.isFood()) {
			FoodComponent foodComponent = nextFoodItem.getItem().getFoodComponent();
			if (foodComponent != null && user.canConsume(foodComponent.isAlwaysEdible())) {
				user.setCurrentHand(hand);
				return TypedActionResult.consume(stackInHand);
			}
			return TypedActionResult.fail(stackInHand);
		}
		return TypedActionResult.pass(stackInHand);
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		openContainer:
		if (!world.isClient && user instanceof ServerPlayerEntity player) {
			// Only open the container if the player hasn't used the item for too long
			int maxUseTime = getMaxUseTime(stack);
			if (maxUseTime - remainingUseTicks > 5) {
				break openContainer;
			}

			// Prevent opening the container directly after eating
			long lastEatTime = ((IServerPlayerEntity) user).spiceOfFabric_getLastContainerEatTime();
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastEatTime < 800) {
				break openContainer;
			}
			((IServerPlayerEntity) user).spiceOfFabric_setLastContainerEatTime(currentTime);

			PlayerInventory inv = player.getInventory();
			for (int i = 0; i < inv.size(); i++) {
				if (inv.getStack(i) == stack) {
					openScreen(stack, player, i);
					return;
				}
			}
		}
		super.onStoppedUsing(stack, world, user, remainingUseTicks);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!(user instanceof PlayerEntity player)) {
			return stack;
		}

		ItemStackInventory inventory = getInventory(stack);
		var foodStack = getNextFoodStack(inventory, player);
		if (foodStack.value().isEmpty()) {
			return stack;
		}

		ItemStack newStack = foodStack.value().finishUsing(world, user);
		if (newStack != foodStack.value()) {
			if (inventory.isValid(foodStack.index(), newStack)) {
				inventory.setStack(foodStack.index(), newStack);
			} else {
				player.getInventory().offerOrDrop(newStack);
				inventory.removeStack(foodStack.index());
			}
		} else {
			inventory.markDirty();
		}

		return stack;
	}

	protected void openScreen(ItemStack stack, PlayerEntity user, int invIndex) {
		user.clearActiveItem();
		user.openHandledScreen(new FoodContainerScreenHandler.Factory(this, stack));
		user.currentScreenHandler.addListener(new ScreenHandlerListener() {
			@Override
			public void onSlotUpdate(ScreenHandler handler, int updateSlotId, ItemStack updateStack) {
				Slot updateSlot = handler.getSlot(updateSlotId);
				if (!(user instanceof ServerPlayerEntity serverPlayer)) {
					return;
				}

				if (updateSlot.getIndex() == invIndex && updateSlot.inventory == user.getInventory()) {
					if (updateStack.isEmpty() || !ItemStack.areEqual(updateStack, stack)) {
						closeScreen(serverPlayer);
					}
				} else {
					if (ItemStack.areEqual(updateStack, stack)) {
						closeScreen(serverPlayer);
					}
				}
			}

			@Override
			public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
				// N/A
			}
		});
	}

	private static void closeScreen(ServerPlayerEntity player) {
		player.closeHandledScreen();
	}

	@Override
	public @Nullable ItemStack getCamoFoodStack(@NotNull ItemStack stack, CamoFoodContext context) {
		if (context.user() instanceof PlayerEntity player) {
			return getNextFoodStack(stack, player);
		}
		return stack;
	}
}
