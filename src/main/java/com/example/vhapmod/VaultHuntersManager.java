package com.example.vhapmod;

import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages Vault Hunters progression tracking and integration with Archipelago.
 *
 * This class handles:
 * - Tracking which skills/talents/mods have been unlocked
 * - Sending location checks to the AP server
 * - Receiving and applying items from AP
 */
public class VaultHuntersManager {

    private static final Logger LOGGER = LogManager.getLogger();

    // AP client reference
    private APWebSocketClient apClient;

    // Track what's been unlocked
    private final Set<Long> checkedSkills = new HashSet<>();
    private final Set<Long> checkedTalents = new HashSet<>();
    private final Set<Long> checkedExpertises = new HashSet<>();
    private final Set<Long> checkedMods = new HashSet<>();
    private final Set<Long> checkedMilestones = new HashSet<>();

    // Current XP and Loot gamerule states
    private String currentXPRule = "NORMAL";
    private String currentLootRule = "NORMAL";

    // VH Data classes (loaded via reflection)
    private static Class<?> playerAbilitiesDataClass;
    private static Class<?> playerTalentsDataClass;
    private static Class<?> playerExpertisesDataClass;
    private static Class<?> playerResearchesDataClass;

    static {
        try {
            playerAbilitiesDataClass = Class.forName("iskallia.vault.world.data.PlayerAbilitiesData");
            playerTalentsDataClass = Class.forName("iskallia.vault.world.data.PlayerTalentsData");
            playerExpertisesDataClass = Class.forName("iskallia.vault.world.data.PlayerExpertisesData");
            playerResearchesDataClass = Class.forName("iskallia.vault.world.data.PlayerResearchesData");
            LOGGER.info("Loaded VH data classes for item giving");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to load VH classes: " + e.getMessage());
        }
    }

    /**
     * Set the AP client (called during mod initialization)
     */
    public void setAPClient(APWebSocketClient client) {
        this.apClient = client;
    }

    // ==================== SKILL TRACKING ====================

    /**
     * Called when a player unlocks a skill in VH
     */
    public void onSkillUnlocked(ServerPlayer player, String skillName) {
        Long locationId = VaultHuntersData.getSkillLocationId(skillName);

        if (locationId != null && locationId != 0L && !checkedSkills.contains(locationId)) {
            checkedSkills.add(locationId);
            sendLocationCheck(locationId, player, "Skill: " + skillName);
        }
    }

    /**
     * Called when a player unlocks a talent in VH
     */
    public void onTalentUnlocked(ServerPlayer player, String talentName) {
        Long locationId = VaultHuntersData.getTalentLocationId(talentName);

        LOGGER.info("Looking up location ID for talent: '{}' -> {}", talentName, locationId);

        if (locationId != null && locationId != 0L && !checkedTalents.contains(locationId)) {
            checkedTalents.add(locationId);
            sendLocationCheck(locationId, player, "Talent: " + talentName);
        }
    }

    /**
     * Called when a player unlocks an expertise in VH
     */
    public void onExpertiseUnlocked(ServerPlayer player, String expertiseName) {
        Long locationId = VaultHuntersData.getExpertiseLocationId(expertiseName);

        LOGGER.info("Looking up location ID for expertise: '{}' -> {}", expertiseName, locationId);

        if (locationId != null && locationId != 0L && !checkedExpertises.contains(locationId)) {
            checkedExpertises.add(locationId);
            sendLocationCheck(locationId, player, "Expertise: " + expertiseName);
        }
    }

    /**
     * Called when a player unlocks a mod in VH
     */
    public void onModUnlocked(ServerPlayer player, String modName) {
        Long locationId = VaultHuntersData.getModLocationId(modName);

        LOGGER.info("Looking up location ID for mod: '{}' -> {}", modName, locationId);

        if (locationId != null && locationId != 0L && !checkedMods.contains(locationId)) {
            checkedMods.add(locationId);
            sendLocationCheck(locationId, player, "Mod: " + modName);
        }
    }

    /**
     * Called when a player reaches a level milestone
     */
    public void onLevelMilestone(ServerPlayer player, int level) {
        String milestoneName = "vhmilestone:level_" + level;
        Long locationId = VaultHuntersData.getMilestoneLocationId(milestoneName);

        if (locationId != null && locationId != 0L && !checkedMilestones.contains(locationId)) {
            checkedMilestones.add(locationId);
            sendLocationCheck(locationId, player, "Level " + level);
        }
    }

    /**
     * Called when a player completes a vault
     */
    public void onVaultCompleted(ServerPlayer player, int totalCompleted) {
        // Check for vault completion milestones
        if (totalCompleted == 1) {
            checkMilestone(player, "vhmilestone:first_vault", "First Vault");
        } else if (totalCompleted == 10) {
            checkMilestone(player, "vhmilestone:complete_10_vaults", "10 Vaults");
        } else if (totalCompleted == 25) {
            checkMilestone(player, "vhmilestone:complete_25_vaults", "25 Vaults");
        } else if (totalCompleted == 50) {
            checkMilestone(player, "vhmilestone:complete_50_vaults", "50 Vaults");
        }
    }

    private void checkMilestone(ServerPlayer player, String milestoneName, String displayName) {
        Long locationId = VaultHuntersData.getMilestoneLocationId(milestoneName);

        if (locationId != null && locationId != 0L && !checkedMilestones.contains(locationId)) {
            checkedMilestones.add(locationId);
            sendLocationCheck(locationId, player, displayName);
        }
    }

    // ==================== ARCHIPELAGO COMMUNICATION ====================

    /**
     * Send a location check to the Archipelago server
     */
    private void sendLocationCheck(long locationId, ServerPlayer player, String displayName) {
        LOGGER.info("Player {} checked location: {} (ID: {})", player.getName().getString(), displayName, locationId);

        // Send to AP via APWebSocketClient
        if (apClient != null && apClient.isConnected()) {
            apClient.sendLocationCheck(locationId);
        } else {
            LOGGER.warn("AP client not connected - cannot send location check");
        }
    }

    /**
     * Called when receiving an item from Archipelago
     */
    public void onItemReceived(long itemId, ServerPlayer player) {
        LOGGER.info("Player {} received AP item ID: {}", player.getName().getString(), itemId);

        // Handle different item types
        if (itemId == VaultHuntersData.ITEM_SKILL_POINT) {
            giveSkillPoint(player);
        } else if (itemId == VaultHuntersData.ITEM_EXPERTISE_POINT) {
            giveExpertisePoint(player);
        } else if (itemId == VaultHuntersData.ITEM_KNOWLEDGE_STAR) {
            giveKnowledgeStar(player);
        } else if (VaultHuntersData.isXPGamerule(itemId)) {
            setXPGamerule(VaultHuntersData.getXPGamerule(itemId));
        } else if (VaultHuntersData.isLootGamerule(itemId)) {
            setLootGamerule(VaultHuntersData.getLootGamerule(itemId));
        }
    }

    // ==================== ITEM APPLICATION ====================

    /**
     * Give the player a skill point
     */
    private void giveSkillPoint(ServerPlayer player) {
        LOGGER.info("Giving skill point to {}", player.getName().getString());
        try {
            // Get PlayerAbilitiesData
            Method getData = playerAbilitiesDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            // Get the player's ability tree
            Method getAbilities = data.getClass().getMethod("getAbilities", player.getUUID().getClass());
            Object abilityTree = getAbilities.invoke(data, player.getUUID());

            // Add skill points
            Method addPoints = abilityTree.getClass().getMethod("addPoints", int.class);
            addPoints.invoke(abilityTree, 1);

            // Mark as dirty to save
            Method setDirty = data.getClass().getMethod("setDirty");
            setDirty.invoke(data);

            LOGGER.info("Successfully gave 1 skill point to {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to give skill point: ", e);
        }
    }

    /**
     * Give the player an expertise point
     */
    private void giveExpertisePoint(ServerPlayer player) {
        LOGGER.info("Giving expertise point to {}", player.getName().getString());
        try {
            // Get PlayerExpertisesData
            Method getData = playerExpertisesDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            // Get the player's expertise tree
            Method getExpertises = data.getClass().getMethod("getExpertises", player.getUUID().getClass());
            Object expertiseTree = getExpertises.invoke(data, player.getUUID());

            // Add expertise points
            Method addPoints = expertiseTree.getClass().getMethod("addPoints", int.class);
            addPoints.invoke(expertiseTree, 1);

            // Mark as dirty to save
            Method setDirty = data.getClass().getMethod("setDirty");
            setDirty.invoke(data);

            LOGGER.info("Successfully gave 1 expertise point to {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to give expertise point: ", e);
        }
    }

    /**
     * Give the player a knowledge star
     */
    private void giveKnowledgeStar(ServerPlayer player) {
        LOGGER.info("Giving knowledge star to {}", player.getName().getString());
        try {
            // Get PlayerResearchesData
            Method getData = playerResearchesDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            // Get the player's research tree
            Method getResearches = data.getClass().getMethod("getResearches", player.getUUID().getClass());
            Object researchTree = getResearches.invoke(data, player.getUUID());

            // Add knowledge (stars)
            Method addKnowledge = researchTree.getClass().getMethod("addKnowledge", int.class);
            addKnowledge.invoke(researchTree, 1); // 1 knowledge star

            // Mark as dirty to save
            Method setDirty = data.getClass().getMethod("setDirty");
            setDirty.invoke(data);

            LOGGER.info("Successfully gave 1 knowledge star to {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to give knowledge star: ", e);
        }
    }

    /**
     * Set the XP gamerule
     */
    private void setXPGamerule(String rule) {
        if (!currentXPRule.equals(rule)) {
            currentXPRule = rule;
            LOGGER.info("Setting VH XP gamerule to: {}", rule);
            // TODO: Execute command: /gamerule vaultExperience [rule]
        }
    }

    /**
     * Set the Loot gamerule
     */
    private void setLootGamerule(String rule) {
        if (!currentLootRule.equals(rule)) {
            currentLootRule = rule;
            LOGGER.info("Setting VH Loot gamerule to: {}", rule);
            // TODO: Execute command: /gamerule vaultLoot [rule]
        }
    }

    // ==================== UTILITY ====================

    /**
     * Check if a location has been checked
     */
    public boolean hasCheckedLocation(long locationId) {
        return checkedSkills.contains(locationId)
                || checkedTalents.contains(locationId)
                || checkedExpertises.contains(locationId)
                || checkedMods.contains(locationId)
                || checkedMilestones.contains(locationId);
    }

    /**
     * Get total number of locations checked
     */
    public int getTotalChecked() {
        return checkedSkills.size()
                + checkedTalents.size()
                + checkedExpertises.size()
                + checkedMods.size()
                + checkedMilestones.size();
    }

    public void onQuestCompleted(ServerPlayer player, String questLocationName) {
        try {
            Long locationId = VaultHuntersData.getLocationId(questLocationName);
            if (locationId != null) {
                sendLocationCheck(locationId.longValue(), player, questLocationName);
            } else {
                LOGGER.warn("No location ID found for quest: {}", questLocationName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send quest check: {}", e.getMessage());
        }
    }
    public void onTrinketFound(ServerPlayer player, String trinketLocationName) {
        try {
            Long locationId = VaultHuntersData.getLocationId(trinketLocationName);
            if (locationId != null) {
                sendLocationCheck(locationId.longValue(), player, trinketLocationName);
            } else {
                LOGGER.warn("No location ID found for trinket: {}", trinketLocationName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send trinket check: {}", e.getMessage());
        }
    }

    // In VaultHuntersManager.java - NO private maps needed!

    public void unlockSkill(ServerPlayer player, String skillName) {
        // APSkillLockManager already has the storage
        APSkillLockManager.unlockSkill(player, skillName);
        APSkillLockManager.syncToClient(player);

        player.sendMessage(
                new TextComponent("[AP] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked skill: " + skillName)),
                player.getUUID()
        );
        LOGGER.info("Unlocked skill {} for player {}", skillName, player.getName().getString());
    }

    public void unlockTalent(ServerPlayer player, String talentName) {
        LOGGER.info("=== unlockTalent() CALLED ===");
        LOGGER.info("Player: {}", player.getName().getString());
        LOGGER.info("Talent: {}", talentName);
        APSkillLockManager.unlockTalent(player, talentName);
        APSkillLockManager.syncToClient(player);

        player.sendMessage(
                new TextComponent("[AP] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked talent: " + talentName)),
                player.getUUID()
        );
        LOGGER.info("Unlocked talent {} for player {}", talentName, player.getName().getString());
    }

    public void unlockMod(ServerPlayer player, String modName) {
        APSkillLockManager.unlockMod(player, modName);
        APSkillLockManager.syncToClient(player);

        player.sendMessage(
                new TextComponent("[AP] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked mod: " + modName)),
                player.getUUID()
        );
        LOGGER.info("Unlocked mod {} for player {}", modName, player.getName().getString());
    }

    public void unlockExpertise(ServerPlayer player, String expertiseName) {
        APSkillLockManager.unlockExpertise(player, expertiseName);
        APSkillLockManager.syncToClient(player);

        player.sendMessage(
                new TextComponent("[AP] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(new TextComponent("Unlocked expertise: " + expertiseName)),
                player.getUUID()
        );
        LOGGER.info("Unlocked expertise {} for player {}", expertiseName, player.getName().getString());
    }
}