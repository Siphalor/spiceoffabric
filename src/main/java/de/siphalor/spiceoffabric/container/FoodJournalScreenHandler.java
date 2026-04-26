package de.siphalor.spiceoffabric.container;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.FoodUtils;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class FoodJournalScreenHandler extends AbstractContainerMenu {
	private static final String PAGE_INDICATOR_TEXT_KEY = "book.pageIndicator";
	private static final Item PAGE_INDICATOR_ITEM = Items.STICK;
	private static final ItemStack PREV_STACK;
	private static final ItemStack NEXT_STACK;

	static {
		PREV_STACK = new ItemStack(Items.FEATHER);
		NEXT_STACK = new ItemStack(Items.FLINT);
		Component prevName = Component.translatable("createWorld.customize.custom.prev").withStyle(style -> style.withItalic(false));
		Component nextName = Component.translatable("createWorld.customize.custom.next").withStyle(style -> style.withItalic(false));
		//# if MC_VERSION_NUMBER >= 12006
		PREV_STACK.set(DataComponents.ITEM_NAME, prevName);
		NEXT_STACK.set(DataComponents.ITEM_NAME, nextName);
		//# else
		//- PREV_STACK.setHoverName(prevName);
		//- NEXT_STACK.setHoverName(nextName);
		//# end
	}

	private static final int JOURNAL_SLOT_COUNT = 9 * 5;

	private final ServerPlayer player;
	private final boolean clientHasMod;
	private final FoodJournalView currentView;
	private final PaginatedReadOnlyInventory foodJournalInventory;
	private final Container infoInventory;

	public FoodJournalScreenHandler(@Nullable MenuType<?> type, int syncId, FoodJournalView currentView, ServerPlayer player, FoodHistory foodHistory) {
		super(type, syncId);
		this.currentView = currentView;
		this.player = player;
		this.clientHasMod = SpiceOfFabric.hasClientMod(player);

		this.foodJournalInventory = createFoodJournalInventory(foodHistory, currentView);
		for (int i = 0; i < JOURNAL_SLOT_COUNT; i++) {
			addSlot(new ReadOnlySlot(foodJournalInventory, i, 0, 0));
		}

		this.infoInventory = new SimpleContainer(
				ItemStack.EMPTY,
				ItemStack.EMPTY,
				ItemStack.EMPTY,
				PREV_STACK,
				createPageIndicatorStack(),
				NEXT_STACK,
				getViewStack(FoodJournalView.HISTORY, Items.BOOK),
				getViewStack(FoodJournalView.CARROT, Items.GOLD_INGOT),
				getViewStack(FoodJournalView.CARROT_UNEATEN, Items.COPPER_INGOT)
		);
		addSlot(new ReadOnlySlot(infoInventory, 0, 0, 0));
		addSlot(new ReadOnlySlot(infoInventory, 1, 0, 0));
		addSlot(new ReadOnlySlot(infoInventory, 2, 0, 0));
		addSlot(new ClickableSlot(infoInventory, 3, 0, 0, this::previousPage));
		addSlot(new ReadOnlySlot(infoInventory, 4, 0, 0));
		addSlot(new ClickableSlot(infoInventory, 5, 0, 0, this::nextPage));
		addSlot(new ClickableSlot(infoInventory, 6, 0, 0, getViewCallback(FoodJournalView.HISTORY)));
		addSlot(new ClickableSlot(infoInventory, 7, 0, 0, getViewCallback(FoodJournalView.CARROT)));
		addSlot(new ClickableSlot(infoInventory, 8, 0, 0, getViewCallback(FoodJournalView.CARROT_UNEATEN)));

		Inventory playerInventory = player.getInventory();
		for (int i = 9; i < 36; i++) {
			addSlot(new Slot(playerInventory, i, 0, 0));
		}
		for (int i = 0; i < 9; i++) {
			addSlot(new Slot(playerInventory, i, 0, 0));
		}
	}

	private PaginatedReadOnlyInventory createFoodJournalInventory(FoodHistory foodHistory, FoodJournalView view) {
		if (view == FoodJournalView.HISTORY) {
			int historySize = foodHistory.getRecentlyEatenCount();
			var stacks = new ArrayList<ItemStack>(historySize);
			for (int i = 0; i < historySize; i++) {
				stacks.add(foodHistory.getStackFromRecentlyEaten(i));
			}
			return new PaginatedReadOnlyInventory(JOURNAL_SLOT_COUNT, stacks);
		} else if (view == FoodJournalView.CARROT) {
			var stacks = foodHistory.getUniqueFoodsEaten().stream()
					.map(FoodHistoryEntry::getStack)
					.sorted(Comparator.comparingInt(stack -> {
						//# if MC_VERSION_NUMBER >= 12006
						FoodProperties foodComponent = stack.get(DataComponents.FOOD);
						//# else
						//- FoodProperties foodComponent = stack.getItem().getFoodProperties();
						//# end
						if (foodComponent == null) {
							return 0;
						}
						//# if MC_VERSION_NUMBER >= 12006
						return foodComponent.nutrition();
						//# else
						//- return foodComponent.getNutrition();
						//# end
					}))
					.toList();
			return new PaginatedReadOnlyInventory(JOURNAL_SLOT_COUNT, stacks);
		} else if (view == FoodJournalView.CARROT_UNEATEN) {
			var eatenItems = foodHistory.getUniqueFoodsEaten().stream()
					.map(entry -> entry.getStack().getItem()).collect(Collectors.toUnmodifiableSet());
			var stacks = BuiltInRegistries.ITEM.stream().parallel()
					.filter(FoodUtils::isFood)
					.filter(item -> !eatenItems.contains(item))
					.sorted(Comparator.comparingInt(item -> {
						//# if MC_VERSION_NUMBER >= 12006
						FoodProperties foodComponent = item.components().get(DataComponents.FOOD);
						//# else
						//- FoodProperties foodComponent = item.getFoodProperties();
						//# end
						if (foodComponent == null) {
							return 0;
						}
						//# if MC_VERSION_NUMBER >= 12006
						return foodComponent.nutrition();
						//# else
						//- return foodComponent.getNutrition();
						//# end
					}))
					.map(ItemStack::new)
					.toList();
			return new PaginatedReadOnlyInventory(JOURNAL_SLOT_COUNT, stacks);
		} else {
			throw new IllegalStateException("Unrecognized view " + view + " during journal screen creation");
		}
	}

	private ItemStack createPageIndicatorStack() {
		var stack = new ItemStack(PAGE_INDICATOR_ITEM);
		MutableComponent name = Component.translatable(
						PAGE_INDICATOR_TEXT_KEY,
						foodJournalInventory.getPage() + 1,
						foodJournalInventory.getPageCount()
				)
				.withStyle(style -> style.withItalic(false));
		//# if MC_VERSION_NUMBER >= 12006
		stack.set(DataComponents.ITEM_NAME, name);
		//# else
		//- stack.setHoverName(name);
		//# end
		return stack;
	}

	private ItemStack getViewStack(FoodJournalView view, Item itemRepresentation) {
		if (this.currentView == view || !view.isAvailable()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(itemRepresentation);
		Component name = clientHasMod
				? view.getTranslatableName()
				: Component.literal(view.getLiteralName()).withStyle(style -> style.withItalic(false));
		//# if MC_VERSION_NUMBER >= 12006
		stack.set(DataComponents.ITEM_NAME, name);
		//# else
		//- stack.setHoverName(name);
		//# end
		return stack;
	}

	private Runnable getViewCallback(FoodJournalView view) {
		if (!view.isAvailable()) {
			return () -> {
			};
		}
		return () -> player.openMenu(new Factory(player, view));
	}

	private void previousPage() {
		int page = foodJournalInventory.getPage();
		if (page > 0) {
			page--;
			foodJournalInventory.setPage(page);
			infoInventory.setItem(4, createPageIndicatorStack());
			sendAllDataToRemote();
		}
	}

	private void nextPage() {
		int page = foodJournalInventory.getPage();
		if (page < foodJournalInventory.getPageCount() - 1) {
			page++;
			foodJournalInventory.setPage(page);
			infoInventory.setItem(4, createPageIndicatorStack());
			sendAllDataToRemote();
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	public static class Factory implements MenuProvider {
		private final ServerPlayer player;
		private final FoodJournalView view;

		public Factory(ServerPlayer player, FoodJournalView view) {
			this.player = player;
			this.view = view;
		}

		@Override
		public Component getDisplayName() {
			if (SpiceOfFabric.hasClientMod(player)) {
				return view.getTranslatableName();
			}
			return Component.literal(view.getLiteralName());
		}

		@Nullable
		@Override
		public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
			return new FoodJournalScreenHandler(MenuType.GENERIC_9x6, syncId, view, this.player, ((IHungerManager) player.getFoodData()).spiceOfFabric_getFoodHistory());
		}
	}
}
