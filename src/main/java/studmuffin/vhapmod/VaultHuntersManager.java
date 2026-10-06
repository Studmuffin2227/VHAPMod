package studmuffin.vhapmod;

import iskallia.vault.world.data.DiscoveredArtifactsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studmuffin.vhapmod.item.ModItems;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private final Set<Long> checkedLocations = new HashSet<>();

    // Current XP and Loot gamerule states
    private String currentXPRule = "NORMAL";
    private String currentLootRule = "NORMAL";
    private int levelCapProgress = 0;
    private int xpScalingProgress = 0;
    private int lootScalingProgress = 0;
    private int vaultTimeProgress = 0;
    private int crystalCostReductionProgress = 0;
    private int maxXpScaling = 3;
    private int maxLootScaling = 3;
    private int maxVaultTimeProgress = 0;
    private int maxCrystalCostReductionProgress = 0;
    private int bountyProgress = 0;
    private int bountyChecksPerProgressive = 5;
    private boolean freeCrystalCosts = true;
    private boolean startWithVaultKit = false;
    private boolean slotSettingsLoaded = false;
    private static final int[] LEVEL_CAP_STEPS = {10, 20, 40, 50, 100};
    private static final int[] EARLY_GOAL_LEVEL_CAP_STEPS = {5, 10};
    private int[] configuredLevelCapSteps;
    private static final String[] XP_RULE_STEPS = {"POOR", "HALF", "NORMAL", "DOUBLE", "TRIPLE", "TRIPLE", "TRIPLE"};
    private static final float[] XP_MULTIPLIER_STEPS = {0.1f, 0.5f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
    private static final String[] LOOT_RULE_STEPS = {"LEGACY", "NORMAL", "PLENTY", "EXTREME", "EXTREME", "EXTREME"};
    private static final float[] LOOT_MULTIPLIER_STEPS = {0.5f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
    private static final String[] CRYSTAL_COST_MODES = {"extreme", "grindy", "double", "normal", "half", "infinity"};

    // VH Data classes (loaded via reflection)
    private static Class<?> playerAbilitiesDataClass;
    private static Class<?> playerTalentsDataClass;
    private static Class<?> playerExpertisesDataClass;
    private static Class<?> playerResearchesDataClass;

    private int totalQuestChecks = 76;
    private int totalBountyChecks = 0;
    private static final long QUEST_CHECK_BASE_ID = 43500L;
    private static final long BOUNTY_CHECK_BASE_ID = 43650L;
    private static final long BOUNTY_CHECK_EXTRA_BASE_ID = 44500L;
    private static final long CHEST_CHECK_BASE_ID = 44000L;
    private float woodenChestWeight = 0.05f;     // Default 5%
    private float normalChestWeight = 0.10f;     // Default 10%
    private int goalLevel = 20;
    private String goalType = "level";
    private int artifactRequirement = 25;
    private int totalChestChecks = getChestChecksForGoalLevel(goalLevel);
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

    private void checkMilestone(ServerPlayer player, String milestoneName, String displayName) {
        Long locationId = VaultHuntersData.getMilestoneLocationId(milestoneName);

        if (locationId != null && locationId != 0L && !checkedMilestones.contains(locationId)) {
            checkedMilestones.add(locationId);
            sendLocationCheck(locationId, player, displayName);
        }
    }


    // ==================== Chest Check Implementation ===================
    // Add setters
    public void setTotalChestChecks(int count) {
        this.totalChestChecks = Math.max(0, count);
        LOGGER.info("Configured total chest checks: {}", this.totalChestChecks);
    }

    public void setGoalLevel(int level) {
        this.goalLevel = level;
        this.totalChestChecks = getChestChecksForGoalLevel(level);
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType == null || goalType.isBlank() ? "level" : goalType;
        LOGGER.info("Configured goal type: {}", this.goalType);
    }

    public void setArtifactRequirement(int artifactRequirement) {
        this.artifactRequirement = Math.max(1, Math.min(25, artifactRequirement));
        LOGGER.info("Configured artifact requirement: {}", this.artifactRequirement);
    }

    public int getArtifactRequirement() {
        return artifactRequirement;
    }

    public boolean suppressesVaultArtifactRewards() {
        return "artifact_puzzle".equals(goalType) || "herald".equals(goalType);
    }

    public void setLevelCapSteps(int[] steps) {
        if (steps == null || steps.length == 0) {
            configuredLevelCapSteps = null;
            return;
        }
        configuredLevelCapSteps = Arrays.stream(steps).filter(step -> step > 0).distinct().sorted().toArray();
        LOGGER.info("Configured progressive level caps: {}", Arrays.toString(configuredLevelCapSteps));
    }

    public void setMaxXpScaling(int multiplier) {
        this.maxXpScaling = Math.max(1, Math.min(5, multiplier));
        LOGGER.info("Configured max XP scaling: {}x", this.maxXpScaling);
    }

    public void setMaxLootScaling(int multiplier) {
        this.maxLootScaling = Math.max(1, Math.min(5, multiplier));
        LOGGER.info("Configured max loot scaling: {}x", this.maxLootScaling);
    }

    public void setProgressiveVaultTime(int count) {
        this.maxVaultTimeProgress = Math.max(0, count);
        LOGGER.info("Configured progressive vault time steps: {}", this.maxVaultTimeProgress);
    }

    public void setBountyChecksPerProgressive(int count) {
        this.bountyChecksPerProgressive = Math.max(1, Math.min(10, count));
        LOGGER.info("Configured bounty checks per Progressive Bounty: {}", this.bountyChecksPerProgressive);
    }

    public void setProgressiveCrystalCostReduction(int count) {
        this.freeCrystalCosts = count < 0;
        this.maxCrystalCostReductionProgress = this.freeCrystalCosts
                ? 0
                : Math.max(0, Math.min(CRYSTAL_COST_MODES.length - 1, count));
        LOGGER.info("Configured progressive crystal cost reduction steps: {}{}",
                this.maxCrystalCostReductionProgress, this.freeCrystalCosts ? " (free crystals)" : "");
    }

    public void setStartWithVaultKit(boolean startWithVaultKit) {
        this.startWithVaultKit = startWithVaultKit;
        LOGGER.info("Configured starting vault kit: {}", startWithVaultKit);
    }

    public void markSlotSettingsUnloaded() {
        this.slotSettingsLoaded = false;
    }

    public void markSlotSettingsLoaded() {
        this.slotSettingsLoaded = true;
    }

    public void initializeWorldProgression(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return;
        }
        if (!server.isSameThread()) {
            server.execute(() -> initializeWorldProgression(server));
            return;
        }

        ReceivedItemsData receivedItems = ReceivedItemsData.get(server);
        APProgressData progressData = APProgressData.get(server);
        int[] levelCapSteps = getLevelCapSteps();
        progressData.cacheSlotSettings(
                goalLevel,
                levelCapSteps,
                maxXpScaling,
                maxLootScaling,
                maxVaultTimeProgress,
                maxCrystalCostReductionProgress,
                freeCrystalCosts,
                bountyChecksPerProgressive);
        int maxLevelIndex = getMaxLevelCapIndex(levelCapSteps);

        levelCapProgress = Math.min(
                Math.max(
                        progressData.getLevelCapProgress(),
                        receivedItems.getProcessedItemCount(VaultHuntersData.ITEM_PROGRESSIVE_LEVEL_CAP)),
                maxLevelIndex);
        xpScalingProgress = Math.min(
                Math.max(
                        progressData.getXpScalingProgress(),
                        receivedItems.getProcessedItemCount(VaultHuntersData.ITEM_PROGRESSIVE_XP_SCALING)),
                getXpStepIndex(maxXpScaling));
        lootScalingProgress = Math.min(
                Math.max(
                        progressData.getLootScalingProgress(),
                        receivedItems.getProcessedItemCount(VaultHuntersData.ITEM_PROGRESSIVE_LOOT_SCALING)),
                getLootStepIndex(maxLootScaling));
        vaultTimeProgress = Math.min(
                Math.max(
                        progressData.getVaultTimeProgress(),
                        receivedItems.getProcessedItemCount(VaultHuntersData.ITEM_PROGRESSIVE_VAULT_TIME)),
                maxVaultTimeProgress);
        crystalCostReductionProgress = Math.min(
                Math.max(
                        progressData.getCrystalCostReductionProgress(),
                        receivedItems.getProcessedItemCount(VaultHuntersData.ITEM_PROGRESSIVE_CRYSTAL_COST_REDUCTION)),
                maxCrystalCostReductionProgress);
        bountyProgress = receivedItems.getProcessedItemCount(VaultHuntersData.ITEM_PROGRESSIVE_BOUNTY);
        int gearRarityProgress = Math.max(
                progressData.getGearRarityProgress(),
                receivedItems.getProcessedItemCount(VaultHuntersData.ITEM_PROGRESSIVE_GEAR_RARITY));
        APGearRarityManager.setProgress(gearRarityProgress);
        progressData.setLevelCapProgress(levelCapProgress);
        progressData.setXpScalingProgress(xpScalingProgress);
        progressData.setLootScalingProgress(lootScalingProgress);
        progressData.setVaultTimeProgress(vaultTimeProgress);
        progressData.setCrystalCostReductionProgress(crystalCostReductionProgress);
        progressData.setGearRarityProgress(APGearRarityManager.getProgress());

        executeServerCommand(server, "gamerule vaultLevelLock " + levelCapSteps[levelCapProgress]);
        executeServerCommand(server, "gamerule vaultExperience " + XP_RULE_STEPS[xpScalingProgress]);
        executeServerCommand(server, "gamerule vaultLoot " + LOOT_RULE_STEPS[lootScalingProgress]);
        applyVaultTimeRule(server);
        applyCrystalCostReductionRule(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            discoverSkippedArtifacts(player);
        }

        APScalingManager.setXpMultiplier(XP_MULTIPLIER_STEPS[xpScalingProgress]);
        APScalingManager.setLootMultiplier(LOOT_MULTIPLIER_STEPS[lootScalingProgress]);
        currentXPRule = XP_RULE_STEPS[xpScalingProgress];
        currentLootRule = LOOT_RULE_STEPS[lootScalingProgress];
        LOGGER.info("Initialized AP progression: vaultLevelLock {}, vaultExperience {}, vaultLoot {}, gear rarity {}",
                levelCapSteps[levelCapProgress], currentXPRule, currentLootRule,
                APGearRarityManager.getForcedRarityName());
        LOGGER.info("Initialized AP bounty access: {}/{} checks available",
                getAvailableBountyChecks(), totalBountyChecks);
    }

    public void restoreWorldProgression(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return;
        }
        if (!server.isSameThread()) {
            server.execute(() -> restoreWorldProgression(server));
            return;
        }
        if (!slotSettingsLoaded) {
            APProgressData progressData = APProgressData.get(server);
            if (!loadCachedSlotSettings(progressData)) {
                LOGGER.info("Skipping AP progression restore until slot settings are loaded");
                return;
            }
            LOGGER.info("Restoring AP progression using cached slot settings");
        }

        APProgressData progressData = APProgressData.get(server);
        int[] levelCapSteps = getLevelCapSteps();
        int maxLevelIndex = getMaxLevelCapIndex(levelCapSteps);

        levelCapProgress = Math.min(progressData.getLevelCapProgress(), maxLevelIndex);
        xpScalingProgress = Math.min(progressData.getXpScalingProgress(), getXpStepIndex(maxXpScaling));
        lootScalingProgress = Math.min(progressData.getLootScalingProgress(), getLootStepIndex(maxLootScaling));
        vaultTimeProgress = Math.min(progressData.getVaultTimeProgress(), maxVaultTimeProgress);
        crystalCostReductionProgress = Math.min(progressData.getCrystalCostReductionProgress(), maxCrystalCostReductionProgress);
        bountyProgress = ReceivedItemsData.get(server).getProcessedItemCount(VaultHuntersData.ITEM_PROGRESSIVE_BOUNTY);
        APGearRarityManager.setProgress(progressData.getGearRarityProgress());

        executeServerCommand(server, "gamerule vaultLevelLock " + levelCapSteps[levelCapProgress]);
        executeServerCommand(server, "gamerule vaultExperience " + XP_RULE_STEPS[xpScalingProgress]);
        executeServerCommand(server, "gamerule vaultLoot " + LOOT_RULE_STEPS[lootScalingProgress]);
        applyVaultTimeRule(server);
        applyCrystalCostReductionRule(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            discoverSkippedArtifacts(player);
        }

        APScalingManager.setXpMultiplier(XP_MULTIPLIER_STEPS[xpScalingProgress]);
        APScalingManager.setLootMultiplier(LOOT_MULTIPLIER_STEPS[lootScalingProgress]);
        currentXPRule = XP_RULE_STEPS[xpScalingProgress];
        currentLootRule = LOOT_RULE_STEPS[lootScalingProgress];

        if (levelCapProgress != progressData.getLevelCapProgress()) {
            progressData.setLevelCapProgress(levelCapProgress);
        }
        if (xpScalingProgress != progressData.getXpScalingProgress()) {
            progressData.setXpScalingProgress(xpScalingProgress);
        }
        if (lootScalingProgress != progressData.getLootScalingProgress()) {
            progressData.setLootScalingProgress(lootScalingProgress);
        }
        if (vaultTimeProgress != progressData.getVaultTimeProgress()) {
            progressData.setVaultTimeProgress(vaultTimeProgress);
        }
        if (crystalCostReductionProgress != progressData.getCrystalCostReductionProgress()) {
            progressData.setCrystalCostReductionProgress(crystalCostReductionProgress);
        }
        if (APGearRarityManager.getProgress() != progressData.getGearRarityProgress()) {
            progressData.setGearRarityProgress(APGearRarityManager.getProgress());
        }

        LOGGER.info("Restored AP progression: vaultLevelLock {}, vaultExperience {}, vaultLoot {}, gear rarity {}",
                levelCapSteps[levelCapProgress], currentXPRule, currentLootRule,
                APGearRarityManager.getForcedRarityName());
    }

    private boolean loadCachedSlotSettings(APProgressData progressData) {
        if (progressData == null || !progressData.hasCachedSlotSettings()) {
            return false;
        }
        this.goalLevel = progressData.getCachedGoalLevel();
        int[] cachedSteps = progressData.getCachedLevelCapSteps();
        this.configuredLevelCapSteps = cachedSteps.length == 0 ? null : Arrays.stream(cachedSteps)
                .filter(step -> step > 0)
                .distinct()
                .sorted()
                .toArray();
        this.maxXpScaling = Math.max(1, Math.min(5, progressData.getCachedMaxXpScaling()));
        this.maxLootScaling = Math.max(1, Math.min(5, progressData.getCachedMaxLootScaling()));
        this.maxVaultTimeProgress = Math.max(0, progressData.getCachedMaxVaultTimeProgress());
        this.maxCrystalCostReductionProgress = Math.max(0, progressData.getCachedMaxCrystalCostReductionProgress());
        this.freeCrystalCosts = progressData.getCachedFreeCrystalCosts();
        this.bountyChecksPerProgressive = Math.max(1, Math.min(10, progressData.getCachedBountyChecksPerProgressive()));
        this.slotSettingsLoaded = true;
        return true;
    }

    public void discoverSkippedArtifacts(ServerPlayer player) {
        if (player == null || player.getServer() == null || artifactRequirement >= 25) {
            return;
        }

        DiscoveredArtifactsData data = DiscoveredArtifactsData.get(player.getServer());
        int discovered = 0;
        for (int artifactOrder : APArtifactLayout.skippedOrders(artifactRequirement)) {
            if (artifactOrder >= 1 && artifactOrder <= 25 && data.discoverArtifact(player.getUUID(), artifactOrder)) {
                discovered++;
            }
        }
        if (discovered > 0) {
            data.syncTo(player);
            LOGGER.info("Discovered {} skipped artifacts for {} with artifact requirement {}", discovered, player.getName().getString(), artifactRequirement);
        }
    }

    public void grantStartingKitIfEnabled(ServerPlayer player) {
        if (!startWithVaultKit) {
            return;
        }

        APProgressData progressData = APProgressData.get(player.getServer());
        if (!progressData.markStartingKitClaimed(player.getUUID())) {
            return;
        }

        equipStartingKitArmor(player, "minecraft:iron_helmet");
        equipStartingKitArmor(player, "minecraft:iron_chestplate");
        equipStartingKitArmor(player, "minecraft:iron_leggings");
        equipStartingKitArmor(player, "minecraft:iron_boots");
        giveStartingKitItem(player, "minecraft:iron_sword", 1);
        giveStartingKitItem(player, "minecraft:iron_pickaxe", 1);
        giveStartingKitItem(player, "minecraft:iron_axe", 1);
        giveStartingKitItem(player, "minecraft:iron_shovel", 1);
        giveStartingKitItem(player, "the_vault:raw_chromatic_iron", 16);
        giveStartingKitItem(player, "the_vault:vault_stone", 64);
        giveStartingKitItem(player, "the_vault:vault_altar", 1);
        giveStartingKitItem(player, "the_vault:coin_pouch", 1);
        giveStartingKitItem(player, "the_vault:shard_pouch", 1);
        giveStartingKitStack(player, new ItemStack(ModItems.ARCHIPELA_POUCH.get()));
        giveStartingKitStack(player, new ItemStack(ModItems.CHECK_TABLE.get()));
        player.sendMessage(new TextComponent("[AP] Starting vault kit granted").withStyle(ChatFormatting.GREEN), player.getUUID());
    }

    private void giveStartingKitItem(ServerPlayer player, String itemId, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item == null) {
            LOGGER.warn("Could not find starting kit item {}", itemId);
            return;
        }
        giveStartingKitStack(player, new ItemStack(item, count));
    }

    private void equipStartingKitArmor(ServerPlayer player, String itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (!(item instanceof ArmorItem armorItem)) {
            LOGGER.warn("Could not find starting kit armor {}", itemId);
            return;
        }

        EquipmentSlot slot = armorItem.getSlot();
        ItemStack stack = new ItemStack(item);
        if (player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, stack);
            player.getInventory().setChanged();
            return;
        }

        giveStartingKitStack(player, stack);
    }

    private void giveStartingKitStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack) || !stack.isEmpty()) {
            player.drop(stack.copy(), false);
        }
        player.getInventory().setChanged();
    }

    public void setTotalQuestChecks(int count) {
        this.totalQuestChecks = Math.max(0, count);
        LOGGER.info("Configured total quest checks: {}", this.totalQuestChecks);
    }

    public void setTotalBountyChecks(int count) {
        this.totalBountyChecks = Math.max(0, count);
        LOGGER.info("Configured total bounty checks: {}", this.totalBountyChecks);
    }

    public void setWoodenChestWeight(float weight) {
        this.woodenChestWeight = weight;
        LOGGER.info("Configured wooden chest weight: {}%", (int)(weight * 100));
    }

    public void setNormalChestWeight(float weight) {
        this.normalChestWeight = weight;
        LOGGER.info("Configured normal chest weight: {}%", (int)(weight * 100));
    }

    // Add getters
    public int getTotalChestChecks() {
        return totalChestChecks;
    }

    public static int getChestChecksForGoalLevel(int level) {
        return Math.max(0, level) * 5;
    }

    public int getTotalQuestChecks() {
        return totalQuestChecks;
    }

    public int getTotalBountyChecks() {
        return totalBountyChecks;
    }

    public int getAvailableBountyChecks() {
        if (totalBountyChecks <= 0) {
            return 0;
        }
        int unlocked = 10 + Math.max(0, bountyProgress) * bountyChecksPerProgressive;
        return Math.min(totalBountyChecks, unlocked);
    }

    public float getWoodenChestWeight() {
        return woodenChestWeight;
    }

    public float getNormalChestWeight() {
        return normalChestWeight;
    }

    public long getNextChestCheckId(ServerPlayer player) {
        APProgressData progressData = APProgressData.get(player.getServer());
        long collectedCount = progressData.getSharedChestCheckCount();

        if (collectedCount >= totalChestChecks) {
            return 0;
        }

        long nextCheckId = CHEST_CHECK_BASE_ID + collectedCount;
        progressData.setSharedChestCheckCount(collectedCount + 1);

        return nextCheckId;
    }

    /**
     * Get how many chest checks a player has collected
     */
    public int getPlayerChestCheckCount(ServerPlayer player) {
        return (int) APProgressData.get(player.getServer()).getChestCheckCount(player.getUUID());
    }

    /**
     * Reset a player's chest check counter (if needed)
     */
    public void resetPlayerChestChecks(ServerPlayer player) {
        APProgressData.get(player.getServer()).setChestCheckCount(player.getUUID(), 0L);
        LOGGER.info("Reset chest check counter for {}", player.getName().getString());
    }

    /**
     * Called when player logs out - clean up if needed
     */
    public void onPlayerLogout(ServerPlayer player) {
        // DON'T clear the counter - we want it to persist!
        // The counter represents actual progression

        // But you might want to save it to disk here
        // saveChestCheckProgress(player);
    }

    // Optional: Save/load chest check progress to NBT or file
    // So it persists across server restarts


    // ==================== ARCHIPELAGO COMMUNICATION ====================

    /**
     * Send a location check to the Archipelago server
     */
    private void sendLocationCheck(long locationId, ServerPlayer player, String displayName) {
        if (checkedLocations.contains(locationId)) {
            LOGGER.info("Skipping duplicate location check: {} (ID: {})", displayName, locationId);
            return;
        }
        checkedLocations.add(locationId);
        LOGGER.info("Player {} checked location: {} (ID: {})", player.getName().getString(), displayName, locationId);

        // Send to AP via APWebSocketClient
        if (apClient != null && apClient.isConnected()) {
            apClient.sendLocationCheck(locationId);
        } else {
            LOGGER.warn("AP client not connected - cannot send location check");
        }
    }

    public boolean isLocationChecked(long locationId) {
        return checkedLocations.contains(locationId);
    }

    /**
     * Mark a location as checked
     */
    public void markLocationChecked(long locationId) {
        if (checkedLocations.add(locationId)) {
            LOGGER.info("Marked location {} as checked", locationId);
        }
    }

    public int claimNextQuestCompletion(ServerPlayer player) {
        APProgressData progressData = APProgressData.get(player.getServer());
        int completed = findNextUncheckedSequentialCount(
                progressData.getCompletedQuestCount(),
                totalQuestChecks,
                QUEST_CHECK_BASE_ID);
        if (completed <= 0) {
            return 0;
        }
        progressData.setCompletedQuestCount(completed);
        return completed;
    }

    private int claimNextBountyCompletion(ServerPlayer player) {
        APProgressData progressData = APProgressData.get(player.getServer());
        int completed = findNextUncheckedSequentialCount(
                progressData.getBountyCompletionCount(),
                getAvailableBountyChecks(),
                VaultHuntersManager::getBountyLocationId);
        if (completed <= 0) {
            return 0;
        }
        progressData.setBountyCompletionCount(completed);
        return completed;
    }

    private int findNextUncheckedSequentialCount(int currentCount, int totalCount, long baseLocationId) {
        return findNextUncheckedSequentialCount(
                currentCount,
                totalCount,
                nextCount -> baseLocationId + nextCount - 1L);
    }

    private int findNextUncheckedSequentialCount(int currentCount, int totalCount, java.util.function.IntToLongFunction locationForCount) {
        for (int nextCount = Math.max(0, currentCount) + 1; nextCount <= totalCount; nextCount++) {
            long locationId = locationForCount.applyAsLong(nextCount);
            if (!isLocationChecked(locationId)) {
                return nextCount;
            }
        }
        return 0;
    }

    private static long getBountyLocationId(int bountyNumber) {
        int index = bountyNumber - 1;
        return index < 50 ? BOUNTY_CHECK_BASE_ID + index : BOUNTY_CHECK_EXTRA_BASE_ID + index - 50L;
    }

    /**
     * Handle when a chest check is collected
     * Called when server receives confirmation from AP
     */
    public void onChestCheckCollected(ServerPlayer player, long locationId) {
        LOGGER.info("Player {} collected chest check at location {}",
                player.getName().getString(), locationId);

        // Mark as checked so it won't spawn again
        markLocationChecked(locationId);
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

    public boolean applyProgressionItem(long itemId, ServerPlayer player) {
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_LEVEL_CAP) {
            applyNextLevelCap(player);
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_XP_SCALING) {
            applyNextXpScaling(player);
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_LOOT_SCALING) {
            applyNextLootScaling(player);
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_GEAR_RARITY) {
            applyNextGearRarity(player);
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_VAULT_TIME) {
            applyNextVaultTime(player);
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_CRYSTAL_COST_REDUCTION) {
            applyNextCrystalCostReduction(player);
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_BOUNTY) {
            applyNextBountyProgress(player);
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_SKILL_POINT) {
            giveSkillPoint(player);
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_EXPERTISE_POINT) {
            giveExpertisePoint(player);
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_KNOWLEDGE_STAR) {
            giveKnowledgeStar(player);
            return true;
        }
        return false;
    }

    public boolean applyMultiplayerPointItem(long itemId, net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return false;
        }
        if (itemId == VaultHuntersData.ITEM_SKILL_POINT) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                giveSkillPoint(player);
            }
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_EXPERTISE_POINT) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                giveExpertisePoint(player);
            }
            return true;
        }
        if (itemId == VaultHuntersData.ITEM_KNOWLEDGE_STAR) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                giveKnowledgeStar(player);
            }
            return true;
        }
        return false;
    }

    private void applyNextLevelCap(ServerPlayer player) {
        int[] levelCapSteps = getLevelCapSteps();
        int maxIndex = getMaxLevelCapIndex(levelCapSteps);
        levelCapProgress = Math.min(levelCapProgress + 1, maxIndex);
        APProgressData.get(player.getServer()).setLevelCapProgress(levelCapProgress);
        int cap = levelCapSteps[levelCapProgress];
        executeServerCommand(player, "gamerule vaultLevelLock " + cap);
        LOGGER.info("Set AP vault level cap to {}", cap);
    }

    private int[] getLevelCapSteps() {
        if (configuredLevelCapSteps != null && configuredLevelCapSteps.length > 0) {
            return configuredLevelCapSteps;
        }
        return goalLevel == 10 ? EARLY_GOAL_LEVEL_CAP_STEPS : LEVEL_CAP_STEPS;
    }

    private int getMaxLevelCapIndex(int[] levelCapSteps) {
        if (configuredLevelCapSteps != null && configuredLevelCapSteps.length > 0) {
            return levelCapSteps.length - 1;
        }
        int maxIndex = 0;
        for (int i = 0; i < levelCapSteps.length; i++) {
            if (levelCapSteps[i] <= goalLevel) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private void applyNextXpScaling(ServerPlayer player) {
        int maxIndex = getXpStepIndex(maxXpScaling);
        xpScalingProgress = Math.min(xpScalingProgress + 1, maxIndex);
        APProgressData.get(player.getServer()).setXpScalingProgress(xpScalingProgress);
        float multiplier = XP_MULTIPLIER_STEPS[xpScalingProgress];
        String rule = XP_RULE_STEPS[xpScalingProgress];
        APScalingManager.setXpMultiplier(multiplier);
        executeServerCommand(player, "gamerule vaultExperience " + rule);
        currentXPRule = rule;
        LOGGER.info("Set AP XP scaling to {}x using vaultExperience {}", multiplier, rule);
    }

    private void applyNextLootScaling(ServerPlayer player) {
        int maxIndex = getLootStepIndex(maxLootScaling);
        lootScalingProgress = Math.min(lootScalingProgress + 1, maxIndex);
        APProgressData.get(player.getServer()).setLootScalingProgress(lootScalingProgress);
        float multiplier = LOOT_MULTIPLIER_STEPS[lootScalingProgress];
        String rule = LOOT_RULE_STEPS[lootScalingProgress];
        APScalingManager.setLootMultiplier(multiplier);
        executeServerCommand(player, "gamerule vaultLoot " + rule);
        currentLootRule = rule;
        LOGGER.info("Set AP loot scaling to {}x using vaultLoot {}", multiplier, rule);
    }

    private void applyNextGearRarity(ServerPlayer player) {
        var rarity = APGearRarityManager.advance();
        APProgressData.get(player.getServer()).setGearRarityProgress(APGearRarityManager.getProgress());
        LOGGER.info("Set AP vault gear rarity to {}", rarity.name());
    }

    private void applyNextVaultTime(ServerPlayer player) {
        vaultTimeProgress = Math.min(vaultTimeProgress + 1, maxVaultTimeProgress);
        APProgressData.get(player.getServer()).setVaultTimeProgress(vaultTimeProgress);
        applyVaultTimeRule(player.getServer());
        LOGGER.info("Set AP vault timer to {} minutes", getVaultTimeMinutes());
    }

    private void applyNextCrystalCostReduction(ServerPlayer player) {
        crystalCostReductionProgress = Math.min(crystalCostReductionProgress + 1, maxCrystalCostReductionProgress);
        APProgressData.get(player.getServer()).setCrystalCostReductionProgress(crystalCostReductionProgress);
        applyCrystalCostReductionRule(player.getServer());
        LOGGER.info("Set AP crystal cost mode to {}", getCrystalCostMode());
    }

    private void applyNextBountyProgress(ServerPlayer player) {
        bountyProgress++;
        APBountyCheckManager.refreshExistingRewards(player.getServer());
        LOGGER.info("Unlocked AP bounty checks: {}/{}", getAvailableBountyChecks(), totalBountyChecks);
    }

    private int getVaultTimeMinutes() {
        return (Math.max(0, vaultTimeProgress) + 1) * 5;
    }

    private String getCrystalCostMode() {
        if (freeCrystalCosts) {
            return "infinity";
        }
        int startIndex = Math.max(0, CRYSTAL_COST_MODES.length - 1 - maxCrystalCostReductionProgress);
        int index = Math.max(startIndex, Math.min(startIndex + crystalCostReductionProgress, CRYSTAL_COST_MODES.length - 1));
        return CRYSTAL_COST_MODES[index];
    }

    private void applyVaultTimeRule(net.minecraft.server.MinecraftServer server) {
        if (maxVaultTimeProgress <= 0) {
            return;
        }
        executeServerCommand(server, "gamerule vaultTimer " + getVaultTimeMinutes());
    }

    private void applyCrystalCostReductionRule(net.minecraft.server.MinecraftServer server) {
        if (!freeCrystalCosts && maxCrystalCostReductionProgress <= 0) {
            return;
        }
        String mode = getLocalCrystalCostMode();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            executeServerCommand(server, "the_vault local_crystal_mode " + player.getGameProfile().getName() + " " + mode);
        }
    }

    private String getLocalCrystalCostMode() {
        String mode = getCrystalCostMode();
        return "infinity".equals(mode) ? "free" : mode;
    }

    private int getXpStepIndex(int targetMultiplier) {
        return switch (Math.max(1, Math.min(5, targetMultiplier))) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            case 4 -> 5;
            default -> 6;
        };
    }

    private int getLootStepIndex(int targetMultiplier) {
        return switch (Math.max(1, Math.min(5, targetMultiplier))) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            default -> 5;
        };
    }

    private void executeServerCommand(ServerPlayer player, String command) {
        executeServerCommand(player.getServer(), command);
    }

    private void executeServerCommand(net.minecraft.server.MinecraftServer server, String command) {
        if (server == null) {
            return;
        }
        if (!server.isSameThread()) {
            server.execute(() -> executeServerCommand(server, command));
            return;
        }

        int result = server.getCommands().performCommand(
                server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(2),
                command
        );
        if (result == 0) {
            LOGGER.warn("Command returned no result: {}", command);
        }
    }

    // ==================== ITEM APPLICATION ====================

    /**
     * Give the player a skill point
     */
    private void giveSkillPoint(ServerPlayer player) {
        LOGGER.info("Giving skill point to {}", player.getName().getString());
        giveProgressionPoint(player, "skill_points", "skill point");
    }

    /**
     * Give the player an expertise point
     */
    private void giveExpertisePoint(ServerPlayer player) {
        LOGGER.info("Giving expertise point to {}", player.getName().getString());
        giveProgressionPoint(player, "expertise_points", "expertise point");
    }

    /**
     * Give the player a knowledge point
     */
    private void giveKnowledgeStar(ServerPlayer player) {
        LOGGER.info("Giving knowledge point to {}", player.getName().getString());
        giveProgressionPoint(player, "knowledge_points", "knowledge point");
    }

    private void giveProgressionPoint(ServerPlayer player, String pointType, String displayName) {
        String command = "the_vault progression " + pointType + " add 1 " + player.getGameProfile().getName();
        player.getServer().getCommands().performCommand(
                player.getServer().createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(2),
                command
        );
        LOGGER.info("Dispatched Vault progression command to give 1 {} to {}", displayName, player.getName().getString());
    }

    /**
     * Set the XP gamerule
     */
    private void setXPGamerule(String rule) {
        if (!currentXPRule.equals(rule)) {
            currentXPRule = rule;
            LOGGER.info("Setting VH XP gamerule to: {}", rule);
        }
    }

    /**
     * Set the Loot gamerule
     */
    private void setLootGamerule(String rule) {
        if (!currentLootRule.equals(rule)) {
            currentLootRule = rule;
            LOGGER.info("Setting VH Loot gamerule to: {}", rule);
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

    public void onArtifactPuzzleCompleted(ServerPlayer player) {
        APProgressData progressData = APProgressData.get(player.getServer());
        if (!progressData.markArtifactPuzzleCompleted()) {
            return;
        }
        sendLocationCheck(VaultHuntersData.GOAL_BASE_ID, player, "Complete Artifact Puzzle");
        if (apClient != null) {
            apClient.checkSpecialGoalReached(player, "artifact_puzzle");
        }
    }

    public void onHeraldDefeated(ServerPlayer player) {
        APProgressData progressData = APProgressData.get(player.getServer());
        if (!progressData.markHeraldDefeated()) {
            return;
        }
        sendLocationCheck(VaultHuntersData.GOAL_BASE_ID + 1, player, "Defeat The Herald");
        if (apClient != null) {
            apClient.checkSpecialGoalReached(player, "herald");
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

    public void onBountyCompleted(ServerPlayer player) {
        LOGGER.debug("Bounty completed by {}; AP bounty checks are granted through bounty rewards",
                player.getName().getString());
    }

    // In VaultHuntersManager.java - NO private maps needed!

    public void unlockSkill(ServerPlayer player, String skillName) {
        // APSkillLockManager already has the storage
        APSkillLockManager.unlockSkill(player, skillName);
        APSkillLockManager.syncToClient(player);
        LOGGER.info("Unlocked skill {} for player {}", skillName, player.getName().getString());
    }

    public void buySkillRankOne(ServerPlayer player, String skillName) {
        String targetKey = APSkillLockManager.canonicalSkillKey(skillName);
        try {
            Object data = VHDataReader.getWorldData(playerAbilitiesDataClass, player.getLevel());
            Method getAbilities = data.getClass().getMethod("getAbilities", player.getUUID().getClass());
            Object abilityTree = getAbilities.invoke(data, player.getUUID());
            if (abilityTree == null) {
                return;
            }

            Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
            Method iterate = abilityTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);
            final Object[] found = new Object[1];
            iterate.invoke(abilityTree, skillClass, (java.util.function.Consumer<Object>) skill -> {
                try {
                    Method getId = skill.getClass().getMethod("getId");
                    String id = APSkillLockManager.canonicalSkillKey((String) getId.invoke(skill));
                    if (targetKey.equals(id)) {
                        found[0] = skill;
                    }
                } catch (Exception ignored) {
                }
            });

            if (found[0] == null) {
                LOGGER.warn("Could not find VH skill {} to buy rank 1", skillName);
                return;
            }

            Object tieredSkill = resolveTieredSkill(found[0]);
            String rawId = getSkillId(found[0]);
            learnSkillInTree(player, abilityTree, rawId, false);
            boolean changed = ensureTierOneLearned(player, tieredSkill, false);

            if (changed) {
                markDirty(data);
                syncSkillTree(player, abilityTree, false);
                APSkillLockManager.syncToClient(player);
                LOGGER.info("Bought rank 1 of {} for {}", skillName, player.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to buy rank 1 for {}: {}", skillName, e.getMessage());
        }
    }

    public void autoActivateSkillUnlock(ServerPlayer player, String skillName) {
        String canonical = APSkillLockManager.canonicalSkillKey(skillName);
        if (VaultHuntersData.isSpecializationSkill(canonical)) {
            selectSpecialization(player, canonical);
        } else {
            buySkillRankOne(player, canonical);
        }
    }

    public void selectSpecialization(ServerPlayer player, String specializationName) {
        String targetKey = APSkillLockManager.canonicalSkillKey(specializationName);
        try {
            Object data = VHDataReader.getWorldData(playerAbilitiesDataClass, player.getLevel());
            Method getAbilities = data.getClass().getMethod("getAbilities", player.getUUID().getClass());
            Object abilityTree = getAbilities.invoke(data, player.getUUID());
            if (abilityTree == null) {
                return;
            }

            Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
            Method iterate = abilityTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);
            final Object[] found = new Object[1];
            iterate.invoke(abilityTree, skillClass, (java.util.function.Consumer<Object>) skill -> {
                try {
                    String id = APSkillLockManager.canonicalSkillKey(getSkillId(skill));
                    if (targetKey.equals(id)) {
                        found[0] = skill;
                    }
                } catch (Exception ignored) {
                }
            });

            if (found[0] == null) {
                LOGGER.warn("Could not find VH specialization {} to select", specializationName);
                return;
            }

            Object parent = found[0];
            while (parent != null && !parent.getClass().getName().equals("iskallia.vault.skill.base.SpecializedSkill")) {
                Method getParent = parent.getClass().getMethod("getParent");
                parent = getParent.invoke(parent);
            }
            if (parent == null) {
                LOGGER.warn("Could not find parent ability group for specialization {}", specializationName);
                return;
            }

            Object context = createSkillContext(player, false);
            String rawSpecializationId = getSkillId(found[0]);
            Method specialize = parent.getClass().getMethod("specialize", String.class, context.getClass());
            specialize.invoke(parent, rawSpecializationId, context);
            markDirty(data);
            syncSkillTree(player, abilityTree, false);
            APSkillLockManager.syncToClient(player);
            LOGGER.info("Selected specialization {} for {}", specializationName, player.getName().getString());
        } catch (Exception e) {
            LOGGER.warn("Failed to select specialization {}: {}", specializationName, e.getMessage());
        }
    }

    public void buyTalentRankOne(ServerPlayer player, String talentName) {
        buyTieredUnlockRankOne(player, playerTalentsDataClass, "getTalents", APSkillLockManager.canonicalTalentKey(talentName), talentName, false);
    }

    public void buyExpertiseRankOne(ServerPlayer player, String expertiseName) {
        buyTieredUnlockRankOne(player, playerExpertisesDataClass, "getExpertises", APSkillLockManager.canonicalExpertiseKey(expertiseName), expertiseName, true);
    }

    public void repairAutoLearnedUnlocks(ServerPlayer player) {
        try {
            APUnlockData unlockData = APUnlockData.get(player.getServer());
            Set<String> skills = unlockData.getUnlockedSkills(player.getUUID());
            for (String skill : skills) {
                if (VaultHuntersData.isSpecializationSkill(skill)) {
                    continue;
                }
                buySkillRankOne(player, skill);
            }
            for (String skill : skills) {
                if (!VaultHuntersData.isSpecializationSkill(skill)) {
                    continue;
                }
                String parent = VaultHuntersData.getSpecializationParent(skill);
                if (parent == null || skills.contains(parent)) {
                    selectSpecialization(player, skill);
                }
            }
            for (String talent : unlockData.getUnlockedTalents(player.getUUID())) {
                buyTalentRankOne(player, talent);
            }
            for (String expertise : unlockData.getUnlockedExpertises(player.getUUID())) {
                buyExpertiseRankOne(player, expertise);
            }
            for (String mod : unlockData.getUnlockedMods(player.getUUID())) {
                researchMod(player, mod);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to repair AP auto-learned unlocks for {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    public void researchMod(ServerPlayer player, String modName) {
        try {
            Object data = VHDataReader.getWorldData(playerResearchesDataClass, player.getLevel());
            Class<?> modConfigsClass = Class.forName("iskallia.vault.init.ModConfigs");
            Object researchConfig = modConfigsClass.getField("RESEARCHES").get(null);
            Object research = resolveResearch(researchConfig, modName);
            if (research == null) {
                LOGGER.warn("Could not find VH research for {}", modName);
                return;
            }

            Class<?> researchClass = Class.forName("iskallia.vault.research.type.Research");
            Method researchMethod = data.getClass().getMethod("research", ServerPlayer.class, researchClass, boolean.class);
            researchMethod.invoke(data, player, research, false);
            Method setDirty = data.getClass().getMethod("setDirty");
            setDirty.invoke(data);
            LOGGER.info("Researched mod {} for {}", modName, player.getName().getString());
        } catch (NoSuchMethodException e) {
            try {
                Object data = VHDataReader.getWorldData(playerResearchesDataClass, player.getLevel());
                Class<?> modConfigsClass = Class.forName("iskallia.vault.init.ModConfigs");
                Object researchConfig = modConfigsClass.getField("RESEARCHES").get(null);
                Object research = resolveResearch(researchConfig, modName);
                if (research == null) {
                    LOGGER.warn("Could not find VH research for {}", modName);
                    return;
                }
                Class<?> researchClass = Class.forName("iskallia.vault.research.type.Research");
                Method researchMethod = data.getClass().getMethod("research", ServerPlayer.class, researchClass);
                researchMethod.invoke(data, player, research);
                Method setDirty = data.getClass().getMethod("setDirty");
                setDirty.invoke(data);
                LOGGER.info("Researched mod {} for {}", modName, player.getName().getString());
            } catch (Exception inner) {
                LOGGER.warn("Failed to research mod {}: {}", modName, inner.getMessage());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to research mod {}: {}", modName, e.getMessage());
        }
    }

    private void buyTieredUnlockRankOne(ServerPlayer player, Class<?> dataClass, String getterName, String targetKey, String displayName, boolean expertise) {
        try {
            Object data = VHDataReader.getWorldData(dataClass, player.getLevel());
            Method getter = data.getClass().getMethod(getterName, player.getUUID().getClass());
            Object tree = getter.invoke(data, player.getUUID());
            if (tree == null) {
                return;
            }

            Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
            Method iterate = tree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);
            final Object[] found = new Object[1];
            iterate.invoke(tree, skillClass, (java.util.function.Consumer<Object>) skill -> {
                try {
                    Method getId = skill.getClass().getMethod("getId");
                    String id = (String) getId.invoke(skill);
                    String key = targetKey.startsWith("vhtalent:")
                            ? APSkillLockManager.canonicalTalentKey(id)
                            : APSkillLockManager.canonicalExpertiseKey(id);
                    if (!targetKey.equals(key)) {
                        Method getName = skill.getClass().getMethod("getName");
                        String displayKey = targetKey.startsWith("vhtalent:")
                                ? APSkillLockManager.canonicalTalentKey((String) getName.invoke(skill))
                                : APSkillLockManager.canonicalExpertiseKey((String) getName.invoke(skill));
                        key = displayKey;
                    }
                    if (targetKey.equals(key)) {
                        found[0] = skill;
                    }
                } catch (Exception ignored) {
                }
            });

            if (found[0] == null) {
                LOGGER.warn("Could not find VH unlock {} to buy rank 1", displayName);
                return;
            }

            Object tieredSkill = resolveTieredSkill(found[0]);
            String rawId = getSkillId(found[0]);
            learnSkillInTree(player, tree, rawId, expertise);
            boolean changed = ensureTierOneLearned(player, tieredSkill, expertise);

            if (changed) {
                markDirty(data);
                syncSkillTree(player, tree, expertise);
                APSkillLockManager.syncToClient(player);
                LOGGER.info("Bought rank 1 of {} for {}", displayName, player.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to buy rank 1 for {}: {}", displayName, e.getMessage());
        }
    }

    private boolean ensureTierOneLearned(ServerPlayer player, Object tieredSkill, boolean expertise) {
        if (tieredSkill == null) {
            return false;
        }

        try {
            Object context = createSkillContext(player, expertise);
            int tier = getSkillTier(tieredSkill);
            if (tier < 1) {
                Method learn = tieredSkill.getClass().getMethod("learn", context.getClass(), boolean.class);
                learn.invoke(tieredSkill, context, true);
                return true;
            }

            Object currentChild = getTierChild(tieredSkill, tier);
            if (currentChild != null && !isSkillPresent(currentChild)) {
                setSkillPresent(currentChild, context, true);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.debug("VH forced learn failed: {}", e.getMessage());
            if (getSkillTier(tieredSkill) < 1 && setSkillTier(tieredSkill, 1)) {
                try {
                    Object context = createSkillContext(player, expertise);
                    Object currentChild = getTierChild(tieredSkill, 1);
                    if (currentChild != null) {
                        setSkillPresent(currentChild, context, true);
                    }
                } catch (Exception ignored) {
                }
                return true;
            }
            return false;
        }
    }

    private Object getTierChild(Object tieredSkill, int tier) {
        try {
            Method getChild = tieredSkill.getClass().getMethod("getChild", int.class);
            return getChild.invoke(tieredSkill, tier);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isSkillPresent(Object skill) {
        try {
            Method isUnlocked = skill.getClass().getMethod("isUnlocked");
            return (boolean) isUnlocked.invoke(skill);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void setSkillPresent(Object skill, Object context, boolean present) throws ReflectiveOperationException {
        Method setPresent = skill.getClass().getMethod("setPresent", boolean.class, context.getClass());
        setPresent.invoke(skill, present, context);
    }

    private boolean learnSkillInTree(ServerPlayer player, Object tree, String skillId, boolean expertise) {
        if (skillId == null || skillId.isEmpty()) {
            return false;
        }
        try {
            Object context = createSkillContext(player, expertise);
            Method learn = tree.getClass().getMethod("learn", String.class, context.getClass());
            learn.invoke(tree, skillId, context);
            return true;
        } catch (Exception e) {
            LOGGER.debug("VH learn() failed for {}: {}", skillId, e.getMessage());
            return false;
        }
    }

    private void syncSkillTree(ServerPlayer player, Object tree, boolean expertise) {
        try {
            Object context = createSkillContext(player, expertise);
            Method sync = tree.getClass().getMethod("sync", context.getClass());
            sync.invoke(tree, context);
        } catch (Exception ignored) {
        }
    }

    private Object createSkillContext(ServerPlayer player, boolean expertise) throws ReflectiveOperationException {
        Class<?> contextClass = Class.forName("iskallia.vault.skill.base.SkillContext");
        String methodName = expertise ? "ofExpertise" : "of";
        Method method = contextClass.getMethod(methodName, ServerPlayer.class);
        return method.invoke(null, player);
    }

    private void markDirty(Object data) throws ReflectiveOperationException {
        Method setDirty = data.getClass().getMethod("setDirty");
        setDirty.invoke(data);
    }

    private String getSkillId(Object skill) throws ReflectiveOperationException {
        Method getId = skill.getClass().getMethod("getId");
        return (String) getId.invoke(skill);
    }

    private int getSkillTier(Object skill) {
        if (skill == null) {
            return 0;
        }
        try {
            Method getUnmodifiedTier = skill.getClass().getMethod("getUnmodifiedTier");
            return (int) getUnmodifiedTier.invoke(skill);
        } catch (Exception ignored) {
        }
        try {
            Field tierField = findField(skill.getClass(), "tier");
            if (tierField != null) {
                tierField.setAccessible(true);
                return tierField.getInt(skill);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private Object resolveTieredSkill(Object skill) {
        Object current = skill;
        while (current != null) {
            if (findField(current.getClass(), "tier") != null) {
                return current;
            }
            try {
                Method getParent = current.getClass().getMethod("getParent");
                current = getParent.invoke(current);
            } catch (Exception ignored) {
                return skill;
            }
        }
        return skill;
    }

    private boolean setSkillTier(Object skill, int tier) {
        if (skill == null) {
            return false;
        }
        boolean changed = false;
        try {
            Field tierField = findField(skill.getClass(), "tier");
            if (tierField != null) {
                tierField.setAccessible(true);
                if (tierField.getInt(skill) < tier) {
                    tierField.setInt(skill, tier);
                    changed = true;
                }
            }
            return changed;
        } catch (Exception e) {
            LOGGER.warn("Failed to set skill tier: {}", e.getMessage());
            return false;
        }
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public void unlockTalent(ServerPlayer player, String talentName) {
        LOGGER.info("=== unlockTalent() CALLED ===");
        LOGGER.info("Player: {}", player.getName().getString());
        LOGGER.info("Talent: {}", talentName);
        APSkillLockManager.unlockTalent(player, talentName);
        APSkillLockManager.syncToClient(player);
        LOGGER.info("Unlocked talent {} for player {}", talentName, player.getName().getString());
    }

    public void unlockMod(ServerPlayer player, String modName) {
        APSkillLockManager.unlockMod(player, modName);
        APSkillLockManager.syncToClient(player);
        LOGGER.info("Unlocked mod {} for player {}", modName, player.getName().getString());
    }

    public String unlockNextBackpackMod(ServerPlayer player) {
        String[] backpackChain = {"vhmod:double_pouches", "vhmod:belts", "vhmod:backpacks", "vhmod:big_backpacks"};
        Set<String> unlocked = APSkillLockManager.getUnlockedMods(player);
        for (String modName : backpackChain) {
            if (!unlocked.contains(APSkillLockManager.canonicalModKey(modName))) {
                unlockMod(player, modName);
                researchMod(player, modName);
                return modName;
            }
        }
        unlockMod(player, backpackChain[backpackChain.length - 1]);
        researchMod(player, backpackChain[backpackChain.length - 1]);
        return backpackChain[backpackChain.length - 1];
    }

    private String displayResearchName(String modName) {
        String raw = modName;
        int separator = raw.indexOf(':');
        if (separator >= 0) {
            raw = raw.substring(separator + 1);
        }

        StringBuilder result = new StringBuilder();
        for (String part : raw.split("_")) {
            if (part.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            result.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return result.toString();
    }

    private Object resolveResearch(Object researchConfig, String modName) throws Exception {
        Method getByName = researchConfig.getClass().getMethod("getByName", String.class);
        for (String name : researchNameCandidates(modName)) {
            Object research = getByName.invoke(researchConfig, name);
            if (research != null) {
                return research;
            }
        }

        Method getAll = researchConfig.getClass().getMethod("getAll");
        Method getModIds = Class.forName("iskallia.vault.research.type.ModResearch").getMethod("getModIds");
        Set<String> modIdCandidates = researchModIdCandidates(modName);
        Object researches = getAll.invoke(researchConfig);
        if (researches instanceof Iterable<?> iterable) {
            for (Object research : iterable) {
                try {
                    Object modIds = getModIds.invoke(research);
                    if (modIds instanceof Iterable<?> ids) {
                        for (Object id : ids) {
                            if (id instanceof String value && modIdCandidates.contains(normalizeResearchKey(value))) {
                                return research;
                            }
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // Custom researches are not ModResearch instances.
                }
            }
        }
        return null;
    }

    private List<String> researchNameCandidates(String modName) {
        String key = normalizeResearchKey(modName);
        return switch (key) {
            case "mekanism_qio" -> Arrays.asList("Mekanism QIO", "Extended Possibilities", displayResearchName(modName));
            case "torchmaster", "torch_master" -> Arrays.asList("Safety First", "Torchmaster", "Torch Master", displayResearchName(modName));
            case "snad" -> Arrays.asList("Uber Sand", "SNAD", displayResearchName(modName));
            case "augment_station", "augmentstation" -> Arrays.asList("Augment Station", displayResearchName(modName));
            default -> Arrays.asList(displayResearchName(modName));
        };
    }

    private Set<String> researchModIdCandidates(String modName) {
        String key = normalizeResearchKey(modName);
        Set<String> candidates = new HashSet<>();
        candidates.add(key);
        switch (key) {
            case "mekanism_qio" -> {
                candidates.add("mekanismqio");
                candidates.add("mekanism_qio");
            }
            case "torch_master" -> candidates.add("torchmaster");
            case "torchmaster" -> candidates.add("torch_master");
            case "augmentstation" -> candidates.add("augment_station");
            case "augment_station" -> candidates.add("augmentstation");
            default -> {
            }
        }
        return candidates;
    }

    private String normalizeResearchKey(String value) {
        String raw = value == null ? "" : value.toLowerCase(Locale.ROOT);
        int separator = raw.indexOf(':');
        if (separator >= 0) {
            raw = raw.substring(separator + 1);
        }
        return raw.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public void unlockExpertise(ServerPlayer player, String expertiseName) {
        APSkillLockManager.unlockExpertise(player, expertiseName);
        APSkillLockManager.syncToClient(player);
        LOGGER.info("Unlocked expertise {} for player {}", expertiseName, player.getName().getString());
    }

    private static final ThreadLocal<Integer> CURRENT_PLAYER_LEVEL = ThreadLocal.withInitial(() -> 0);

    public boolean isConnected() {
        return apClient != null && apClient.isConnected();
    }

    //Get how many chest checks this player has found (for generating location IDs)
    public long getChestCheckCount(ServerPlayer player) {
        // This should track per-player how many chests they've opened
        // For now, just count checked locations
        long count = 0;
        for (long id = CHEST_CHECK_BASE_ID; id < CHEST_CHECK_BASE_ID + 500L; id++) {
            if (isLocationChecked(id)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
    public long getNextChestCheckIdForItem(ServerPlayer player) {
        APProgressData progressData = APProgressData.get(player.getServer());

        long currentCount = progressData.getSharedChestCheckCount();

        if (currentCount >= totalChestChecks) {
            LOGGER.info("Server has reached max chest checks ({})", totalChestChecks);
            return 0L;
        }

        long locationId = CHEST_CHECK_BASE_ID + currentCount;

        progressData.setSharedChestCheckCount(currentCount + 1);

        LOGGER.info("Generated chest check item for player {}: location ID {} (check #{})",
                player.getName().getString(), locationId, currentCount + 1);

        return locationId;
    }
}
