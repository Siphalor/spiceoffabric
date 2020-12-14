package de.siphalor.spiceoffabric.foodhistory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FoodHistory {

	public Set<FoodHistoryEntry> carrotHistory;

    public BiMap<Integer, FoodHistoryEntry> dictionary;
    public int nextId = 0;
    public Queue<Integer> history;
    public Map<Integer, Integer> stats;

    public FoodHistory() {
    	setup();
    }

    public void setup() {
    	dictionary = HashBiMap.create();
    	history = new ConcurrentLinkedQueue<>();
    	stats = new Int2IntArrayMap();
    	if(Config.carrot.enable)
    		carrotHistory = new HashSet<>();
    }

    public void reset() {
    	dictionary.clear();
    	nextId = 0;
    	history.clear();
    	stats.clear();
    	if(carrotHistory != null) carrotHistory.clear();
	}

	public void write(PacketByteBuf buffer) {
    	buffer.writeVarInt(dictionary.size());
    	for(Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
    		buffer.writeVarInt(entry.getKey());
    		entry.getValue().write(buffer);
		}
        buffer.writeVarInt(history.size());
    	for(int integer : history) {
    		buffer.writeVarInt(integer);
		}
    	if(carrotHistory != null) {
    		buffer.writeBoolean(true);
    		buffer.writeVarInt(carrotHistory.size());
    		for(FoodHistoryEntry entry : carrotHistory) {
    			entry.write(buffer);
			}
		} else
			buffer.writeBoolean(false);
	}

	public void read(PacketByteBuf buffer) {
    	for(int l = buffer.readVarInt(), i = 0; i < l; i++) {
    		dictionary.put(buffer.readVarInt(), FoodHistoryEntry.from(buffer));
		}
    	for(int l = buffer.readVarInt(), i = 0; i < l; i++) {
    		history.offer(buffer.readVarInt());
		}
    	if(buffer.readBoolean()) {
    		final int length = buffer.readVarInt();
    		carrotHistory = new HashSet<>(length);
    		for(int i = 0; i < length; i++) {
    			carrotHistory.add(FoodHistoryEntry.from(buffer));
			}
		}
    	buildStats();
	}

	public CompoundTag write(CompoundTag compoundTag) {
		defragmentDictionary();
		ListTag list = new ListTag();
		for(Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
			list.add(entry.getKey(), entry.getValue().write(new CompoundTag()));
		}
		compoundTag.put("dictionary", list);
		ListTag historyList = new ListTag();
		for(Integer id : history) {
			historyList.add(IntTag.of(id));
		}
		compoundTag.put("history", historyList);
		if(carrotHistory != null) {
			ListTag carrotHistoryList = new ListTag();
			for(FoodHistoryEntry entry : carrotHistory) {
                carrotHistoryList.add(entry.write(new CompoundTag()));
			}
			compoundTag.put("carrotHistory", carrotHistoryList);
		}
		return compoundTag;
	}

	public static FoodHistory read(CompoundTag compoundTag) {
        FoodHistory foodHistory = new FoodHistory();
		ListTag list = (ListTag) compoundTag.get("dictionary");
        for(int i = 0; i < list.size(); i++) {
        	foodHistory.dictionary.put(i, new FoodHistoryEntry().read(list.getCompound(i)));
        }
        foodHistory.nextId = foodHistory.dictionary.size();
        list = (ListTag) compoundTag.get("history");
        for(Tag tag : list) {
        	foodHistory.history.offer(((IntTag) tag).getInt());
        }
        foodHistory.buildStats();

        if(compoundTag.contains("carrotHistory")) {
        	list = (ListTag) compoundTag.get("carrotHistory");
        	if(Config.carrot.enable) {
        		foodHistory.carrotHistory = new HashSet<>();
				for (Tag tag : list) {
					foodHistory.carrotHistory.add(new FoodHistoryEntry().read((CompoundTag) tag));
				}
			}
		}
		return foodHistory;
	}

	public void buildStats() {
    	stats.clear();
    	for(Integer id : history) {
    		if(stats.containsKey(id)) {
				stats.put(id, stats.get(id) + 1);
			}
    		else {
				stats.put(id, 1);
			}
	    }
	}

	public void defragmentDictionary() {
		Map<Integer, Integer> oldToNewMap = new HashMap<>();
		int i = 0;
        for(Integer id : dictionary.keySet()) {
        	oldToNewMap.put(id, i);
        	i++;
        }
        nextId = i;
        Queue<Integer> newHistory = new ConcurrentLinkedQueue<>();
        while(true) {
        	Integer id = history.poll();
        	if(id == null) break;
        	newHistory.offer(id);
        }
        history = newHistory;
        Map<Integer, Integer> newStats = new Int2IntArrayMap();
        for(Map.Entry<Integer, Integer> entry : stats.entrySet()) {
        	newStats.put(oldToNewMap.get(entry.getKey()), entry.getValue());
        }
        stats = newStats;
        BiMap<Integer, FoodHistoryEntry> newDictionary = HashBiMap.create();
        for(Map.Entry<Integer, FoodHistoryEntry> entry : dictionary.entrySet()) {
        	newDictionary.put(oldToNewMap.get(entry.getKey()), entry.getValue());
        }
        dictionary = newDictionary;
	}

	public int getTimesEaten(ItemStack stack) {
    	return stats.getOrDefault(dictionary.inverse().get(FoodHistoryEntry.fromItemStack(stack)), 0);
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
		Integer id = dictionary.inverse().get(entry);
		if(id == null) {
			id = nextId++;
			dictionary.put(id, entry);
		}
		history.offer(id);
		while(history.size() > Config.food.historyLength) {
			removeLastFood();
		}
		stats.put(id, stats.getOrDefault(id, 0) + 1);

		if(Config.carrot.enable) {
			if(carrotHistory == null) {
				System.err.println("Carrot food history is null");
			} else {
				carrotHistory.add(entry);
			}
		}
	}

    public void removeLastFood() {
    	int id = history.remove();
    	if (stats.containsKey(id)) stats.put(id, stats.get(id) - 1);
    }

    public ListTag genJournalPages(PlayerEntity playerEntity) {
    	boolean hasMod = ((IServerPlayerEntity) playerEntity).spiceOfFabric_hasClientMod();

    	ListTag pages = new ListTag();

    	LiteralText textOnPage = new LiteralText("");
		textOnPage.append(
				hasMod
						? new TranslatableText(SpiceOfFabric.MOD_ID + ".journal.inside_title")
						: new LiteralText("\u25b6 Diet Journal \u25c0")
						.setStyle(Style.EMPTY
								.withColor(Formatting.DARK_GRAY)
								.withFormatting(Formatting.UNDERLINE)
								.withBold(true)
						)
		);
		textOnPage.append("\n\n");

		Style numberStyle = Style.EMPTY.withBold(true);
		Style itemStyle = Style.EMPTY.withColor(Formatting.DARK_GRAY);

		int linesOnPage = 2;
		int number = 1;
		for(int foodId : history) {
			FoodHistoryEntry entry = dictionary.get(foodId);
			if(hasMod) {
				textOnPage.append(
						new TranslatableText(
								SpiceOfFabric.MOD_ID + ".journal.line",
								number,
								entry.getStackName()
						).setStyle(
								Style.EMPTY.
										withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
												new HoverEvent.ItemStackContent(entry.getStack())))))
						.append("\n");
			} else {
				textOnPage.append(new LiteralText(number + ". ").setStyle(numberStyle))
						.append(entry.getStackName().setStyle(itemStyle.withHoverEvent(
										new HoverEvent(
												HoverEvent.Action.SHOW_ITEM,
												new HoverEvent.ItemStackContent(entry.getStack())
										)
								).withBold(false)
						)
				).append("\n");
			}
			number++; linesOnPage++;
			if(linesOnPage >= 14) {
				pages.add(StringTag.of(Text.Serializer.toJson(textOnPage)));
				linesOnPage = 0;
				textOnPage = new LiteralText("");
			}
		}

		if(linesOnPage > 0) {
			pages.add(StringTag.of(Text.Serializer.toJson(textOnPage)));
		}

		return pages;
	}
}
