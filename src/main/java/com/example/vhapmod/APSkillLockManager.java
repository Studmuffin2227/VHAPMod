package com.example.vhapmod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.vhapmod.network.APUnlockSyncMessage;
import net.minecraftforge.network.NetworkDirection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.MutableComponent;

/**
 * Manages which skills are unlocked by Archipelago for each player.
 * Skills are locked by default and must be unlocked by receiving items from AP.
 */
public class APSkillLockManager {

    private static final Logger LOGGER = LogManager.getLogger();

    // Track unlocked skills per player
    // UUID -> Set of unlocked skill names
    private static final Map<UUID, Set<String>> unlockedSkills = new HashMap<>();
    private static final Map<UUID, Set<String>> unlockedTalents = new HashMap<>();
    private static final Map<UUID, Set<String>> unlockedExpertises = new HashMap<>();
    private static final Map<UUID, Set<String>> unlockedMods = new HashMap<>();

    // Debug mode - set to true to unlock everything (for testing without AP)
    private static boolean DEBUG_MODE = false;

    /**
     * Check if a skill is unlocked for a player
     */
    public static boolean isSkillUnlocked(ServerPlayer player, String skillName) {
        if (DEBUG_MODE) {
            return true; // All skills unlocked in debug mode
        }

        UUID uuid = player.getUUID();

        // Normalize the skill name
        String normalizedName = normalizeSkillName(skillName);

        // Check if player has this skill unlocked
        Set<String> playerSkills = unlockedSkills.getOrDefault(uuid, new HashSet<>());
        boolean unlocked = playerSkills.contains(normalizedName);

        if (!unlocked) {
            // Send message to player
            net.minecraft.network.chat.MutableComponent message =
                    new TextComponent("[AP] ")
                            .withStyle(net.minecraft.ChatFormatting.GREEN)
                            .append(new TextComponent("This skill is locked! You need to receive it from Archipelago first."));

            player.sendMessage(message, player.getUUID());
            LOGGER.info("Player {} tried to unlock locked skill: {}", player.getName().getString(), skillName);
        }

        return unlocked;
    }

    /**
     * Silent check methods for enforcer (no warning messages)
     */
    public static boolean isSkillUnlockedSilent(ServerPlayer player, String skillName) {
        if (DEBUG_MODE) return true;
        UUID uuid = player.getUUID();
        String normalizedName = normalizeSkillName(skillName);
        Set<String> playerSkills = unlockedSkills.getOrDefault(uuid, new HashSet<>());
        return playerSkills.contains(normalizedName);
    }

    public static boolean isExpertiseUnlockedSilent(ServerPlayer player, String expertiseName) {
        if (DEBUG_MODE) return true;
        UUID uuid = player.getUUID();
        String normalizedName = normalizeExpertiseName(expertiseName);
        Set<String> playerExpertises = unlockedExpertises.getOrDefault(uuid, new HashSet<>());
        return playerExpertises.contains(normalizedName);
    }

    public static boolean isModUnlockedSilent(ServerPlayer player, String modName) {
        if (DEBUG_MODE) return true;
        UUID uuid = player.getUUID();
        String normalizedName = normalizeModName(modName);
        Set<String> playerMods = unlockedMods.getOrDefault(uuid, new HashSet<>());
        return playerMods.contains(normalizedName);
    }
    public static boolean isTalentUnlockedSilent(ServerPlayer player, String talentName) {
        if (DEBUG_MODE) {
            return true;
        }

        UUID uuid = player.getUUID();
        String normalizedName = normalizeTalentName(talentName);

        Set<String> playerTalents = unlockedTalents.getOrDefault(uuid, new HashSet<>());
        return playerTalents.contains(normalizedName);
    }

    /**
     * Check if a talent is unlocked for a player
     */
    public static boolean isTalentUnlocked(ServerPlayer player, String talentName) {
        if (DEBUG_MODE) {
            return true;
        }

        UUID uuid = player.getUUID();
        String normalizedName = normalizeTalentName(talentName);

        Set<String> playerTalents = unlockedTalents.getOrDefault(uuid, new HashSet<>());
        return playerTalents.contains(normalizedName);
    }

    /**
     * Check if an expertise is unlocked for a player
     */
    public static boolean isExpertiseUnlocked(ServerPlayer player, String expertiseName) {
        if (DEBUG_MODE) {
            return true;
        }

        UUID uuid = player.getUUID();
        String normalizedName = normalizeExpertiseName(expertiseName);

        Set<String> playerExpertises = unlockedExpertises.getOrDefault(uuid, new HashSet<>());
        return playerExpertises.contains(normalizedName);
    }

    /**
     * Check if a mod is unlocked for a player
     */
    public static boolean isModUnlocked(ServerPlayer player, String modName) {
        if (DEBUG_MODE) {
            return true;
        }

        UUID uuid = player.getUUID();
        String normalizedName = normalizeModName(modName);

        Set<String> playerMods = unlockedMods.getOrDefault(uuid, new HashSet<>());
        return playerMods.contains(normalizedName);
    }

    /**
     * Unlock a skill for a player
     */
    public static void unlockSkill(ServerPlayer player, String skillName) {
        UUID uuid = player.getUUID();
        String normalizedName = normalizeSkillName(skillName);

        Set<String> playerSkills = unlockedSkills.computeIfAbsent(uuid, k -> new HashSet<>());
        playerSkills.add(normalizedName);

        LOGGER.info("=== UNLOCKING SKILL ===");
        LOGGER.info("Input: '{}' for player UUID={}", skillName, uuid);
        LOGGER.info("Normalized: '{}'", normalizedName);
        LOGGER.info("All unlocked skills for player: {}", playerSkills);
        LOGGER.info("========================");

        net.minecraft.network.chat.MutableComponent skillMessage =
                new TextComponent("[AP] ")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked skill: " + skillName));
        player.sendMessage(skillMessage, uuid);
    }

    /**
     * Unlock a talent for a player
     */
    public static void unlockTalent(ServerPlayer player, String talentName) {
        UUID uuid = player.getUUID();
        String normalizedName = normalizeTalentName(talentName);

        Set<String> playerTalents = unlockedTalents.computeIfAbsent(uuid, k -> new HashSet<>());
        playerTalents.add(normalizedName);


        LOGGER.info("=== UNLOCKING TALENT ===");
        LOGGER.info("Input: '{}' for player UUID={}", talentName, uuid);
        LOGGER.info("Normalized: '{}'", normalizedName);
        LOGGER.info("All unlocked talents for player: {}", playerTalents);
        LOGGER.info("========================");

        net.minecraft.network.chat.MutableComponent talentMessage =
                new TextComponent("[AP] ")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked talent: " + talentName));
        player.sendMessage(talentMessage, uuid);
    }

    /**
     * Unlock an expertise for a player
     */
    public static void unlockExpertise(ServerPlayer player, String expertiseName) {
        UUID uuid = player.getUUID();
        String normalizedName = normalizeExpertiseName(expertiseName);

        Set<String> playerExpertises = unlockedExpertises.computeIfAbsent(uuid, k -> new HashSet<>());
        playerExpertises.add(normalizedName);

        LOGGER.info("=== UNLOCKING EXPERTISE ===");
        LOGGER.info("Input: '{}'", expertiseName);
        LOGGER.info("Normalized: '{}'", normalizedName);
        LOGGER.info("========================");

        net.minecraft.network.chat.MutableComponent expertiseMessage =
                new TextComponent("[AP] ")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked expertise: " + expertiseName));
        player.sendMessage(expertiseMessage, uuid);
    }

    /**
     * Unlock a mod for a player
     */
    public static void unlockMod(ServerPlayer player, String modName) {
        UUID uuid = player.getUUID();
        String normalizedName = normalizeModName(modName);

        Set<String> playerMods = unlockedMods.computeIfAbsent(uuid, k -> new HashSet<>());
        playerMods.add(normalizedName);

        LOGGER.info("Unlocked mod for {}: {}", player.getName().getString(), modName);
        net.minecraft.network.chat.MutableComponent modMessage =
                new TextComponent("[AP] ")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked mod: " + modName));
        player.sendMessage(modMessage, player.getUUID());

    }

    /**
     * Clear all unlocks for a player (on logout)
     */
    public static void clearPlayer(UUID uuid) {
        unlockedSkills.remove(uuid);
        unlockedTalents.remove(uuid);
        unlockedExpertises.remove(uuid);
        unlockedMods.remove(uuid);
    }

    /**
     * Set debug mode (unlocks everything for testing)
     */
    public static void setDebugMode(boolean enabled) {
        DEBUG_MODE = enabled;
        LOGGER.info("AP Debug Mode: " + (enabled ? "ENABLED (all skills unlocked)" : "DISABLED (AP required)"));
    }

    // Normalization methods to match VH's naming format
    private static String normalizeSkillName(String name) {
        // VH uses "Mega Jump", we track as "vhskill:mega_jump"
        return "vhskill:" + name.toLowerCase().replace(" ", "_");
    }

    private static String normalizeTalentName(String name) {
        return "vhtalent:" + name.toLowerCase().replace(" ", "_");
    }

    private static String normalizeExpertiseName(String name) {
        return "vhexpertise:" + name.toLowerCase().replace(" ", "_");
    }

    private static String normalizeModName(String name) {
        return "vhmod:" + name.toLowerCase().replace(" ", "_");
    }
    public static Set<String> getUnlockedSkills(ServerPlayer player) {
        return unlockedSkills.getOrDefault(player.getUUID(), new HashSet<>());
    }

    public static Set<String> getUnlockedTalents(ServerPlayer player) {
        return unlockedTalents.getOrDefault(player.getUUID(), new HashSet<>());
    }

    public static Set<String> getUnlockedMods(ServerPlayer player) {
        return unlockedMods.getOrDefault(player.getUUID(), new HashSet<>());
    }

    public static Set<String> getUnlockedExpertises(ServerPlayer player) {
        return unlockedExpertises.getOrDefault(player.getUUID(), new HashSet<>());
    }
    // Add these to APSkillLockManager.java

    // Client-side caches (for GUI greying)
    private static Set<String> clientUnlockedSkills = new HashSet<>();
    private static Set<String> clientUnlockedTalents = new HashSet<>();
    private static Set<String> clientUnlockedMods = new HashSet<>();
    private static Set<String> clientUnlockedExpertises = new HashSet<>();

    // Client-side setters (called by sync packet)
    public static void setClientUnlockedSkills(Set<String> skills) {
        clientUnlockedSkills = new HashSet<>(skills);
    }

    public static void setClientUnlockedTalents(Set<String> talents) {
        clientUnlockedTalents = new HashSet<>(talents);
    }

    public static void setClientUnlockedMods(Set<String> mods) {
        clientUnlockedMods = new HashSet<>(mods);
    }

    public static void setClientUnlockedExpertises(Set<String> expertises) {
        clientUnlockedExpertises = new HashSet<>(expertises);
    }

    // Client-side lock checking (for GUI)
    public static boolean isSkillUnlockedClient(String normalizedName) {
        return clientUnlockedSkills.contains(normalizedName);
    }

    public static boolean isTalentUnlockedClient(String normalizedName) {
        return clientUnlockedTalents.contains(normalizedName);
    }

    public static boolean isModUnlockedClient(String normalizedName) {
        return clientUnlockedMods.contains(normalizedName);
    }

    public static boolean isExpertiseUnlockedClient(String normalizedName) {
        return clientUnlockedExpertises.contains(normalizedName);
    }
    // Send sync packet to client
    public static void syncToClient(ServerPlayer player) {
        UUID playerId = player.getUUID();

        Set<String> skills = unlockedSkills.getOrDefault(playerId, new HashSet<>());
        Set<String> talents = unlockedTalents.getOrDefault(playerId, new HashSet<>());
        Set<String> mods = unlockedMods.getOrDefault(playerId, new HashSet<>());
        Set<String> expertises = unlockedExpertises.getOrDefault(playerId, new HashSet<>());

        APUnlockSyncMessage message = new APUnlockSyncMessage(skills, talents, mods, expertises);

        // Use ModNetwork.CHANNEL instead of just CHANNEL
        ModNetwork.CHANNEL.sendTo(
                message,
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
    }
}