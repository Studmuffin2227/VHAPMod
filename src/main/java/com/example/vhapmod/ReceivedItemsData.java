package com.example.vhapmod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class ReceivedItemsData extends SavedData {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DATA_NAME = "apvaulthuntersmod_received_items";

    private Set<String> processedItems = new HashSet<>();

    public ReceivedItemsData() {
    }

    public ReceivedItemsData(CompoundTag tag) {
        ListTag list = tag.getList("ProcessedItems", 8);
        for (int i = 0; i < list.size(); i++) {
            processedItems.add(list.getString(i));
        }
        LOGGER.info("Loaded {} processed items from disk", processedItems.size());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String item : processedItems) {
            list.add(StringTag.valueOf(item));
        }
        tag.put("ProcessedItems", list);
        LOGGER.info("Saved {} processed items to disk", processedItems.size());
        return tag;
    }

    public boolean hasProcessed(String uniqueKey) {
        return processedItems.contains(uniqueKey);
    }

    public void markProcessed(String uniqueKey) {
        processedItems.add(uniqueKey);
        setDirty();
    }

    public void clear() {
        processedItems.clear();
        setDirty();
    }

    public int getProcessedCount() {
        return processedItems.size();
    }

    public static ReceivedItemsData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                ReceivedItemsData::new,
                ReceivedItemsData::new,
                DATA_NAME
        );
    }
}