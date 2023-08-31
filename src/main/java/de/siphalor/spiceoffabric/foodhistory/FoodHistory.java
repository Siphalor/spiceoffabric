package de.siphalor.spiceoffabric.foodhistory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.FixedLengthIntFIFOQueue;
import de.siphalor.spiceoffabric.util.IHungerManager;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FoodHistory {

	protected static final String DICTIONARY_NBT_KEY = "dictionary";
	protected static final String RECENT_HISTORY_NBT_KEY = "history";
	protected static final String CARROT_HISTORY_NBT_KEY = "carrotHistory";

	public static FoodHistory get(PlayerEntity player) {
		if (player == null) {
			return null;
		}
		HungerManager hungerManager = player.getHungerManager();
		if (!(hungerManager instanceof IHungerManager)) {
			return null;
		}
		return ((IHungerManager) hungerManager).spiceOfFabric_getFoodHistory();
	}

	protected Set<FoodHistoryEntry> carrotHistory;

	protected BiMap<Integer, FoodHistoryEntry> dictionary;
	protected int nextId = 0;
	protected FixedLengthIntFIFOQueue history;
	protected Int2IntMap stats;

	public FoodHistory() {
		setup();
	}

	public void setup() {
		dictionary = HashBiMap.create();
		history = new FixedLengthIntFIFOQueue(Config.food.historyLength);
		stats = new Int2IntArrayMap();
		carrotHistory = new HashSet<>();
	}

	public void reset() {
		resetHistory();
		resetCarrotHistory();
	}

	public void resetHistory() {
		dictionary.clear();
		nextId = 0;
		history.clear();
		stats.clear();
	}

	public Set<FoodHistoryEntry> getCarrotHistory() {
		return carrotHistory;
	}

	public void resetCarrotHistory() {
		carrotHistory.clear();
	}

	public void write(PacketByteBuf buffer) {
		buffer.writeVarInt(dictionary.size());
		for (Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
			buffer.writeVarInt(entry.getKey());
			entry.getValue().write(buffer);
		}
		buffer.writeVarInt(history.size());
		for (int integer : history) {
			buffer.writeVarInt(integer);
		}
		if (Config.carrot.enable) {
			buffer.writeBoolean(true);
			buffer.writeVarInt(carrotHistory.size());
			for (FoodHistoryEntry entry : carrotHistory) {
				entry.write(buffer);
			}
		} else {
			buffer.writeBoolean(false);
		}
	}

	public void read(PacketByteBuf buffer) {
		dictionary.clear();
		history.clear();
		history.setLength(Config.food.historyLength);
		for (int l = buffer.readVarInt(), i = 0; i < l; i++) {
			dictionary.put(buffer.readVarInt(), FoodHistoryEntry.from(buffer));
		}
		for (int l = buffer.readVarInt(), i = 0; i < l; i++) {
			// Using forceEnqueue here to make sure we're not running out of space and throwing an exception
			// just because of a small desync of the history length ;)
			history.forceEnqueue(buffer.readVarInt());
		}
		if (buffer.readBoolean()) {
			final int length = buffer.readVarInt();
			carrotHistory = new HashSet<>(length);
			for (int i = 0; i < length; i++) {
				carrotHistory.add(FoodHistoryEntry.from(buffer));
			}
		}
		buildStats();
	}

	public NbtCompound write(NbtCompound compoundTag) {
		defragmentDictionary();
		NbtList list = new NbtList();
		for (Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
			list.add(entry.getKey(), entry.getValue().write(new NbtCompound()));
		}
		compoundTag.put(DICTIONARY_NBT_KEY, list);
		NbtList historyList = new NbtList();
		for (Integer id : history) {
			historyList.add(NbtInt.of(id));
		}
		compoundTag.put(RECENT_HISTORY_NBT_KEY, historyList);
		NbtList carrotHistoryList = new NbtList();
		for (FoodHistoryEntry entry : carrotHistory) {
			carrotHistoryList.add(entry.write(new NbtCompound()));
		}
		compoundTag.put(CARROT_HISTORY_NBT_KEY, carrotHistoryList);
		return compoundTag;
	}

	public static FoodHistory read(NbtCompound compoundTag) {
		FoodHistory foodHistory = new FoodHistory();
		if (compoundTag.contains(DICTIONARY_NBT_KEY, 9)) {
			NbtList nbtDictionary = compoundTag.getList(DICTIONARY_NBT_KEY, 10);
			for (int i = 0; i < nbtDictionary.size(); i++) {
				FoodHistoryEntry entry = new FoodHistoryEntry().read((NbtCompound) nbtDictionary.get(i));
				if (entry != null) {
					foodHistory.dictionary.put(i, entry);
				}
			}
		}
		foodHistory.nextId = foodHistory.dictionary.size();

		if (compoundTag.contains(RECENT_HISTORY_NBT_KEY, 9)) {
			NbtList nbtRecentHistory = compoundTag.getList(RECENT_HISTORY_NBT_KEY, 3);

			for (NbtElement tag : nbtRecentHistory) {
				// Using forceEnqueue here to make sure we're not running out of space and throwing an exception.
				// The history length might have changed (decreased) since the last time the player logged in.
				foodHistory.history.forceEnqueue(((NbtInt) tag).intValue());
			}
		}

		foodHistory.buildStats();

		if (compoundTag.contains(CARROT_HISTORY_NBT_KEY, 9)) {
			NbtList nbtCarrotHistory = compoundTag.getList(CARROT_HISTORY_NBT_KEY, 10);
			foodHistory.carrotHistory = new HashSet<>(nbtCarrotHistory.size());
			for (NbtElement tag : nbtCarrotHistory) {
				if (tag instanceof NbtCompound carrotEntry) {
					FoodHistoryEntry entry = new FoodHistoryEntry().read(carrotEntry);
					if (entry != null) {
						foodHistory.carrotHistory.add(entry);
					}
				}
			}
		}

		return foodHistory;
	}

	public void buildStats() {
		stats.clear();
		history.forEach(id -> {
			if (stats.containsKey(id)) {
				stats.put(id, stats.get(id) + 1);
			} else {
				stats.put(id, 1);
			}
		});
	}

	public void defragmentDictionary() {
		Int2IntMap oldToNewMap = new Int2IntArrayMap();
		int i = 0;
		for (Integer id : dictionary.keySet()) {
			oldToNewMap.put((int) id, i);
			i++;
		}
		nextId = i;
		int historySize = history.size();
		for (int j = 0; j < historySize; j++) {
			history.enqueue(oldToNewMap.get(history.dequeue()));
		}
		Int2IntMap newStats = new Int2IntArrayMap();
		for (Int2IntMap.Entry entry : stats.int2IntEntrySet()) {
			newStats.put(oldToNewMap.get(entry.getIntKey()), entry.getIntValue());
		}
		stats = newStats;
		BiMap<Integer, FoodHistoryEntry> newDictionary = HashBiMap.create();
		for (Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
			newDictionary.put(oldToNewMap.get((int) entry.getKey()), entry.getValue());
		}
		dictionary = newDictionary;
	}

	public int getTimesEaten(ItemStack stack) {
		Integer id = dictionary.inverse().get(FoodHistoryEntry.fromItemStack(stack));
		if (id == null) {
			return 0;
		}
		return stats.getOrDefault((int) id, 0);
	}

	public int getLastEaten(ItemStack stack) {
		Integer id = dictionary.inverse().get(FoodHistoryEntry.fromItemStack(stack));
		if (id == null) {
			return -1;
		}
		IntIterator iterator = history.iterator();
		int foundI = Integer.MIN_VALUE;
		while (iterator.hasNext()) {
			foundI++;
			if (iterator.nextInt() == id) {
				foundI = 0;
			}
		}
		return foundI;
	}

	public void addFood(ItemStack stack, ServerPlayerEntity serverPlayerEntity) {
		FoodHistoryEntry entry = FoodHistoryEntry.fromItemStack(stack);

		if (ServerPlayNetworking.canSend(serverPlayerEntity, SpiceOfFabric.ADD_FOOD_S2C_PACKET)) {
			PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
			entry.write(buffer);
			ServerPlayNetworking.send(serverPlayerEntity, SpiceOfFabric.ADD_FOOD_S2C_PACKET, buffer);
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
		if (history.getLength() != Config.food.historyLength) {
			history.setLength(Config.food.historyLength);
			buildStats();
		}

		if (history.size() == Config.food.historyLength) {
			// History is full, overwrite would happen
			int oldestEntryId = history.dequeue(); // Save the oldest value
			stats.computeIfPresent(oldestEntryId, (id_, count) -> count - 1); // Remove from stats
		}

		history.enqueue(id);
		stats.mergeInt(id, 1, Integer::sum);

		if (Config.carrot.enable) {
			carrotHistory.add(entry);
		}
	}

	public int getHistorySize() {
		return history.size();
	}

	public ItemStack getStackFromHistory(int index) {
		if (index < 0 || index >= history.size()) {
			return null;
		}
		return dictionary.get(history.get(index)).getStack();
	}

	public boolean isInCarrotHistory(ItemStack stack) {
		FoodHistoryEntry entry = FoodHistoryEntry.fromItemStack(stack);
		return carrotHistory.contains(entry);
	}

	public int getCarrotHealthOffset(PlayerEntity player) {
		EntityAttributeInstance maxHealthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		Config.setHealthFormulaExpressionValues(carrotHistory.size(), (int) maxHealthAttr.getBaseValue());

		int newMaxHealth = MathHelper.floor(Config.healthFormulaExpression.evaluate());
		if (Config.carrot.maxHealth > 0) {
			newMaxHealth = MathHelper.clamp(newMaxHealth, 1, Config.carrot.maxHealth);
		}
		return newMaxHealth - (int) maxHealthAttr.getBaseValue();
	}

	public int getCarrotMaxHealth(PlayerEntity player) {
		EntityAttributeInstance maxHealthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		Config.setHealthFormulaExpressionValues(carrotHistory.size(), (int) maxHealthAttr.getBaseValue());
		return MathHelper.floor(Config.healthFormulaExpression.evaluate());
	}
}
