package studmuffin.vhapmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studmuffin.vhapmod.item.ModItems;

import java.util.Locale;
import java.util.Random;

import static studmuffin.vhapmod.APSkillLockManager.syncToClient;

/**
 * Listens for Forge events and VH-specific events to track progression
 */
@Mod.EventBusSubscriber(modid = "apvaulthuntersmod")
public class VaultEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static VaultHuntersManager manager;
    private static final Random RANDOM = new Random();

    public static void setManager(VaultHuntersManager vhManager) {
        manager = vhManager;
    }

    // ==================== FORGE EVENTS ====================

    /**
     * Track when players log in - connect to AP and check their VH data
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player {} logged in - checking VH progression", player.getName().getString());

            // Load player's unlocked skills/talents/expertises/mods from disk
            APSkillLockManager.loadPlayer(player);

            // Connect to AP server
            /*
            APIntegration apIntegration = VaultHuntersAPMod.getAPIntegration();
            if (apIntegration != null) {
                apIntegration.connectToAP(player, player.getName().getString());
            }
            */
            // Log their current progression (for testing)
            VHDataReader.logPlayerProgression(player);

            // Check player progression against AP
            checkPlayerProgression(player);
            if (manager != null) {
                manager.grantStartingKitIfEnabled(player);
            }

            APWebSocketClient apClient = VaultHuntersAPMod.getAPClient();
            if (apClient != null) {
                apClient.flushPendingReceivedItems();
            }

            // Sync Client To Server
            syncToClient(player);
        }
    }

    /**
     * Clean up when players log out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player {} logged out", player.getName().getString());

            VHProgressionTracker.onPlayerLogout(player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        VaultHuntersManager activeManager = manager != null ? manager : VaultHuntersAPMod.getManager();
        if (!(event.getPlayer() instanceof ServerPlayer player) || activeManager == null) {
            return;
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
        if (blockId == null || !"the_vault".equals(blockId.getNamespace())) {
            return;
        }

        String path = blockId.getPath().toLowerCase(Locale.ROOT);
        float chestChance = activeManager.getNormalChestWeight();
        if (isCoinPile(event.getState(), path)) {
            rollBits(player, chestChance / 5.0F, 1, 1, "coin pile");
        } else if (isVaultOre(event.getState(), path)) {
            rollBits(player, chestChance * 0.9F, 3, 5, "ore");
        }
    }

    private static boolean isVaultOre(BlockState state, String path) {
        return state.getBlock().getClass().getName().contains("VaultOreBlock")
                || (path.contains("ore") && !path.contains("decor"));
    }

    private static boolean isCoinPile(BlockState state, String path) {
        String className = state.getBlock().getClass().getName();
        return className.contains("CoinPile")
                || path.contains("coin_pile")
                || path.contains("coins")
                || path.contains("vault_coin");
    }

    private static void rollBits(ServerPlayer player, float chance, int min, int max, String source) {
        if (RANDOM.nextFloat() >= chance) {
            return;
        }

        int amount = min + RANDOM.nextInt(max - min + 1);
        ItemStack stack = ModItems.getBitsStack(amount);
        if (!player.getInventory().add(stack)) {
            ItemEntity dropped = player.drop(stack, false);
            if (dropped != null) {
                dropped.setNoPickUpDelay();
                dropped.setOwner(player.getUUID());
            }
        }
        LOGGER.info("Gave {} Archipela-bit(s) from {}", amount, source);
    }

    /**
     * Track level changes - for milestone checks
     */
    @SubscribeEvent
    public static void onPlayerLevelChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // We'll check level milestones periodically
            checkLevelMilestones(player);
        }
    }

    // ==================== VH PROGRESSION CHECKING ====================

    /**
     * Check a player's current VH progression and send any missing checks
     */
    private static void checkPlayerProgression(ServerPlayer player) {
        try {
            // Try to access VH's player data using reflection
            LOGGER.info("Attempting to read VH player data for {}", player.getName().getString());

            // The VHProgressionTracker handles this now

        } catch (Exception e) {
            LOGGER.error("Failed to read VH player data", e);
        }
    }

    /**
     * Check if player has reached any level milestones
     */
    private static void checkLevelMilestones(ServerPlayer player) {
        if (manager == null) return;

        try {
            // Get actual VH level
            int level = VHDataReader.getPlayerLevel(player);

            if (level >= 10) manager.onLevelMilestone(player, 10);
            if (level >= 20) manager.onLevelMilestone(player, 20);
            if (level >= 40) manager.onLevelMilestone(player, 40);
            if (level >= 50) manager.onLevelMilestone(player, 50);
            if (level >= 100) manager.onLevelMilestone(player, 100);

        } catch (Exception e) {
            LOGGER.error("Failed to check level milestones", e);
        }
    }
}
