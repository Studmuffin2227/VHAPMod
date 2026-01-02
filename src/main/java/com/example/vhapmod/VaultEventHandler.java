package com.example.vhapmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Listens for Forge events and VH-specific events to track progression
 */
@Mod.EventBusSubscriber(modid = "apvaulthuntersmod")
public class VaultEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static VaultHuntersManager manager;

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
        }
    }

    /**
     * Clean up when players log out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player {} logged out", player.getName().getString());

            // Disconnect from AP
            APWebSocketClient apClient = VaultHuntersAPMod.getAPClient();
            if (apClient != null) {
                apClient.disconnect();
            }

            VHProgressionTracker.onPlayerLogout(player);
        }
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
            if (level >= 25) manager.onLevelMilestone(player, 25);
            if (level >= 50) manager.onLevelMilestone(player, 50);
            if (level >= 75) manager.onLevelMilestone(player, 75);
            if (level >= 100) manager.onLevelMilestone(player, 100);

        } catch (Exception e) {
            LOGGER.error("Failed to check level milestones", e);
        }
    }
}