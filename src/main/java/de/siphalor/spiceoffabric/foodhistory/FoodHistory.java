package de.siphalor.spiceoffabric.foodhistory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.SOFConfig;
import de.siphalor.spiceoffabric.networking.SOFCommonNetworking;
import de.siphalor.spiceoffabric.networking.SyncFoodHistoryS2CPacket;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.queue.ArrayFixedLengthIntFIFOQueue;
import de.siphalor.spiceoffabric.util.queue.FixedLengthIntFIFOQueueWithStats;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

import com.mojang.serialization.Codec;
//- import net.minecraft.nbt.CollectionTag;
//- import net.minecraft.nbt.CompoundTag;
//- import net.minecraft.nbt.IntTag;
//- import net.minecraft.nbt.ListTag;
//- import net.minecraft.nbt.Tag;
//- import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FoodHistory {

	protected static final String DICTIONARY_NBT_KEY = "dictionary";
	protected static final String RECENT_HISTORY_NBT_KEY = "history";
	protected static final String CARROT_HISTORY_NBT_KEY = "carrotHistory";

	public static FoodHistory get(Player player) {
		if (player == null) {
			return null;
		}
		FoodData hungerManager = player.getFoodData();
		if (!(hungerManager instanceof IHungerManager)) {
			return null;
		}
		return ((IHungerManager) hungerManager).spiceOfFabric_getFoodHistory();
	}

	protected BiMap<Integer, FoodHistoryEntry> dictionary;
	protected int nextId = 0;

	protected FixedLengthIntFIFOQueueWithStats recentlyEaten;

	@Getter
	protected Set<FoodHistoryEntry> uniqueFoodsEaten;

	public FoodHistory() {
		dictionary = HashBiMap.create();
		recentlyEaten = new FixedLengthIntFIFOQueueWithStats(new ArrayFixedLengthIntFIFOQueue(
				SpiceOfFabric.config.food.historyLength
		));
		uniqueFoodsEaten = new HashSet<>();
	}

	public void reset() {
		resetHistory();
		resetUniqueFoodsEaten();
	}

	public void resetHistory() {
		dictionary.clear();
		nextId = 0;
		recentlyEaten.clear();
	}

	public void resetUniqueFoodsEaten() {
		uniqueFoodsEaten.clear();
	}

	public SyncFoodHistoryS2CPacket toPacket() {
		return new SyncFoodHistoryS2CPacket(dictionary, recentlyEaten, uniqueFoodsEaten);
	}

	public void applyPacket(SyncFoodHistoryS2CPacket packet) {
		dictionary = packet.getDictionary();
		recentlyEaten = packet.getRecentlyEaten();
	 	uniqueFoodsEaten = packet.getUniqueFoodsEaten();
	}

	//# if MC_VERSION_NUMBER >= 12106
	public void write(ValueOutput valueOutput) {
	//# else
	//- public CompoundTag write(CompoundTag compoundTag) {
	//# end
		defragmentDictionary();

		//# if MC_VERSION_NUMBER >= 12106
		ValueOutput.ValueOutputList list = valueOutput.childrenList(DICTIONARY_NBT_KEY);
		dictionary.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey))
				.forEach(entry -> entry.getValue().write(list.addChild()));
		//# else
		//- ListTag list = new ListTag();
		//- for (Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
		//- 	list.add(entry.getKey(), entry.getValue().write(new CompoundTag()));
		//- }
		//- compoundTag.put(DICTIONARY_NBT_KEY, list);
		//# end

		//# if MC_VERSION_NUMBER >= 12106
		ValueOutput.TypedOutputList<Integer> historyList = valueOutput.list(RECENT_HISTORY_NBT_KEY, Codec.INT);
		recentlyEaten.forEach(historyList::add);
		//# else
		//- ListTag historyList = new ListTag();
		//- for (Integer id : recentlyEaten) {
		//- 	historyList.add(IntTag.valueOf(id));
		//- }
		//- compoundTag.put(RECENT_HISTORY_NBT_KEY, historyList);
		//# end

		//# if MC_VERSION_NUMBER >= 12106
		ValueOutput.ValueOutputList carrotHistoryList = valueOutput.childrenList(CARROT_HISTORY_NBT_KEY);
		uniqueFoodsEaten.forEach(entry -> entry.write(carrotHistoryList.addChild()));
		//# else
		//- ListTag carrotHistoryList = new ListTag();
		//- for (FoodHistoryEntry entry : uniqueFoodsEaten) {
		//- 	carrotHistoryList.add(entry.write(new CompoundTag()));
		//- }
		//- compoundTag.put(CARROT_HISTORY_NBT_KEY, carrotHistoryList);
		//- return compoundTag;
		//# end
	}

	//# if MC_VERSION_NUMBER >= 12106
	public static FoodHistory read(ValueInput valueInput) {
	//# else
	//- public static FoodHistory read(CompoundTag compoundTag) {
	//# end
		FoodHistory foodHistory = new FoodHistory();
		//# if MC_VERSION_NUMBER >= 12106
		int i = 0;
		for (ValueInput entryInput : valueInput.childrenListOrEmpty(DICTIONARY_NBT_KEY)) {
			int finalI = i;
			FoodHistoryEntry.read(entryInput).ifPresent(entry ->
					foodHistory.dictionary.put(finalI, entry)
			);
			i++;
		}
		//# else
		//- if (compoundTag.contains(DICTIONARY_NBT_KEY, 9)) {
		//- 	ListTag nbtDictionary = compoundTag.getList(DICTIONARY_NBT_KEY, 10);
		//- 	for (int i = 0; i < nbtDictionary.size(); i++) {
		//- 		int finalI = i;
		//- 		FoodHistoryEntry.read((CompoundTag) nbtDictionary.get(i)).ifPresent(entry ->
		//- 			foodHistory.dictionary.put(finalI, entry)
		//- 		);
		//- 	}
		//- }
		//# end
		foodHistory.nextId = foodHistory.dictionary.size();

		//# if MC_VERSION_NUMBER >= 12106
		// Using forceEnqueue here to make sure we're not running out of space and throwing an exception.
		// The history length might have changed (decreased) since the last time the player logged in.
		valueInput.listOrEmpty(RECENT_HISTORY_NBT_KEY, Codec.INT).forEach(foodHistory.recentlyEaten::enqueue);
		//# else
		//- Tag recentHistoryTag = compoundTag.get(RECENT_HISTORY_NBT_KEY);
		//- if (recentHistoryTag instanceof CollectionTag<?>) {
		//- 	for (Tag tag : (CollectionTag<?>) recentHistoryTag) {
		//- 		// Using forceEnqueue here to make sure we're not running out of space and throwing an exception.
		//- 		// The history length might have changed (decreased) since the last time the player logged in.
		//- 		foodHistory.recentlyEaten.forceEnqueue(((IntTag) tag).getAsInt());
		//- 	}
		//- }
		//# end

		//# if MC_VERSION_NUMBER >= 12106
		valueInput.childrenListOrEmpty(CARROT_HISTORY_NBT_KEY).forEach(entryInput ->
			FoodHistoryEntry.read(entryInput).ifPresent(foodHistory.uniqueFoodsEaten::add)
		);
		//# else
		//- if (compoundTag.contains(CARROT_HISTORY_NBT_KEY, 9)) {
		//- 	ListTag nbtCarrotHistory = compoundTag.getList(CARROT_HISTORY_NBT_KEY, 10);
		//- 	for (Tag tag : nbtCarrotHistory) {
		//- 		if (!(tag instanceof CompoundTag carrotEntry)) {
		//- 			continue;
		//- 		}
		//- 		FoodHistoryEntry.read(carrotEntry).ifPresent(foodHistory.uniqueFoodsEaten::add);
		//- 	}
		//- }
		//# end

		return foodHistory;
	}

	public void defragmentDictionary() {
		Int2IntMap oldToNewMap = new Int2IntArrayMap();
		int i = 0;
		for (Integer id : dictionary.keySet()) {
			oldToNewMap.put((int) id, i);
			i++;
		}
		nextId = i;
		int historySize = recentlyEaten.size();
		for (int j = 0; j < historySize; j++) {
			recentlyEaten.enqueue(oldToNewMap.get(recentlyEaten.dequeue()));
		}
		BiMap<Integer, FoodHistoryEntry> newDictionary = HashBiMap.create();
		for (Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
			newDictionary.put(oldToNewMap.get((int) entry.getKey()), entry.getValue());
		}
		dictionary.clear();
		dictionary.putAll(newDictionary);
	}

	public int getTimesRecentlyEaten(ItemStack stack) {
		Integer id = dictionary.inverse().get(FoodHistoryEntry.fromItemStack(stack));
		if (id == null) {
			return 0;
		}
		return recentlyEaten.getStats().getOrDefault((int) id, 0);
	}

	public int getFoodCountSinceLastEaten(ItemStack stack) {
		Integer id = dictionary.inverse().get(FoodHistoryEntry.fromItemStack(stack));
		if (id == null) {
			return -1;
		}
		IntIterator iterator = recentlyEaten.iterator();
		int foundI = Integer.MIN_VALUE;
		while (iterator.hasNext()) {
			foundI++;
			if (iterator.nextInt() == id) {
				foundI = 0;
			}
		}
		return foundI;
	}

	public void addFood(ItemStack stack, ServerPlayer player) {
		FoodHistoryEntry entry = FoodHistoryEntry.fromItemStack(stack);

		if (SpiceOfFabric.hasClientMod(player)) {
			SOFCommonNetworking.sendAddFoodPacket(player, entry);
		}

		addFood(entry);
	}

	public void addFood(FoodHistoryEntry entry) {
		Integer boxedId = dictionary.inverse().get(entry);
		int id;
		if (boxedId == null) {
			id = nextId++;
			dictionary.put(id, entry);
		} else {
			id = boxedId;
		}

		// Make sure the history length is correct, just in case
		if (recentlyEaten.getLength() != SpiceOfFabric.config.food.historyLength) {
			recentlyEaten.setLength(SpiceOfFabric.config.food.historyLength);
		}

		recentlyEaten.forceEnqueue(id);

		if (SpiceOfFabric.config.carrot.enable) {
			uniqueFoodsEaten.add(entry);
		}
	}

	public int getRecentlyEatenCount() {
		return recentlyEaten.size();
	}

	public ItemStack getStackFromRecentlyEaten(int index) {
		if (index < 0 || index >= recentlyEaten.size()) {
			return null;
		}
		return dictionary.get(recentlyEaten.get(index)).getStack();
	}

	public boolean isInUniqueEaten(ItemStack stack) {
		FoodHistoryEntry entry = FoodHistoryEntry.fromItemStack(stack);
		return uniqueFoodsEaten.contains(entry);
	}

	public int getCarrotHealthOffset(Player player) {
		AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
		SOFConfig.Carrot carrotConfig = SpiceOfFabric.config.carrot;
		carrotConfig.prepareExpressions(uniqueFoodsEaten.size(), (int) maxHealthAttr.getBaseValue());

		int newMaxHealth = Mth.floor(carrotConfig.healthFormula.evaluate());
		if (carrotConfig.maxHealth > 0) {
			newMaxHealth = Mth.clamp(newMaxHealth, 1, carrotConfig.maxHealth);
		}
		return newMaxHealth - (int) maxHealthAttr.getBaseValue();
	}

	public int getCarrotMaxHealth(Player player) {
		AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
		SOFConfig.Carrot carrotConfig = SpiceOfFabric.config.carrot;
		carrotConfig.prepareExpressions(uniqueFoodsEaten.size(), (int) maxHealthAttr.getBaseValue());
		return Mth.floor(carrotConfig.healthFormula.evaluate());
	}
}
