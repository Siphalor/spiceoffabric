package de.siphalor.spiceoffabric.item;

import de.siphalor.capsaicin.api.food.CamoFoodContext;
import de.siphalor.capsaicin.api.food.CamoFoodItem;
import de.siphalor.capsaicin.api.food.DynamicFoodPropertiesAccess;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.container.FoodContainerScreenHandler;
import de.siphalor.spiceoffabric.container.FoodContainerTooltip;
import de.siphalor.spiceoffabric.container.ItemStackInventory;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import de.siphalor.spiceoffabric.util.IndexedValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//- import net.minecraft.world.InteractionResultHolder;
//- import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUtils;
//- import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class FoodContainerItem extends Item implements CamoFoodItem {
	private static final String INVENTORY_NBT_KEY = "inventory";
	private static final Style LORE_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false);
	public static final Component LORE_EMPTY = Component.translatable(SpiceOfFabric.MOD_ID + ".food_container.lore.empty").setStyle(LORE_STYLE);
	public static final String LORE_GENERAL_KEY = SpiceOfFabric.MOD_ID + ".food_container.lore.general";
	private static final IndexedValue<ItemStack> NO_STACK = new IndexedValue<>(-1, ItemStack.EMPTY);

	private final MenuType<?> screenHandlerType;
	private final int size;

	public FoodContainerItem(Properties settings, int size, MenuType<?> screenHandlerType) {
		super(settings);
		this.screenHandlerType = screenHandlerType;
		this.size = size;
	}

	public MenuType<?> getScreenHandlerType() {
		return screenHandlerType;
	}

	public int getSize() {
		return size;
	}

	public ItemStack getNextFoodStack(ItemStack stack, Player player) {
		//# if MC_VERSION_NUMBER >= 12006
		return getNextFoodStack(getInventory(stack, player.registryAccess()), player).value();
		//# else
		//- return getNextFoodStack(getInventory(stack), player).value();
		//# end
	}

	private IndexedValue<ItemStack> getNextFoodStack(ItemStackInventory inventory, Player player) {
		FoodHistory foodHistory = FoodHistory.get(player);
		if (foodHistory == null) {
			return NO_STACK;
		}

		var filteredInv = new ArrayList<IndexedValue<Pair<ItemStack, FoodProperties>>>(inventory.getContainerSize());
		var foodPropertiesAccess = DynamicFoodPropertiesAccess.create();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			FoodProperties foodComponent = foodPropertiesAccess.withStack(stack).getModifiedFoodComponent();
			if (foodComponent == null) {
				SpiceOfFabric.LOGGER.warn("Non-food stack " + stack + " found in food container " + this);
				continue;
			}
			if (stack.isEmpty() || !player.canEat(foodComponent.canAlwaysEat())) {
				continue;
			}

			filteredInv.add(new IndexedValue<>(i, Pair.of(stack, foodComponent)));
		}
		if (filteredInv.isEmpty()) {
			return NO_STACK;
		}

		int requiredFood = 20 - player.getFoodData().getFoodLevel();
		return findMostAppropriateFood(filteredInv, requiredFood, player);
	}

	private IndexedValue<ItemStack> findMostAppropriateFood(
			List<IndexedValue<Pair<ItemStack, FoodProperties>>> foods,
			int requiredFood,
			LivingEntity entity
	) {
		var bestStack = NO_STACK;
		int bestDelta = Integer.MAX_VALUE;
		int bestConsumeTime = Integer.MAX_VALUE;
		for (var value : foods) {
			ItemStack stack = value.value().getFirst();
			int delta = requiredFood - value.value().getSecond().nutrition();
			//# if MC_VERSION_NUMBER >= 12100
			int consumeTime = stack.getUseDuration(entity);
			//# else
			//- int consumeTime = stack.getUseDuration();
			//# end
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

	//# if MC_VERSION_NUMBER >= 12006
	public boolean isInventoryEmpty(ItemStack stack, HolderLookup.Provider levelRegistry) {
		return getInventory(stack, levelRegistry).isEmpty();
	}

	public ItemStackInventory getInventory(ItemStack stack, HolderLookup.Provider levelRegistry) {
		return ItemStackInventory.fromStack(stack, INVENTORY_NBT_KEY, size, levelRegistry);
	}
	//# else
	//- public boolean isInventoryEmpty(ItemStack stack) {
	//- 	return getInventory(stack).isEmpty();
	//- }

	//- public ItemStackInventory getInventory(ItemStack stack) {
	//- 	return ItemStackInventory.fromStack(stack, INVENTORY_NBT_KEY, size);
	//- }
	//# end

	@Override
	public void onDestroyed(ItemEntity entity) {
		//# if MC_VERSION_NUMBER >= 12006
		ItemStackInventory inventory = getInventory(entity.getItem(), entity.registryAccess());
		//# else
		//- ItemStackInventory inventory = getInventory(entity.getItem());
		//# end
		ItemUtils.onContainerDestroyed(
				entity,
				//# if MC_VERSION_NUMBER >= 260100
				inventory.getContainedStacks().stream()
				//# else
				//- inventory.getContainedStacks().stream().toList()
				//# end
		);
	}

	//# if MC_VERSION_NUMBER >= 12106
	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		TooltipDisplay tooltipDisplay = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
		if (!tooltipDisplay.shows(DataComponents.CONTAINER)) {
			return Optional.empty();
		}
		var containerContents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		int count = 0;
		int filled = 0;
		//# if MC_VERSION_NUMBER >= 260100
		for (ItemStackTemplate containedStack : containerContents.nonEmptyItems()) {
			if (containedStack.count() > 0) {
				count += containedStack.count();
				filled++;
			}
		}
		//# else
		//- for (ItemStack containedStack : containerContents.nonEmptyItems()) {
		//- 	if (!containedStack.isEmpty()) {
		//- 		count += containedStack.getCount();
		//- 		filled++;
		//- 	}
		//- }
		//# end
		return Optional.of(new FoodContainerTooltip(size, filled, count));
	}
	//# else
	//- @Override
	//- public void appendHoverText(
	//- 		ItemStack stack,
	//- 		TooltipContext context,
	//- 		List<Component> tooltip,
	//- 		TooltipFlag tooltipFlag
	//- ) {
	//- 	//# if MC_VERSION_NUMBER >= 12006
	//- 	ItemStackInventory inventory = getInventory(stack, context.registries());
	//- 	//# else
	//- 	ItemStackInventory inventory = getInventory(stack);
	//- 	//# end
	//- 	if (inventory.isEmpty()) {
	//- 		tooltip.add(LORE_EMPTY);
	//- 	} else {
	//- 		int count = 0;
	//- 		int filled = 0;
	//- 		for (int i = 0; i < inventory.getContainerSize(); i++) {
	//- 			ItemStack invStack = inventory.getItem(i);
	//- 			if (!invStack.isEmpty()) {
	//- 				count += invStack.getCount();
	//- 				filled++;
	//- 			}
	//- 		}
	//- 		tooltip.add(Component.translatable(LORE_GENERAL_KEY, filled, size, count).setStyle(LORE_STYLE));
	//- 	}
	//- }
	//# end

	//# if MC_VERSION_NUMBER < 12006
	//- @Override
	//- public boolean isEdible() {
	//- 	return true;
	//- }
	//# end

	@Override
	//# if MC_VERSION_NUMBER >= 12102
	public InteractionResult
	//# else
	//- public InteractionResultHolder<ItemStack>
	//# end
	use(Level world, Player user, InteractionHand hand) {
		ItemStack stackInHand = user.getItemInHand(hand);
		ItemStack nextFoodItem = getNextFoodStack(stackInHand, user);
		if (nextFoodItem.isEmpty()) {
			// Prevent opening the container directly after eating
			long currentTime = System.currentTimeMillis();
			if (user instanceof ServerPlayer player && checkLastEatTime(player, currentTime)) {
				updateLastEatTime(player, currentTime);

				int slot = hand == InteractionHand.MAIN_HAND
						//# if MC_VERSION_NUMBER >= 12106
						? user.getInventory().getSelectedSlot()
						//# else
					//-     ? user.getInventory().selected
						//# end
						: Inventory.SLOT_OFFHAND;
				openScreen(stackInHand, user, slot);
				//# if MC_VERSION_NUMBER >= 12102
				return InteractionResult.SUCCESS;
				//# else
				//- return InteractionResultHolder.success(stackInHand);
				//# end
			}
		} else {
			FoodProperties foodComponent = nextFoodItem.get(DataComponents.FOOD);
			if (foodComponent != null) {
				if (user.canEat(foodComponent.canAlwaysEat())) {
					user.startUsingItem(hand);
					//# if MC_VERSION_NUMBER >= 12102
					return InteractionResult.CONSUME;
					//# else
					//- return InteractionResultHolder.consume(stackInHand);
					//# end
				}
				//# if MC_VERSION_NUMBER >= 12102
				return InteractionResult.FAIL;
				//# else
				//- return InteractionResultHolder.fail(stackInHand);
				//# end
			}
		}
		//# if MC_VERSION_NUMBER >= 12102
		return InteractionResult.PASS;
		//# else
		//- return InteractionResultHolder.pass(stackInHand);
		//# end
	}

	@Override
	//# if MC_VERSION_NUMBER >= 12102
	public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
	//# else
	//- public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
	//# end
		openContainer:
		//# if MC_VERSION_NUMBER >= 12110
		if (!world.isClientSide() && user instanceof ServerPlayer player) {
		//# else
		//- if (!world.isClientSide && user instanceof ServerPlayer player) {
		//# end
			// Only open the container if the player hasn't used the item for too long
			//# if MC_VERSION_NUMBER >= 12100
			int maxUseTime = getUseDuration(stack, user);
			//# else
			//- int maxUseTime = getUseDuration(stack);
			//# end
			if (maxUseTime - remainingUseTicks > 5) {
				break openContainer;
			}

			// Prevent opening the container directly after eating
			long currentTime = System.currentTimeMillis();
			if (!checkLastEatTime(player, currentTime)) {
				break openContainer;
			}
			updateLastEatTime(player, currentTime);

			Inventory inv = player.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				if (inv.getItem(i) == stack) {
					openScreen(stack, player, i);
					//# if MC_VERSION_NUMBER >= 12102
					return true;
					//# else
					//- return;
					//# end
				}
			}
		}
		//# if MC_VERSION_NUMBER >= 12102
		return super.releaseUsing(stack, world, user, remainingUseTicks);
		//# else
		//- super.releaseUsing(stack, world, user, remainingUseTicks);
		//# end
	}

	public boolean checkLastEatTime(ServerPlayer user, long currentTime) {
		long lastEatTime = ((IServerPlayerEntity) user).spiceOfFabric_getLastContainerEatTime();
		return currentTime - lastEatTime >= 1000;
	}

	public void updateLastEatTime(ServerPlayer user, long currentTime) {
		((IServerPlayerEntity) user).spiceOfFabric_setLastContainerEatTime(currentTime);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if (!(user instanceof Player player)) {
			return stack;
		}

		//# if MC_VERSION_NUMBER >= 12006
		ItemStackInventory inventory = getInventory(stack, world.registryAccess());
		//# else
		//- ItemStackInventory inventory = getInventory(stack);
		//# end
		var foodStack = getNextFoodStack(inventory, player);
		if (foodStack.value().isEmpty()) {
			return stack;
		}

		if (player instanceof ServerPlayer) {
			((IServerPlayerEntity) player).spiceOfFabric_setLastContainerEatTime(System.currentTimeMillis());
		}
		ItemStack newStack = foodStack.value().finishUsingItem(world, user);
		if (newStack != foodStack.value()) {
			if (inventory.canPlaceItem(foodStack.index(), newStack)) {
				inventory.setItem(foodStack.index(), newStack);
			} else {
				player.getInventory().placeItemBackInInventory(newStack);
				inventory.removeItemNoUpdate(foodStack.index());
			}
		} else {
			inventory.setChanged();
		}

		return stack;
	}

	protected void openScreen(ItemStack stack, Player user, int invIndex) {
		user.stopUsingItem();
		user.openMenu(new FoodContainerScreenHandler.Factory(this, stack));
		user.containerMenu.addSlotListener(new ContainerListener() {
			@Override
			public void slotChanged(AbstractContainerMenu handler, int updateSlotId, ItemStack updateStack) {
				Slot updateSlot = handler.getSlot(updateSlotId);
				if (!(user instanceof ServerPlayer serverPlayer)) {
					return;
				}

				if (updateSlot.getContainerSlot() == invIndex && updateSlot.container == user.getInventory()) {
					if (updateStack.isEmpty() || !ItemStack.matches(updateStack, stack)) {
						closeScreen(serverPlayer);
					}
				} else {
					if (ItemStack.matches(updateStack, stack)) {
						closeScreen(serverPlayer);
					}
				}
			}

			@Override
			public void dataChanged(AbstractContainerMenu handler, int property, int value) {
				// N/A
			}
		});
	}

	private static void closeScreen(ServerPlayer player) {
		player.closeContainer();
	}

	@Override
	public @Nullable ItemStack getCamoFoodStack(@NotNull ItemStack stack, CamoFoodContext context) {
		if (context.user() instanceof Player player) {
			return getNextFoodStack(stack, player);
		}
		return stack;
	}
}
