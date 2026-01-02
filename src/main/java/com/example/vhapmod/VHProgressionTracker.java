package com.example.vhapmod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Tracks VH progression in real-time by polling player data every few seconds
 */
@Mod.EventBusSubscriber(modid = "apvaulthuntersmod")
public class VHProgressionTracker {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CHECK_INTERVAL = 100; // Check every 5 seconds (100 ticks)

    private static int tickCounter = 0;
    private static VaultHuntersManager manager;
    private static MinecraftServer server;
    private static int completedQuestCount = 0;

    // Cache of what each player had last time we checked
    private static final Map<UUID, PlayerProgressionCache> playerCache = new HashMap<>();

    /**
     * Set the VaultHuntersManager instance (called from main mod class)
     */
    public static void setManager(VaultHuntersManager vhManager) {
        manager = vhManager;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
    }

    /**
     * Check all online players every 5 seconds
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (manager == null || server == null) return;

        tickCounter++;
        if (tickCounter % CHECK_INTERVAL != 0) return; // Only check every 5 seconds

        // Check all online players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // ENFORCE SKILL LOCKS FIRST
            VHSkillEnforcer.enforceSkillLocks(player);

            // Then check for new unlocks
            checkPlayerProgression(player);
        }
    }

    /**
     * Check a single player's progression and detect new unlocks
     */
    private static void checkPlayerProgression(ServerPlayer player) {
        UUID uuid = player.getUUID();

        // Get current progression - wrapped in try-catch to prevent spam
        int currentLevel = 0;
        Set<String> currentAbilities = new HashSet<>();
        Set<String> currentTalents = new HashSet<>();
        Set<String> currentMods = new HashSet<>();

        try {
            currentLevel = VHDataReader.getPlayerLevel(player);
            currentAbilities = new HashSet<>(VHDataReader.getUnlockedAbilities(player));
            currentTalents = new HashSet<>(VHDataReader.getUnlockedTalents(player));
            currentMods = new HashSet<>(VHDataReader.getResearchedMods(player));
        } catch (Exception e) {
            // Silently skip if VH data not available
            // This happens in creative mode or non-vault worlds
            return;
        }

        // Get cached progression (what they had last check)
        PlayerProgressionCache cache = playerCache.get(uuid);

        if (cache == null) {
            // First time seeing this player - just cache their current state
            cache = new PlayerProgressionCache(currentLevel, currentAbilities, currentTalents, currentMods);
            playerCache.put(uuid, cache);
            LOGGER.info("Initialized progression cache for " + player.getName().getString());
            return;
        }

        boolean anyNewUnlock = false;

        // Check for NEW abilities
        for (String ability : currentAbilities) {
            if (!cache.abilities.contains(ability)) {
                LOGGER.info("NEW ABILITY UNLOCKED: " + player.getName().getString() + " learned " + ability);
                String normalizedName = "vhskill:" + ability.toLowerCase().replace(" ", "_");
                manager.onSkillUnlocked(player, normalizedName);
                anyNewUnlock = true;
            }
        }

        // Check for NEW talents
        for (String talent : currentTalents) {
            if (!cache.talents.contains(talent)) {
                LOGGER.info("NEW TALENT UNLOCKED: " + player.getName().getString() + " learned " + talent);
                String normalizedName = "vhtalent:" + talent.toLowerCase().replace(" ", "_");
                manager.onTalentUnlocked(player, normalizedName);
                anyNewUnlock = true;
            }
        }

        // Check for NEW researched mods
        for (String mod : currentMods) {
            if (!cache.mods.contains(mod)) {
                LOGGER.info("NEW MOD RESEARCHED: " + player.getName().getString() + " researched " + mod);
                String normalizedName = "vhmod:" + mod.toLowerCase().replace(" ", "_");
                manager.onModUnlocked(player, normalizedName);
            }
        }

        // Check for level milestones
        if (currentLevel != cache.level) {
            LOGGER.info("LEVEL UP: " + player.getName().getString() + " is now level " + currentLevel);

            // Check ALL milestones between old and new level
            int[] milestones = {10, 25, 50, 75, 100};
            for (int milestone : milestones) {
                if (cache.level < milestone && currentLevel >= milestone) {
                    LOGGER.info("MILESTONE REACHED: Level " + milestone);
                    manager.onLevelMilestone(player, milestone);
                }
            }

            // Check if goal level reached
            APWebSocketClient client = VaultHuntersAPMod.getAPClient();
            if (client != null) {
                client.checkGoalReached(player);
            }
        }

        // If any new unlock was detected, immediately enforce locks again
        if (anyNewUnlock) {
            LOGGER.info("New unlock detected, immediately enforcing locks...");
            VHSkillEnforcer.enforceSkillLocks(player);
        }

        // Update cache with current state
        cache.level = currentLevel;
        cache.abilities = currentAbilities;
        cache.talents = currentTalents;
        cache.mods = currentMods;
    }

    /**
     * Remove player from cache when they log out
     */
    public static void onPlayerLogout(ServerPlayer player) {
        playerCache.remove(player.getUUID());
        VHSkillEnforcer.clearWarnings(player.getUUID());
        LOGGER.info("Removed progression cache for " + player.getName().getString());
    }

    /**
     * Track quest completions (progressive, not tied to specific quests)
     */
    public static void onQuestCompleted(ServerPlayer player) {
        completedQuestCount++;
        String locationName = "Quest Completion " + completedQuestCount;

        if (manager != null) {
            manager.onQuestCompleted(player, locationName);
            LOGGER.info("Player {} completed quest #{}", player.getName().getString(), completedQuestCount);
        } else {
            LOGGER.warn("Manager is null, cannot send quest check!");
        }
    }

    /**
     * Simple cache to store what a player had last time we checked
     */
    private static class PlayerProgressionCache {
        int level;
        Set<String> abilities;
        Set<String> talents;
        Set<String> mods;

        PlayerProgressionCache(int level, Set<String> abilities, Set<String> talents, Set<String> mods) {
            this.level = level;
            this.abilities = abilities;
            this.talents = talents;
            this.mods = mods;
        }
    }
}