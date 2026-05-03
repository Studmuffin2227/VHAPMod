package com.example.vhapmod.event;

import com.example.vhapmod.VaultHuntersAPMod;
import com.example.vhapmod.VaultHuntersManager;
import com.example.vhapmod.item.ModItems;
import iskallia.vault.core.event.common.ChestGenerationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class VaultChestEventListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    // Track which chests have already been processed to prevent duplicates
    private static final Map<String, Boolean> processedChests = new HashMap<>();

    public static void onChestGeneration(ChestGenerationEvent.Data data) {
        Player player = data.getPlayer();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        VaultHuntersManager manager = VaultHuntersAPMod.getManager();
        if (manager == null) {
            return;
        }

        // Only process during PRE phase (once per chest)
        if (data.getPhase() != ChestGenerationEvent.Phase.PRE) {
            return;
        }

        // Get chest position to create unique key
        BlockPos chestPos = data.getPos();
        String chestKey = player.getUUID() + "_" + chestPos.toShortString();

        // Check if we've already processed this chest
        if (processedChests.containsKey(chestKey)) {
            LOGGER.debug("Chest already processed: {}", chestKey);
            return;
        }

        // Mark this chest as processed
        processedChests.put(chestKey, true);

        // Clean up old entries (keep only last 100 chests)
        if (processedChests.size() > 1000) {
            Iterator<String> it = processedChests.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        // Roll the dice - does this chest have a check?
        float checkChance = manager.getNormalChestWeight();
        if (RANDOM.nextFloat() >= checkChance) {
            LOGGER.debug("Chest didn't win check lottery ({}% chance)", checkChance * 100);
            return;
        }

        // Get the next check location ID
        long locationId = manager.getNextChestCheckIdForItem(serverPlayer);
        if (locationId == 0L) {
            return;
        }

        int checkNumber = (int)(locationId - 44000L + 1);
        LOGGER.info("Player {} found chest check #{} at pos {} ({}% chance)",
                serverPlayer.getName().getString(), checkNumber, chestPos, (checkChance * 100));

        // Create the CLAIMABLE check item
        ItemStack checkItem = ModItems.getCheckItemStack(locationId);

        // Give directly to player inventory
        boolean added = serverPlayer.getInventory().add(checkItem);

        if (!added) {
            serverPlayer.drop(checkItem, false);
            LOGGER.debug("Dropped check item (inventory full)");
        } else {
            LOGGER.debug("Added check item to player inventory");
        }
    }

    /**
     * Clear processed chests for a player (call on logout)
     */
    public static void clearPlayerChests(UUID playerId) {
        processedChests.keySet().removeIf(key -> key.startsWith(playerId.toString()));
    }
}
