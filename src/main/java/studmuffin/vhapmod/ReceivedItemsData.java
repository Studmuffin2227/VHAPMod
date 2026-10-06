package studmuffin.vhapmod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReceivedItemsData extends SavedData {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DATA_NAME = "apvaulthuntersmod_received_items";

    private Set<String> processedItems = new HashSet<>();
    private List<PendingItem> pendingItems = new ArrayList<>();
    private List<PendingPlayerItem> pendingPlayerItems = new ArrayList<>();
    private Set<UUID> knownPlayers = new HashSet<>();
    private String connectionSignature = "";

    public ReceivedItemsData() {
    }

    public ReceivedItemsData(CompoundTag tag) {
        this.connectionSignature = tag.getString("ConnectionSignature");
        ListTag list = tag.getList("ProcessedItems", 8);
        for (int i = 0; i < list.size(); i++) {
            processedItems.add(list.getString(i));
        }
        ListTag pending = tag.getList("PendingItems", 10);
        for (int i = 0; i < pending.size(); i++) {
            CompoundTag entry = pending.getCompound(i);
            pendingItems.add(new PendingItem(
                    entry.getString("Key"),
                    entry.getLong("ItemId"),
                    entry.getLong("LocationId"),
                    entry.getInt("SendingPlayer"),
                    entry.getInt("Flags")
            ));
        }
        ListTag pendingPlayers = tag.getList("PendingPlayerItems", 10);
        for (int i = 0; i < pendingPlayers.size(); i++) {
            CompoundTag entry = pendingPlayers.getCompound(i);
            if (!entry.hasUUID("PlayerId")) {
                continue;
            }
            pendingPlayerItems.add(new PendingPlayerItem(
                    entry.getUUID("PlayerId"),
                    entry.getString("Key"),
                    entry.getLong("ItemId"),
                    entry.getLong("LocationId"),
                    entry.getInt("SendingPlayer"),
                    entry.getInt("Flags")
            ));
        }
        ListTag players = tag.getList("KnownPlayers", 10);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            if (entry.hasUUID("PlayerId")) {
                knownPlayers.add(entry.getUUID("PlayerId"));
            }
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
        ListTag pending = new ListTag();
        for (PendingItem item : pendingItems) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Key", item.uniqueKey);
            entry.putLong("ItemId", item.itemId);
            entry.putLong("LocationId", item.locationId);
            entry.putInt("SendingPlayer", item.sendingPlayer);
            entry.putInt("Flags", item.flags);
            pending.add(entry);
        }
        tag.put("PendingItems", pending);
        ListTag pendingPlayers = new ListTag();
        for (PendingPlayerItem item : pendingPlayerItems) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("PlayerId", item.playerId);
            entry.putString("Key", item.uniqueKey);
            entry.putLong("ItemId", item.itemId);
            entry.putLong("LocationId", item.locationId);
            entry.putInt("SendingPlayer", item.sendingPlayer);
            entry.putInt("Flags", item.flags);
            pendingPlayers.add(entry);
        }
        tag.put("PendingPlayerItems", pendingPlayers);
        ListTag players = new ListTag();
        for (UUID playerId : knownPlayers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("PlayerId", playerId);
            players.add(entry);
        }
        tag.put("KnownPlayers", players);
        tag.putString("ConnectionSignature", connectionSignature);
        LOGGER.info("Saved {} processed items to disk", processedItems.size());
        return tag;
    }

    public boolean hasProcessed(String uniqueKey) {
        return processedItems.contains(uniqueKey);
    }

    public void markProcessed(String uniqueKey) {
        processedItems.add(uniqueKey);
        pendingItems.removeIf(item -> item.uniqueKey.equals(uniqueKey));
        setDirty();
    }

    public void addPending(String uniqueKey, long itemId, long locationId, int sendingPlayer, int flags) {
        if (hasProcessed(uniqueKey)) {
            return;
        }
        for (PendingItem item : pendingItems) {
            if (item.uniqueKey.equals(uniqueKey)) {
                return;
            }
        }
        pendingItems.add(new PendingItem(uniqueKey, itemId, locationId, sendingPlayer, flags));
        setDirty();
    }

    public List<PendingItem> getPendingItems() {
        return new ArrayList<>(pendingItems);
    }

    public void addKnownPlayer(UUID playerId) {
        if (playerId != null && knownPlayers.add(playerId)) {
            setDirty();
        }
    }

    public Set<UUID> getKnownPlayers() {
        return new HashSet<>(knownPlayers);
    }

    public void addPendingForPlayer(UUID playerId, String uniqueKey, long itemId, long locationId, int sendingPlayer, int flags) {
        if (playerId == null || hasProcessed(uniqueKey)) {
            return;
        }
        for (PendingPlayerItem item : pendingPlayerItems) {
            if (item.playerId.equals(playerId) && item.uniqueKey.equals(uniqueKey)) {
                return;
            }
        }
        pendingPlayerItems.add(new PendingPlayerItem(playerId, uniqueKey, itemId, locationId, sendingPlayer, flags));
        setDirty();
    }

    public List<PendingPlayerItem> getPendingItemsForPlayer(UUID playerId) {
        List<PendingPlayerItem> result = new ArrayList<>();
        if (playerId == null) {
            return result;
        }
        for (PendingPlayerItem item : pendingPlayerItems) {
            if (item.playerId.equals(playerId)) {
                result.add(item);
            }
        }
        return result;
    }

    public void removePendingForPlayer(UUID playerId, String uniqueKey) {
        pendingPlayerItems.removeIf(item -> item.playerId.equals(playerId) && item.uniqueKey.equals(uniqueKey));
        setDirty();
    }

    public void removePending(String uniqueKey) {
        pendingItems.removeIf(item -> item.uniqueKey.equals(uniqueKey));
        setDirty();
    }

    public void clear() {
        processedItems.clear();
        pendingItems.clear();
        pendingPlayerItems.clear();
        setDirty();
    }

    public boolean syncConnectionSignature(String newSignature) {
        String signature = newSignature == null ? "" : newSignature;
        if (signature.isBlank()) {
            return false;
        }
        if (connectionSignature.isBlank()) {
            connectionSignature = signature;
            clear();
            LOGGER.info("Initialized AP received-item cache for {}; cleared old unscoped entries", signature);
            return false;
        }
        if (!connectionSignature.equals(signature)) {
            LOGGER.info("AP connection changed from {} to {}; clearing received-item cache", connectionSignature, signature);
            connectionSignature = signature;
            clear();
            return true;
        }
        return false;
    }

    public int getProcessedCount() {
        return processedItems.size();
    }

    public int getProcessedItemCount(long itemId) {
        String prefix = itemId + ":";
        int count = 0;
        for (String processedItem : processedItems) {
            if (processedItem.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    public static ReceivedItemsData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                ReceivedItemsData::new,
                ReceivedItemsData::new,
                DATA_NAME
        );
    }

    public static class PendingItem {
        public final String uniqueKey;
        public final long itemId;
        public final long locationId;
        public final int sendingPlayer;
        public final int flags;

        public PendingItem(String uniqueKey, long itemId, long locationId, int sendingPlayer, int flags) {
            this.uniqueKey = uniqueKey;
            this.itemId = itemId;
            this.locationId = locationId;
            this.sendingPlayer = sendingPlayer;
            this.flags = flags;
        }
    }

    public static class PendingPlayerItem extends PendingItem {
        public final UUID playerId;

        public PendingPlayerItem(UUID playerId, String uniqueKey, long itemId, long locationId, int sendingPlayer, int flags) {
            super(uniqueKey, itemId, locationId, sendingPlayer, flags);
            this.playerId = playerId;
        }
    }
}
