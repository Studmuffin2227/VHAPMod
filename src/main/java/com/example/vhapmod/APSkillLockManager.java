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
    private static final Map<String, String> TALENT_ALIASES = createTalentAliases();
    private static final Map<String, String> MOD_ALIASES = createModAliases();
    private static boolean lockSpecializations = true;

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
        String normalizedName = normalizeSkillName(skillName);
        if (!lockSpecializations && SPECIALIZATION_KEYS.contains(normalizedName)) {
            return true;
        }
        UUID uuid = player.getUUID();
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
        unlockData.addGlobalSkill(normalizedName);
        applyGlobalUnlockToOnlinePlayers(player, normalizedName, unlockedSkills);

        LOGGER.info("=== UNLOCKING SKILL ===");
        LOGGER.info("Input: '{}' for player UUID={}", skillName, uuid);
        LOGGER.info("Normalized: '{}'", normalizedName);
        LOGGER.info("All unlocked skills for player: {}", playerSkills);
        LOGGER.info("========================");

        syncAllOnlinePlayers(player);
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
        unlockData.addGlobalTalent(normalizedName);
        applyGlobalUnlockToOnlinePlayers(player, normalizedName, unlockedTalents);

        LOGGER.info("=== UNLOCKING TALENT ===");
        LOGGER.info("Input: '{}' for player UUID={}", talentName, uuid);
        LOGGER.info("Normalized: '{}'", normalizedName);
        LOGGER.info("All unlocked talents for player: {}", playerTalents);
        LOGGER.info("========================");

        syncAllOnlinePlayers(player);
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
        unlockData.addGlobalExpertise(normalizedName);
        applyGlobalUnlockToOnlinePlayers(player, normalizedName, unlockedExpertises);

        LOGGER.info("=== UNLOCKING EXPERTISE ===");
        LOGGER.info("Input: '{}'", expertiseName);
        LOGGER.info("Normalized: '{}'", normalizedName);
        LOGGER.info("========================");

        syncAllOnlinePlayers(player);
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
        unlockData.addGlobalMod(normalizedName);
        applyGlobalUnlockToOnlinePlayers(player, normalizedName, unlockedMods);

        LOGGER.info("Unlocked mod for {}: {}", player.getName().getString(), modName);
        syncAllOnlinePlayers(player);
    }

    private static void applyGlobalUnlockToOnlinePlayers(ServerPlayer source, String unlock, Map<UUID, Set<String>> unlockMap) {
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            unlockMap.computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(unlock);
        }
    }

    private static void syncAllOnlinePlayers(ServerPlayer source) {
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            syncToClient(player);
        }
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
        if (normalized.matches(".*_\\d+$")) {
            normalized = normalized.replaceFirst("_\\d+$", "");
        }
        if (normalized.matches(".*_lvl\\d+$")) {
            normalized = normalized.replaceFirst("_lvl\\d+$", "");
        }
        normalized = SKILL_ALIASES.getOrDefault(normalized, normalized);
        return "vhskill:" + normalized;
    }

    private static String normalizeTalentName(String name) {
        return normalizeName("vhtalent:", name, TALENT_ALIASES);
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
        aliases.put("grenade", "chaos_cube");
        aliases.put("grenade_base", "chaos_cube");
        aliases.put("totem", "rejuvenation_totem");
        aliases.put("totem_base", "rejuvenation_totem");
        aliases.put("totem_rejuv", "rejuvenation_totem");
        aliases.put("sa_thunder", "storm_arrow");
        aliases.put("lightning_strike", "lightning_strike");
        aliases.put("chaos_cube", "chaos_cube");
        aliases.put("nova_base", "nova");
        aliases.put("fireball_base", "fireball");
        aliases.put("javelin_base", "javelin");
        aliases.put("stonefall_base", "stonefall");
        aliases.put("ice_bolt_base", "ice_bolt");
        aliases.put("implode_base", "implode");
        aliases.put("mana_shield_implode", "implode");
        aliases.put("shield_bash_base", "shield_bash");
        aliases.put("retribution", "shield_bash");
        aliases.put("arcane_base", "arcane");
        aliases.put("earthquake_base", "earthquake");
        aliases.put("dash_base", "dash");
        aliases.put("vein_miner_base", "vein_miner");
        aliases.put("ghost_walk_base", "ghost_walk");
        aliases.put("rampage_base", "rampage");
        aliases.put("mega_jump_base", "mega_jump");
        aliases.put("shell_base", "shell");
        aliases.put("taunt_base", "taunt");
        aliases.put("heal_base", "heal");
        aliases.put("angel_base", "angel");
        aliases.put("empower_base", "empower");
        aliases.put("hunter_base", "hunter");
        aliases.put("smite_base", "smite");
        aliases.put("storm_arrow_base", "storm_arrow");
        aliases.put("battle_cry_base", "battle_cry");
        aliases.put("mana_shield_base", "mana_shield");

        aliases.put("heal_group", "heal_group");
        aliases.put("heal_aid", "heal_group");
        aliases.put("group_heal", "heal_group");
        aliases.put("heal_cleanse", "heal_cleanse");
        aliases.put("cleanse", "heal_cleanse");
        aliases.put("dash_damage", "dash_damage");
        aliases.put("dash_bullet", "dash_damage");
        aliases.put("bullet", "dash_damage");
        aliases.put("dash_warp", "dash_warp");
        aliases.put("nova_slow", "nova_slow");
        aliases.put("nova_frost", "nova_slow");
        aliases.put("frost_nova", "nova_slow");
        aliases.put("nova_dot", "nova_dot");
        aliases.put("poison_nova", "nova_dot");
        aliases.put("ghost_walk_spirit", "ghost_walk_spirit");
        aliases.put("spirit_walk", "ghost_walk_spirit");
        aliases.put("mega_jump_break_up", "mega_jump_break_up");
        aliases.put("mega_jump_drill", "mega_jump_break_up");
        aliases.put("mega_jump_break_down", "mega_jump_break_down");
        aliases.put("mega_drill", "mega_jump_break_down");
        aliases.put("mega_dig", "mega_jump_break_down");
        aliases.put("rampage_leech", "rampage_leech");
        aliases.put("vampiric", "rampage_leech");
        aliases.put("rampage_chain", "rampage_chain");
        aliases.put("chaining", "rampage_chain");
        aliases.put("smite_archon", "smite_archon");
        aliases.put("archon", "smite_archon");
        aliases.put("smite_thunderstorm", "smite_thunderstorm");
        aliases.put("empower_ice_armor", "empower_ice_armor");
        aliases.put("ice_armour", "empower_ice_armor");
        aliases.put("ice_armor", "empower_ice_armor");
        aliases.put("empower_slowness_aura", "empower_slowness_aura");
        aliases.put("entropic_bind", "empower_slowness_aura");
        aliases.put("vein_miner_fortune", "vein_miner_fortune");
        aliases.put("vein_miner_durability", "vein_miner_durability");
        aliases.put("finesse_miner", "vein_miner_durability");
        aliases.put("vein_miner_void", "vein_miner_void");
        aliases.put("void_miner", "vein_miner_void");
        aliases.put("javelin_sight", "javelin_sight");
        aliases.put("hunter_javelin", "javelin_sight");
        aliases.put("mana_barrier", "mana_barrier");
        aliases.put("mana_shield_retribution", "mana_shield_retribution");
        aliases.put("shield_bash_retribution", "mana_shield_retribution");
        aliases.put("implode_life_tap", "implode_life_tap");
        aliases.put("mana_implode", "implode");
        aliases.put("life_tap", "implode_life_tap");
        aliases.put("taunt_repel", "taunt_repel");
        aliases.put("fear", "taunt_repel");
        aliases.put("taunt_charm", "taunt_charm");
        aliases.put("charm", "taunt_charm");
        aliases.put("stonefall_snow", "stonefall_snow");
        aliases.put("heros_landing", "stonefall_snow");
        aliases.put("herolanding", "stonefall_snow");
        aliases.put("stonefall_cold", "stonefall_cold");
        aliases.put("coldsnap", "stonefall_cold");
        aliases.put("coldfall", "stonefall_cold");
        aliases.put("totem_player_damage", "totem_player_damage");
        aliases.put("wrath_totem", "totem_player_damage");
        aliases.put("totem_mana_regen", "totem_mana_regen");
        aliases.put("spirit_totem", "totem_mana_regen");
        aliases.put("totem_mob_damage", "totem_mob_damage");
        aliases.put("hatred_totem", "totem_mob_damage");
        aliases.put("javelin_piercing", "javelin_piercing");
        aliases.put("javelin_pierce", "javelin_piercing");
        aliases.put("piercing_javelin", "javelin_piercing");
        aliases.put("javelin_scatter", "javelin_scatter");
        aliases.put("scatter_javelin", "javelin_scatter");
        aliases.put("shell_porcupine", "shell_porcupine");
        aliases.put("porcupine", "shell_porcupine");
        aliases.put("shell_quill", "shell_quill");
        aliases.put("quill", "shell_quill");
        aliases.put("fireball_volley", "fireball_volley");
        aliases.put("fire_volley", "fireball_volley");
        aliases.put("fireball_fireshot", "fireball_fireshot");
        aliases.put("fireshot", "fireball_fireshot");
        aliases.put("storm_arrow_blizzard", "storm_arrow_blizzard");
        aliases.put("sa_blizzard", "storm_arrow_blizzard");
        aliases.put("blizzard_arrow", "storm_arrow_blizzard");
        aliases.put("battle_cry_spectral_strike", "battle_cry_spectral_strike");
        aliases.put("spectral_cry", "battle_cry_spectral_strike");
        aliases.put("battle_cry_lucky_strike", "battle_cry_lucky_strike");
        aliases.put("lucky_cry", "battle_cry_lucky_strike");
        aliases.put("ice_bolt_blast", "ice_bolt_blast");
        aliases.put("glacial_blast", "ice_bolt_blast");
        aliases.put("arcane_rail", "arcane_rail");
        aliases.put("rail", "arcane_rail");
        aliases.put("earthquake_landmine", "earthquake_landmine");
        aliases.put("landmine", "earthquake_landmine");
        aliases.put("chain_lightning_orbs", "chain_lightning_orbs");
        aliases.put("ball_lightning", "chain_lightning_orbs");
        aliases.put("toxic_grenade", "toxic_grenade");
        aliases.put("toxic_vial", "toxic_grenade");
        return aliases;
    }

    private static final Set<String> AP_SKILL_KEYS = Set.of(
            "nova", "fireball", "javelin", "stonefall", "ice_bolt", "implode",
            "shield_bash", "arcane", "earthquake", "lightning_strike", "dash",
            "vein_miner", "ghost_walk", "rampage", "mega_jump", "shell", "taunt",
            "heal", "angel", "empower", "hunter", "smite", "storm_arrow",
            "battle_cry", "rejuvenation_totem", "mana_shield", "chaos_cube"
    );

    private static final Set<String> SPECIALIZATION_KEYS = Set.of(
            "vhskill:heal_group", "vhskill:heal_cleanse", "vhskill:dash_damage",
            "vhskill:dash_warp", "vhskill:nova_slow", "vhskill:nova_dot",
            "vhskill:ghost_walk_spirit", "vhskill:mega_jump_break_up",
            "vhskill:mega_jump_break_down", "vhskill:rampage_leech",
            "vhskill:rampage_chain", "vhskill:smite_archon",
            "vhskill:smite_thunderstorm", "vhskill:empower_ice_armor",
            "vhskill:empower_slowness_aura", "vhskill:vein_miner_fortune",
            "vhskill:vein_miner_durability", "vhskill:vein_miner_void",
            "vhskill:javelin_sight", "vhskill:mana_barrier",
            "vhskill:mana_shield_retribution", "vhskill:implode_life_tap",
            "vhskill:taunt_repel", "vhskill:taunt_charm", "vhskill:stonefall_snow",
            "vhskill:stonefall_cold", "vhskill:totem_player_damage",
            "vhskill:totem_mana_regen", "vhskill:totem_mob_damage",
            "vhskill:javelin_piercing", "vhskill:javelin_scatter",
            "vhskill:shell_porcupine", "vhskill:shell_quill",
            "vhskill:fireball_volley", "vhskill:fireball_fireshot",
            "vhskill:storm_arrow_blizzard", "vhskill:battle_cry_spectral_strike",
            "vhskill:battle_cry_lucky_strike", "vhskill:ice_bolt_blast",
            "vhskill:arcane_rail", "vhskill:earthquake_landmine",
            "vhskill:chain_lightning_orbs", "vhskill:toxic_grenade"
    );

    private static Map<String, String> createTalentAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("stone_skin", "stoneskin");
        aliases.put("lightning_stun", "lightning_finesse");
        aliases.put("lightning_damage", "lightning_mastery");
        aliases.put("javelin_throw_power", "throw_power");
        aliases.put("javelin_damage", "damage");
        aliases.put("javelin_conduct", "conduct");
        aliases.put("javelin_frugal", "ethereal");
        return aliases;
    }

    public static void setLockSpecializations(boolean enabled) {
        lockSpecializations = enabled;
        LOGGER.info("AP specialization locking: {}", enabled ? "enabled" : "disabled");
    }

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
        if (!lockSpecializations && SPECIALIZATION_KEYS.contains(key)) {
            return true;
        }
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
