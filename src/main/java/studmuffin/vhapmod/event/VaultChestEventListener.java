package studmuffin.vhapmod.event;

import studmuffin.vhapmod.APGearRarityManager;
import studmuffin.vhapmod.VHDataReader;
import studmuffin.vhapmod.VaultHuntersAPMod;
import studmuffin.vhapmod.VaultHuntersManager;
import studmuffin.vhapmod.item.ModItems;
import iskallia.vault.core.event.common.ChestGenerationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
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

        if (data.getPhase() == ChestGenerationEvent.Phase.POST) {
            int changed = 0;
            int itemLevel = VHDataReader.getPlayerLevel(serverPlayer);
            for (ItemStack stack : data.getLoot()) {
                if (APGearRarityManager.forceStackRarity(stack, itemLevel)) {
                    changed++;
                }
            }
            if (changed > 0) {
                LOGGER.debug("Forced {} generated vault gear stack(s) to {} rarity",
                        changed, APGearRarityManager.getForcedRarityName());
            }
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

        float bitChance = manager.getNormalChestWeight();
        if (RANDOM.nextFloat() >= bitChance) {
            LOGGER.debug("Chest didn't win Archipela-bit lottery ({}% chance)", bitChance * 100);
            return;
        }

        int amount = 1 + RANDOM.nextInt(3);
        LOGGER.info("Player {} found {} Archipela-bit(s) from chest at pos {} ({}% chance)",
                serverPlayer.getName().getString(), amount, chestPos, (bitChance * 100));
        ItemStack inventoryCopy = ModItems.getBitsStack(amount);
        boolean added = serverPlayer.getInventory().add(inventoryCopy);

        if (!added || !inventoryCopy.isEmpty()) {
            ItemStack dropStack = inventoryCopy.copy();
            ItemEntity dropped = serverPlayer.drop(dropStack, false);
            if (dropped != null) {
                dropped.setNoPickUpDelay();
                dropped.setOwner(serverPlayer.getUUID());
                LOGGER.debug("Dropped Archipela-bits (inventory full)");
            } else {
                LOGGER.warn("Failed to add or drop Archipela-bits for {}", serverPlayer.getName().getString());
            }
        } else {
            LOGGER.debug("Added Archipela-bits to player inventory");
        }
    }

    /**
     * Clear processed chests for a player (call on logout)
     */
    public static void clearPlayerChests(UUID playerId) {
        processedChests.keySet().removeIf(key -> key.startsWith(playerId.toString()));
    }
}
