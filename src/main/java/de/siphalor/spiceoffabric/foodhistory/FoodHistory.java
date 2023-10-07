package de.siphalor.spiceoffabric.foodhistory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.queue.ArrayFixedLengthIntFIFOQueue;
import de.siphalor.spiceoffabric.util.queue.FixedLengthIntFIFOQueueWithStats;
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

	protected final BiMap<Integer, FoodHistoryEntry> dictionary;
	protected int nextId = 0;

	protected final FixedLengthIntFIFOQueueWithStats recentlyEaten;

	protected final Set<FoodHistoryEntry> uniqueFoodsEaten;

	public FoodHistory() {
		dictionary = HashBiMap.create();
		recentlyEaten = new FixedLengthIntFIFOQueueWithStats(new ArrayFixedLengthIntFIFOQueue(Config.food.historyLength));
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

	public Set<FoodHistoryEntry> getUniqueFoodsEaten() {
		return uniqueFoodsEaten;
	}

	public void resetUniqueFoodsEaten() {
		uniqueFoodsEaten.clear();
	}

	public void write(PacketByteBuf buffer) {
		buffer.writeVarInt(dictionary.size());
		for (Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
			buffer.writeVarInt(entry.getKey());
			entry.getValue().write(buffer);
		}
		buffer.writeVarInt(recentlyEaten.size());
		for (int integer : recentlyEaten) {
			buffer.writeVarInt(integer);
		}
		if (Config.carrot.enable) {
			buffer.writeBoolean(true);
			buffer.writeVarInt(uniqueFoodsEaten.size());
			for (FoodHistoryEntry entry : uniqueFoodsEaten) {
				entry.write(buffer);
			}
		} else {
			buffer.writeBoolean(false);
		}
	}

	public void read(PacketByteBuf buffer) {
		dictionary.clear();
		recentlyEaten.clear();
		recentlyEaten.setLength(Config.food.historyLength);

		for (int l = buffer.readVarInt(), i = 0; i < l; i++) {
			dictionary.put(buffer.readVarInt(), FoodHistoryEntry.from(buffer));
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
				uniqueFoodsEaten.add(FoodHistoryEntry.from(buffer));
			}
		}
	}

	public NbtCompound write(NbtCompound compoundTag) {
		defragmentDictionary();
		NbtList list = new NbtList();
		for (Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
			list.add(entry.getKey(), entry.getValue().write(new NbtCompound()));
		}
		compoundTag.put(DICTIONARY_NBT_KEY, list);
		NbtList historyList = new NbtList();
		for (Integer id : recentlyEaten) {
			historyList.add(NbtInt.of(id));
		}
		compoundTag.put(RECENT_HISTORY_NBT_KEY, historyList);
		NbtList carrotHistoryList = new NbtList();
		for (FoodHistoryEntry entry : uniqueFoodsEaten) {
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
				foodHistory.recentlyEaten.forceEnqueue(((NbtInt) tag).intValue());
			}
		}

		if (compoundTag.contains(CARROT_HISTORY_NBT_KEY, 9)) {
			NbtList nbtCarrotHistory = compoundTag.getList(CARROT_HISTORY_NBT_KEY, 10);
			for (NbtElement tag : nbtCarrotHistory) {
				if (!(tag instanceof NbtCompound carrotEntry)) {
					continue;
				}
				FoodHistoryEntry entry = new FoodHistoryEntry().read(carrotEntry);
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
		if (recentlyEaten.getLength() != Config.food.historyLength) {
			recentlyEaten.setLength(Config.food.historyLength);
		}

		recentlyEaten.forceEnqueue(id);

		if (Config.carrot.enable) {
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

	public int getCarrotHealthOffset(PlayerEntity player) {
		EntityAttributeInstance maxHealthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		Config.setHealthFormulaExpressionValues(uniqueFoodsEaten.size(), (int) maxHealthAttr.getBaseValue());

		int newMaxHealth = MathHelper.floor(Config.healthFormulaExpression.evaluate());
		if (Config.carrot.maxHealth > 0) {
			newMaxHealth = MathHelper.clamp(newMaxHealth, 1, Config.carrot.maxHealth);
		}
		return newMaxHealth - (int) maxHealthAttr.getBaseValue();
	}

	public int getCarrotMaxHealth(PlayerEntity player) {
		EntityAttributeInstance maxHealthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		Config.setHealthFormulaExpressionValues(uniqueFoodsEaten.size(), (int) maxHealthAttr.getBaseValue());
		return MathHelper.floor(Config.healthFormulaExpression.evaluate());
	}
}
