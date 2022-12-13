package de.siphalor.spiceoffabric.item;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IndexedValue;
import de.siphalor.spiceoffabric.util.MaxUseTimeCalculator;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FoodContainerItem extends Item {
	private static final String INVENTORY_NBT_KEY = "inventory";
	private static final Style LORE_STYLE = Style.EMPTY.withColor(Formatting.GRAY).withItalic(false);
	private static final Text LORE_EMPTY = new TranslatableText(SpiceOfFabric.MOD_ID + ".food_container.lore.empty").setStyle(LORE_STYLE);
	private static final String LORE_GENERAL_KEY = SpiceOfFabric.MOD_ID + ".food_container.lore.general";
	private static final IndexedValue<ItemStack> NO_STACK = new IndexedValue<>(-1, ItemStack.EMPTY);
	private static final FoodComponent EMPTY_FOOD_COMPONENT = new FoodComponent.Builder().build();

	private final ScreenHandlerType<?> screenHandlerType;
	private final int size;

	public FoodContainerItem(Settings settings, int size, ScreenHandlerType<?> screenHandlerType) {
		super(settings);
		this.screenHandlerType = screenHandlerType;
		this.size = size;
	}

	public ItemStack getNextFoodStack(ItemStack stack, PlayerEntity player) {
		return getNextFoodStack(getInventory(stack), player).value();
	}

	private IndexedValue<ItemStack> getNextFoodStack(StackInventory inventory, PlayerEntity player) {
		FoodHistory foodHistory = FoodHistory.get(player);
		if (foodHistory == null) {
			return NO_STACK;
		}

		var filteredInv = new ArrayList<IndexedValue<ItemStack>>(inventory.size());
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isEmpty()) {
				continue;
			}
			FoodComponent foodComponent = stack.getItem().getFoodComponent();
			if (foodComponent == null) {
				SpiceOfFabric.LOGGER.warn("Non-food stack " + stack + " found in food container " + this);
				continue;
			}
			if (!stack.isEmpty() && player.canConsume(foodComponent.isAlwaysEdible())) {
				filteredInv.add(new IndexedValue<>(i, stack));
			}
		}
		if (filteredInv.isEmpty()) {
			return NO_STACK;
		}

		int reqFood = 20 - player.getHungerManager().getFoodLevel();
		var bestStack = NO_STACK;
		int bestDelta = Integer.MAX_VALUE;
		int bestConsumeTime = Integer.MAX_VALUE;
		for (IndexedValue<ItemStack> value : filteredInv) {
			ItemStack stack = value.value();
			FoodComponent foodComponent = stack.getItem().getFoodComponent();
			if (foodComponent == null) {
				continue;
			}
			Config.setHungerExpressionValues(foodHistory.getTimesEaten(stack), foodComponent.getHunger(), foodComponent.getSaturationModifier(), stack.getMaxUseTime());
			int delta = reqFood - Config.getHungerValue();
			int consumeTime = (int) Config.consumeDurationExpression.evaluate();
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

	protected StackInventory getInventory(ItemStack stack) {
		return new StackInventory(stack);
	}

	@Override
	public void onItemEntityDestroyed(ItemEntity entity) {
		StackInventory inventory = getInventory(entity.getStack());
		ItemUsage.spawnItemContents(entity, inventory.stacks.stream());
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		StackInventory inventory = getInventory(stack);
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
			tooltip.add(new TranslatableText(LORE_GENERAL_KEY, filled, size, count).setStyle(LORE_STYLE));
		}
	}

	@Override
	public boolean isFood() {
		return true;
	}

	@Nullable
	@Override
	public FoodComponent getFoodComponent() {
		return EMPTY_FOOD_COMPONENT;
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return getNextFoodStack(stack, MaxUseTimeCalculator.currentPlayer).getMaxUseTime();
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stackInHand = user.getStackInHand(hand);
		ItemStack nextFoodItem = getNextFoodStack(stackInHand, user);
		if (nextFoodItem.isEmpty()) {
			openScreen(stackInHand, user, hand == Hand.MAIN_HAND ? user.getInventory().selectedSlot : PlayerInventory.OFF_HAND_SLOT);
			return TypedActionResult.success(stackInHand);
		} else if (nextFoodItem.isFood()) {
			if (user.canConsume(nextFoodItem.getItem().getFoodComponent().isAlwaysEdible())) {
				user.setCurrentHand(hand);
				return TypedActionResult.consume(stackInHand);
			}
			return TypedActionResult.fail(stackInHand);
		}
		return TypedActionResult.pass(stackInHand);
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (user instanceof PlayerEntity player) {
			int maxUseTime = getMaxUseTime(stack);
			if (maxUseTime - remainingUseTicks <= 5) {
				if (!world.isClient) {
					PlayerInventory inv = ((PlayerEntity) user).getInventory();
					for (int i = 0; i < inv.size(); i++) {
						if (inv.getStack(i) == stack) {
							openScreen(stack, player, i);
							break;
						}
					}
				}
				return;
			}
		}
		super.onStoppedUsing(stack, world, user, remainingUseTicks);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!(user instanceof PlayerEntity player)) {
			return stack;
		}

		StackInventory inventory = getInventory(stack);
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
		user.openHandledScreen(new ScreenHandlerFactory(stack));
		user.currentScreenHandler.addListener(new ScreenHandlerListener() {
			@Override
			public void onSlotUpdate(ScreenHandler handler, int updateSlotId, ItemStack updateStack) {
				Slot updateSlot = handler.getSlot(updateSlotId);
				if (user instanceof ServerPlayerEntity serverPlayer) {
					if (updateSlot.getIndex() == invIndex && updateSlot.inventory == user.getInventory()) {
						if (updateStack.isEmpty()) {
							closeScreen(serverPlayer);
						} else if (!ItemStack.areEqual(updateStack, stack)) {
							closeScreen(serverPlayer);
						}
					} else {
						if (ItemStack.areEqual(updateStack, stack)) {
							closeScreen(serverPlayer);
						}
					}
					if (updateSlot.inventory == user.getInventory() && updateSlot.getIndex() == invIndex && !ItemStack.areEqual(updateStack, stack)) {
						closeScreen(serverPlayer);
					} else if (updateSlot.getIndex() != invIndex && ItemStack.areEqual(updateStack, stack)) {
						closeScreen(serverPlayer);
					}
				}
			}

			@Override
			public void onPropertyUpdate(ScreenHandler handler, int property, int value) {

			}
		});
	}

	private static void closeScreen(ServerPlayerEntity player) {
		player.closeHandledScreen();
	}

	protected class StackInventory implements Inventory {
		private final ItemStack containerStack;
		private final DefaultedList<ItemStack> stacks;

		StackInventory(ItemStack containerStack) {
			this.containerStack = containerStack;
			stacks = DefaultedList.ofSize(size, ItemStack.EMPTY);
			Inventories.readNbt(containerStack.getOrCreateSubNbt(INVENTORY_NBT_KEY), stacks);
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean isEmpty() {
			for (ItemStack stack : stacks) {
				if (!stack.isEmpty()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public ItemStack getStack(int slot) {
			return stacks.get(slot);
		}

		@Override
		public ItemStack removeStack(int slot, int amount) {
			ItemStack split = stacks.get(slot).split(amount);
			markDirty();
			return split;
		}

		@Override
		public ItemStack removeStack(int slot) {
			ItemStack stack = stacks.get(slot);
			stacks.set(slot, ItemStack.EMPTY);
			markDirty();
			return stack;
		}

		@Override
		public boolean isValid(int slot, ItemStack stack) {
			return stack.isFood();
		}

		@Override
		public void setStack(int slot, ItemStack stack) {
			stacks.set(slot, stack);
			markDirty();
		}

		@Override
		public void markDirty() {
			Inventories.writeNbt(containerStack.getOrCreateSubNbt(INVENTORY_NBT_KEY), stacks);
		}

		@Override
		public boolean canPlayerUse(PlayerEntity player) {
			return true;
		}

		@Override
		public void clear() {
			stacks.clear();
		}
	}



	private class CustomScreenHandler extends ScreenHandler {
		protected CustomScreenHandler(int syncId, PlayerInventory playerInventory, ItemStack containerStack) {
			super(screenHandlerType, syncId);

			StackInventory inventory = getInventory(containerStack);
			for (int i = 0; i < size; i++) {
				addSlot(new FoodSlot(inventory, i, 0, 0));
			}
			for (int i = 9; i < 36; i++) {
				addSlot(new Slot(playerInventory, i, 0, 0));
			}
			for (int i = 0; i < 9; i++) {
				addSlot(new Slot(playerInventory, i, 0, 0));
			}
		}

		@Override
		public boolean canUse(PlayerEntity player) {
			return true;
		}

		@Override
		public ItemStack transferSlot(PlayerEntity player, int index) {
			ItemStack result = ItemStack.EMPTY;
			Slot slot = this.slots.get(index);
			if (slot.hasStack()) {
				ItemStack moveStack = slot.getStack();
				result = moveStack.copy();
				if (index < size ? !this.insertItem(moveStack, size, this.slots.size(), true) : !this.insertItem(moveStack, 0, size, false)) {
					return ItemStack.EMPTY;
				}
				if (moveStack.isEmpty()) {
					slot.setStack(ItemStack.EMPTY);
				} else {
					slot.markDirty();
				}
			}
			return result;
		}
	}

	private static class FoodSlot extends Slot {
		public FoodSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return inventory.isValid(this.getIndex(), stack);
		}
	}

	private class ScreenHandlerFactory implements NamedScreenHandlerFactory {
		private final ItemStack containerStack;

		ScreenHandlerFactory(ItemStack containerStack) {
			this.containerStack = containerStack;
		}

		@Override
		public Text getDisplayName() {
			return containerStack.getName();
		}

		@Nullable
		@Override
		public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
			return new CustomScreenHandler(syncId, playerInventory, containerStack);
		}
	}
}
