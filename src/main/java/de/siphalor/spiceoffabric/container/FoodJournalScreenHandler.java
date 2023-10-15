package de.siphalor.spiceoffabric.container;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.foodhistory.FoodHistoryEntry;
import de.siphalor.spiceoffabric.util.FoodUtils;
import de.siphalor.spiceoffabric.util.IHungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class FoodJournalScreenHandler extends ScreenHandler {
	private static final String PAGE_INDICATOR_TEXT_KEY = "book.pageIndicator";
	private static final Item PAGE_INDICATOR_ITEM = Items.STICK;
	private static final ItemStack PREV_STACK;
	private static final ItemStack NEXT_STACK;

	static {
		PREV_STACK = new ItemStack(Items.FEATHER);
		PREV_STACK.setCustomName(new TranslatableText("createWorld.customize.custom.prev").styled(style -> style.withItalic(false)));
		NEXT_STACK = new ItemStack(Items.FLINT);
		NEXT_STACK.setCustomName(new TranslatableText("createWorld.customize.custom.next").styled(style -> style.withItalic(false)));
	}

	private static final int JOURNAL_SLOT_COUNT = 9 * 5;

	private final ServerPlayerEntity player;
	private final boolean clientHasMod;
	private final FoodJournalView currentView;
	private final PaginatedReadOnlyInventory foodJournalInventory;
	private final Inventory infoInventory;

	public FoodJournalScreenHandler(@Nullable ScreenHandlerType<?> type, int syncId, FoodJournalView currentView, ServerPlayerEntity player, FoodHistory foodHistory) {
		super(type, syncId);
		this.currentView = currentView;
		this.player = player;
		this.clientHasMod = SpiceOfFabric.hasClientMod(player);

		this.foodJournalInventory = createFoodJournalInventory(foodHistory, currentView);
		for (int i = 0; i < JOURNAL_SLOT_COUNT; i++) {
			addSlot(new ReadOnlySlot(foodJournalInventory, i, 0, 0));
		}

		this.infoInventory = new SimpleInventory(
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

		PlayerInventory playerInventory = player.getInventory();
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
						FoodComponent foodComponent = stack.getItem().getFoodComponent();
						if (foodComponent == null) {
							return 0;
						}
						return foodComponent.getHunger();
					}))
					.toList();
			return new PaginatedReadOnlyInventory(JOURNAL_SLOT_COUNT, stacks);
		} else if (view == FoodJournalView.CARROT_UNEATEN) {
			var eatenItems = foodHistory.getUniqueFoodsEaten().stream()
					.map(entry -> entry.getStack().getItem()).collect(Collectors.toUnmodifiableSet());
			var stacks = Registry.ITEM.stream().parallel()
					.filter(FoodUtils::isFood)
					.filter(item -> !eatenItems.contains(item))
					.sorted(Comparator.comparingInt(item -> {
						FoodComponent foodComponent = item.getFoodComponent();
						if (foodComponent == null) {
							return 0;
						}
						return foodComponent.getHunger();
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
		stack.setCustomName(
				new TranslatableText(PAGE_INDICATOR_TEXT_KEY, foodJournalInventory.getPage() + 1, foodJournalInventory.getPageCount())
						.styled(style -> style.withItalic(false))
		);
		return stack;
	}

	private ItemStack getViewStack(FoodJournalView view, Item itemRepresentation) {
		if (this.currentView == view || !view.isAvailable()) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(itemRepresentation)
				.setCustomName(
						clientHasMod
								? view.getTranslatableName()
								: new LiteralText(view.getLiteralName()).styled(style -> style.withItalic(false))
				);
	}

	private Runnable getViewCallback(FoodJournalView view) {
		if (!view.isAvailable()) {
			return () -> {
			};
		}
		return () -> player.openHandledScreen(new Factory(player, view));
	}

	private void previousPage() {
		int page = foodJournalInventory.getPage();
		if (page > 0) {
			page--;
			foodJournalInventory.setPage(page);
			infoInventory.setStack(4, createPageIndicatorStack());
			syncState();
		}
	}

	private void nextPage() {
		int page = foodJournalInventory.getPage();
		if (page < foodJournalInventory.getPageCount() - 1) {
			page++;
			foodJournalInventory.setPage(page);
			infoInventory.setStack(4, createPageIndicatorStack());
			syncState();
		}
	}

	@Override
	public ItemStack transferSlot(PlayerEntity player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	public static class Factory implements NamedScreenHandlerFactory {
		private final ServerPlayerEntity player;
		private final FoodJournalView view;

		public Factory(ServerPlayerEntity player, FoodJournalView view) {
			this.player = player;
			this.view = view;
		}

		@Override
		public Text getDisplayName() {
			if (SpiceOfFabric.hasClientMod(player)) {
				return view.getTranslatableName();
			}
			return new LiteralText(view.getLiteralName());
		}

		@Nullable
		@Override
		public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
			return new FoodJournalScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId, view, this.player, ((IHungerManager) player.getHungerManager()).spiceOfFabric_getFoodHistory());
		}
	}
}
