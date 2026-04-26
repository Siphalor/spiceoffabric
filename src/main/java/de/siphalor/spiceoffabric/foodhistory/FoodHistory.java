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
import lombok.Getter;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	public void read(FriendlyByteBuf buffer) {
		dictionary.clear();
		recentlyEaten.clear();
		recentlyEaten.setLength(SpiceOfFabric.config.food.historyLength);

		for (int l = buffer.readVarInt(), i = 0; i < l; i++) {
			dictionary.put(buffer.readVarInt(), FoodHistoryEntry.read(buffer));
		}
		for (int l = buffer.readVarInt(), i = 0; i < l; i++) {
			// Using forceEnqueue here to make sure we're not running out of space and throwing an exception
			// just because of a small desync of the history length ;)
			recentlyEaten.forceEnqueue(buffer.readVarInt());
		}

		uniqueFoodsEaten.clear();

		if (buffer.readBoolean()) {
			final int length = buffer.readVarInt();
			for (int i = 0; i < length; i++) {
				uniqueFoodsEaten.add(FoodHistoryEntry.read(buffer));
			}
		}
	}

	public CompoundTag write(CompoundTag compoundTag) {
		defragmentDictionary();
		ListTag list = new ListTag();
		for (Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
			list.add(entry.getKey(), entry.getValue().write(new CompoundTag()));
		}
		compoundTag.put(DICTIONARY_NBT_KEY, list);
		ListTag historyList = new ListTag();
		for (Integer id : recentlyEaten) {
			historyList.add(IntTag.valueOf(id));
		}
		compoundTag.put(RECENT_HISTORY_NBT_KEY, historyList);
		ListTag carrotHistoryList = new ListTag();
		for (FoodHistoryEntry entry : uniqueFoodsEaten) {
			carrotHistoryList.add(entry.write(new CompoundTag()));
		}
		compoundTag.put(CARROT_HISTORY_NBT_KEY, carrotHistoryList);
		return compoundTag;
	}

	public static FoodHistory read(CompoundTag compoundTag) {
		FoodHistory foodHistory = new FoodHistory();
		if (compoundTag.contains(DICTIONARY_NBT_KEY, 9)) {
			ListTag nbtDictionary = compoundTag.getList(DICTIONARY_NBT_KEY, 10);
			for (int i = 0; i < nbtDictionary.size(); i++) {
				FoodHistoryEntry entry = FoodHistoryEntry.read((CompoundTag) nbtDictionary.get(i));
				if (entry != null) {
					foodHistory.dictionary.put(i, entry);
				}
			}
		}
		foodHistory.nextId = foodHistory.dictionary.size();

		Tag recentHistoryTag = compoundTag.get(RECENT_HISTORY_NBT_KEY);
		if (recentHistoryTag instanceof CollectionTag<?>) {
			for (Tag tag : (CollectionTag<?>) recentHistoryTag) {
				// Using forceEnqueue here to make sure we're not running out of space and throwing an exception.
				// The history length might have changed (decreased) since the last time the player logged in.
				foodHistory.recentlyEaten.forceEnqueue(((IntTag) tag).getAsInt());
			}
		}

		if (compoundTag.contains(CARROT_HISTORY_NBT_KEY, 9)) {
			ListTag nbtCarrotHistory = compoundTag.getList(CARROT_HISTORY_NBT_KEY, 10);
			for (Tag tag : nbtCarrotHistory) {
				if (!(tag instanceof CompoundTag carrotEntry)) {
					continue;
				}
				FoodHistoryEntry entry = FoodHistoryEntry.read(carrotEntry);
				if (entry != null) {
					foodHistory.uniqueFoodsEaten.add(entry);
				}
			}
		}

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
