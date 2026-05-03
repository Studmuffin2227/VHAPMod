package com.example.vhapmod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.vhapmod.network.APUnlockSyncMessage;
import net.minecraftforge.network.PacketDistributor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
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
    private static final Map<String, String> SKILL_ALIASES = createSkillAliases();
    private static final Map<String, String> MOD_ALIASES = createModAliases();

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

        // Persist to disk
        APUnlockData unlockData = APUnlockData.get(player.getServer());
        unlockData.addSkill(uuid, normalizedName);

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
        syncToClient(player);
    }

    /**
     * Unlock a talent for a player
     */
    public static void unlockTalent(ServerPlayer player, String talentName) {
        UUID uuid = player.getUUID();
        String normalizedName = normalizeTalentName(talentName);

        Set<String> playerTalents = unlockedTalents.computeIfAbsent(uuid, k -> new HashSet<>());
        playerTalents.add(normalizedName);

        // Persist to disk
        APUnlockData unlockData = APUnlockData.get(player.getServer());
        unlockData.addTalent(uuid, normalizedName);

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
        syncToClient(player);
    }

    /**
     * Unlock an expertise for a player
     */
    public static void unlockExpertise(ServerPlayer player, String expertiseName) {
        UUID uuid = player.getUUID();
        String normalizedName = normalizeExpertiseName(expertiseName);

        Set<String> playerExpertises = unlockedExpertises.computeIfAbsent(uuid, k -> new HashSet<>());
        playerExpertises.add(normalizedName);

        // Persist to disk
        APUnlockData unlockData = APUnlockData.get(player.getServer());
        unlockData.addExpertise(uuid, normalizedName);

        LOGGER.info("=== UNLOCKING EXPERTISE ===");
        LOGGER.info("Input: '{}'", expertiseName);
        LOGGER.info("Normalized: '{}'", normalizedName);
        LOGGER.info("========================");

        net.minecraft.network.chat.MutableComponent expertiseMessage =
                new TextComponent("[AP] ")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked expertise: " + expertiseName));
        player.sendMessage(expertiseMessage, uuid);
        syncToClient(player);
    }

    /**
     * Unlock a mod for a player
     */
    public static void unlockMod(ServerPlayer player, String modName) {
        UUID uuid = player.getUUID();
        String normalizedName = normalizeModName(modName);

        Set<String> playerMods = unlockedMods.computeIfAbsent(uuid, k -> new HashSet<>());
        playerMods.add(normalizedName);

        // Persist to disk
        APUnlockData unlockData = APUnlockData.get(player.getServer());
        unlockData.addMod(uuid, normalizedName);

        LOGGER.info("Unlocked mod for {}: {}", player.getName().getString(), modName);
        net.minecraft.network.chat.MutableComponent modMessage =
                new TextComponent("[AP] ")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked mod: " + modName));
        player.sendMessage(modMessage, player.getUUID());
        syncToClient(player);
    }

    /**
     * Load unlocks for a player from disk (on login)
     */
    public static void loadPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        APUnlockData unlockData = APUnlockData.get(player.getServer());

        // Load from persistent storage
        unlockedSkills.put(uuid, unlockData.getUnlockedSkills(uuid));
        unlockedTalents.put(uuid, unlockData.getUnlockedTalents(uuid));
        unlockedExpertises.put(uuid, unlockData.getUnlockedExpertises(uuid));
        unlockedMods.put(uuid, unlockData.getUnlockedMods(uuid));

        LOGGER.info("Loaded {} skills, {} talents, {} expertises, {} mods for player {}",
                unlockedSkills.get(uuid).size(),
                unlockedTalents.get(uuid).size(),
                unlockedExpertises.get(uuid).size(),
                unlockedMods.get(uuid).size(),
                player.getName().getString());
    }

    /**
     * Clear all unlocks for a player (on logout) - only clears from memory, not disk
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

    private static String normalizeSkillName(String name) {
        String normalized = normalizeRawName(name);
        normalized = SKILL_ALIASES.getOrDefault(normalized, normalized);

        for (String skill : AP_SKILL_KEYS) {
            if (normalized.startsWith(skill + "_")) {
                normalized = skill;
                break;
            }
        }

        normalized = SKILL_ALIASES.getOrDefault(normalized, normalized);
        return "vhskill:" + normalized;
    }

    private static String normalizeTalentName(String name) {
        return normalizeName("vhtalent:", name, null);
    }

    private static String normalizeExpertiseName(String name) {
        return normalizeName("vhexpertise:", name, null);
    }

    private static String normalizeModName(String name) {
        return normalizeName("vhmod:", name, MOD_ALIASES);
    }

    private static String normalizeName(String prefix, String name, Map<String, String> aliases) {
        String normalized = normalizeRawName(name);

        if (aliases != null) {
            normalized = aliases.getOrDefault(normalized, normalized);
        }

        return prefix + normalized;
    }

    private static String normalizeRawName(String name) {
        if (name == null || name.isBlank()) {
            return "unknown";
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        int prefixSeparator = normalized.indexOf(':');
        if (prefixSeparator >= 0) {
            normalized = normalized.substring(prefixSeparator + 1);
        }

        normalized = normalized
                .replace("'", "")
                .replace("-", "_")
                .replace(" ", "_")
                .replace("&", "and")
                .replaceAll("[^a-z0-9_]", "")
                .replaceAll("_+", "_");

        return normalized;
    }

    private static Map<String, String> createSkillAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("chain_lightning", "lightning_strike");
        aliases.put("chain_lightning_base", "lightning_strike");
        aliases.put("chain_lightning_orbs", "lightning_strike");
        aliases.put("ball_lightning", "lightning_strike");
        aliases.put("grenade", "chaos_cube");
        aliases.put("grenade_base", "chaos_cube");
        aliases.put("toxic_grenade", "chaos_cube");
        aliases.put("totem", "rejuvenation_totem");
        aliases.put("totem_base", "rejuvenation_totem");
        aliases.put("totem_player_damage", "rejuvenation_totem");
        aliases.put("totem_mana_regen", "rejuvenation_totem");
        aliases.put("totem_mob_damage", "rejuvenation_totem");
        aliases.put("totem_rejuv", "rejuvenation_totem");
        aliases.put("totem_wrath", "rejuvenation_totem");
        aliases.put("totem_spirit", "rejuvenation_totem");
        aliases.put("totem_hatred", "rejuvenation_totem");
        aliases.put("mana_barrier", "mana_shield");
        aliases.put("retribution", "shield_bash");
        aliases.put("mana_shield_retribution", "shield_bash");
        aliases.put("mana_shield_implode", "implode");
        aliases.put("life_tap", "implode");
        aliases.put("stonefall_snow", "stonefall");
        aliases.put("herolanding", "stonefall");
        aliases.put("stonefall_cold", "stonefall");
        aliases.put("coldfall", "stonefall");
        aliases.put("sa_thunder", "storm_arrow");
        aliases.put("sa_blizzard", "storm_arrow");
        return aliases;
    }

    private static final Set<String> AP_SKILL_KEYS = Set.of(
            "nova", "fireball", "javelin", "stonefall", "ice_bolt", "implode",
            "shield_bash", "arcane", "earthquake", "lightning_strike", "dash",
            "vein_miner", "ghost_walk", "rampage", "mega_jump", "shell", "taunt",
            "heal", "angel", "empower", "hunter", "smite", "storm_arrow",
            "battle_cry", "rejuvenation_totem", "mana_shield", "chaos_cube"
    );

    private static Map<String, String> createModAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("colossal_chest", "colossal_chests");
        aliases.put("stackupgrading", "stack_upgrading");
        aliases.put("autorefill", "auto_refill");
        aliases.put("autofeeding", "auto_feeding");
        aliases.put("torch_master", "torchmaster");
        aliases.put("filters", "vault_filters");
        aliases.put("potion", "potions");
        aliases.put("mixture", "mixtures");
        aliases.put("markers", "map_markers");
        aliases.put("pretty_pipes", "pipez");
        return aliases;
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

    // Called by sync packet
    public static void setClientUnlockedSkills(Set<String> skills) {
        clientUnlockedSkills = normalizeAll(skills, APSkillLockManager::canonicalSkillKey);
        LOGGER.info("Client cache updated: {} skills {}", clientUnlockedSkills.size(), clientUnlockedSkills);
    }

    public static void setClientUnlockedTalents(Set<String> talents) {
        clientUnlockedTalents = normalizeAll(talents, APSkillLockManager::canonicalTalentKey);
        LOGGER.info("Client cache updated: {} talents", talents.size());
    }

    public static void setClientUnlockedMods(Set<String> mods) {
        clientUnlockedMods = normalizeAll(mods, APSkillLockManager::canonicalModKey);
        LOGGER.info("Client cache updated: {} mods", mods.size());
    }

    public static void setClientUnlockedExpertises(Set<String> expertises) {
        clientUnlockedExpertises = normalizeAll(expertises, APSkillLockManager::canonicalExpertiseKey);
        LOGGER.info("Client cache updated: {} expertises", expertises.size());
    }

    private static Set<String> normalizeAll(Set<String> values, java.util.function.Function<String, String> normalizer) {
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            normalized.add(normalizer.apply(value));
        }
        return normalized;
    }

    // Client-side check methods
    public static boolean isSkillUnlockedClient(String skillName) {
        String key = canonicalSkillKey(skillName);
        boolean unlocked = clientUnlockedSkills.contains(key);
        LOGGER.debug("Client unlock check - Skill: '{}' -> Key: '{}' -> Unlocked: {} (Cache: {})",
                skillName, key, unlocked, clientUnlockedSkills);
        return unlocked;
    }

    public static boolean isTalentUnlockedClient(String talentName) {
        String key = canonicalTalentKey(talentName);
        boolean unlocked = clientUnlockedTalents.contains(key);
        LOGGER.debug("Client unlock check - Talent: '{}' -> Key: '{}' -> Unlocked: {}",
                talentName, key, unlocked);
        return unlocked;
    }

    public static boolean isModUnlockedClient(String modName) {
        String key = canonicalModKey(modName);
        boolean unlocked = clientUnlockedMods.contains(key);
        LOGGER.debug("Client unlock check - Mod: '{}' -> Key: '{}' -> Unlocked: {}",
                modName, key, unlocked);
        return unlocked;
    }

    public static boolean isExpertiseUnlockedClient(String expertiseName) {
        String key = canonicalExpertiseKey(expertiseName);
        boolean unlocked = clientUnlockedExpertises.contains(key);
        LOGGER.debug("Client unlock check - Expertise: '{}' -> Key: '{}' -> Unlocked: {}",
                expertiseName, key, unlocked);
        return unlocked;
    }

    public static String canonicalSkillKey(String name) {
        return normalizeSkillName(name);
    }

    public static String canonicalTalentKey(String name) {
        return normalizeTalentName(name);
    }

    public static String canonicalModKey(String name) {
        return normalizeModName(name);
    }

    public static String canonicalExpertiseKey(String name) {
        return normalizeExpertiseName(name);
    }

    // Send sync packet to client
    public static void syncToClient(ServerPlayer player) {
        UUID playerId = player.getUUID();

        Set<String> skills = unlockedSkills.getOrDefault(playerId, new HashSet<>());
        Set<String> talents = unlockedTalents.getOrDefault(playerId, new HashSet<>());
        Set<String> mods = unlockedMods.getOrDefault(playerId, new HashSet<>());
        Set<String> expertises = unlockedExpertises.getOrDefault(playerId, new HashSet<>());

        APUnlockSyncMessage message = new APUnlockSyncMessage(skills, talents, mods, expertises);

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
