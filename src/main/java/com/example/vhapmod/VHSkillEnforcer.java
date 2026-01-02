package com.example.vhapmod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Aggressively removes unauthorized skills by directly manipulating state
 */
public class VHSkillEnforcer {

    private static final Logger LOGGER = LogManager.getLogger();

    // Track which unauthorized skills we've already warned about
    private static final Map<UUID, Set<String>> warnedSkills = new HashMap<>();

    // VH classes
    private static Class<?> playerAbilitiesDataClass;
    private static Class<?> playerTalentsDataClass;

    static {
        try {
            playerAbilitiesDataClass = Class.forName("iskallia.vault.world.data.PlayerAbilitiesData");
            playerTalentsDataClass = Class.forName("iskallia.vault.world.data.PlayerTalentsData");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to load VH classes: " + e.getMessage());
        }
    }

    public static void enforceSkillLocks(ServerPlayer player) {
        try {
            enforceAbilityLocks(player);
            enforceTalentLocks(player);
            //enforceModLocks(player);
            //enforceExpertiseLocks(player);
        } catch (Exception e) {
            LOGGER.error("Error enforcing locks: " + e.getMessage());
        }
    }

    private static void enforceAbilityLocks(ServerPlayer player) {
        try {
            Method getData = playerAbilitiesDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            Method getAbilities = data.getClass().getMethod("getAbilities", UUID.class);
            Object abilityTree = getAbilities.invoke(data, player.getUUID());

            if (abilityTree == null) return;

            checkAndRemoveUnauthorized(abilityTree, player, true, data);

        } catch (Exception e) {
            LOGGER.error("Error enforcing abilities: " + e.getMessage());
        }
    }

    private static void enforceTalentLocks(ServerPlayer player) {
        try {
            Method getData = playerTalentsDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            Method getTalents = data.getClass().getMethod("getTalents", UUID.class);
            Object talentTree = getTalents.invoke(data, player.getUUID());

            if (talentTree == null) return;

            boolean anyRemoved = checkAndRemoveUnauthorized(talentTree, player, false, data);

            // If we removed any talents, force a "fake relog" to recalculate
            if (anyRemoved) {
                try {
                    // Mark data as dirty
                    Method setDirty = data.getClass().getMethod("setDirty");
                    setDirty.invoke(data);

                    // Force VH to reload the player's talent data as if they just logged in
                    // This triggers all the recalculation logic
                    try {
                        // Call load or similar method on the data
                        Method load = data.getClass().getMethod("load", player.getLevel().getClass());
                        load.invoke(data, player.getLevel());
                        LOGGER.info("Called load() to force recalculation");
                    } catch (NoSuchMethodException e) {
                        LOGGER.warn("No load method found");
                    }

                    // Try to re-get the talents (forces recalculation)
                    Object freshTalentTree = getTalents.invoke(data, player.getUUID());

                    // Call onTick to force re-evaluation of conditions
                    if (freshTalentTree != null) {
                        Object context = createSkillContext(player);
                        if (context != null) {
                            try {
                                Method onTick = freshTalentTree.getClass().getMethod("onTick", context.getClass());
                                onTick.invoke(freshTalentTree, context);
                                LOGGER.info("Called onTick after reload");
                            } catch (Exception e) {
                                LOGGER.warn("onTick failed: " + e.getMessage());
                            }
                        }
                    }

                    LOGGER.info("Forced talent recalculation for " + player.getName().getString());
                } catch (Exception e) {
                    LOGGER.warn("Failed to recalculate talents: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error enforcing talents: " + e.getMessage());
        }
    }

    private static void enforceModLocks(ServerPlayer player) {
        try {
            Class<?> researchDataClass = Class.forName("iskallia.vault.world.data.PlayerResearchesData");
            Method getData = researchDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            Method getResearches = data.getClass().getMethod("getResearches", UUID.class);
            Object researchTree = getResearches.invoke(data, player.getUUID());

            if (researchTree == null) return;

            checkAndRemoveUnauthorizedMods(researchTree, player, data);

        } catch (Exception e) {
            LOGGER.error("Error enforcing mods: " + e.getMessage());
        }
    }

    private static void enforceExpertiseLocks(ServerPlayer player) {
        try {
            Class<?> expertiseDataClass = Class.forName("iskallia.vault.world.data.PlayerExpertisesData");
            Method getData = expertiseDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            Method getExpertises = data.getClass().getMethod("getExpertises", UUID.class);
            Object expertiseTree = getExpertises.invoke(data, player.getUUID());

            if (expertiseTree == null) return;

            checkAndRemoveUnauthorizedExpertises(expertiseTree, player, data);

        } catch (Exception e) {
            LOGGER.error("Error enforcing expertises: " + e.getMessage());
        }
    }

    private static boolean checkAndRemoveUnauthorized(Object skillTree, ServerPlayer player, boolean isAbility, Object dataObject) {
        try {
            List<Object> toRemove = new ArrayList<>();
            Method iterate = skillTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);

            Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
            iterate.invoke(skillTree, skillClass, (java.util.function.Consumer<Object>) skill -> {
                try {
                    Method isUnlocked = skill.getClass().getMethod("isUnlocked");
                    if ((boolean) isUnlocked.invoke(skill)) {
                        Method getName = skill.getClass().getMethod("getName");
                        String name = (String) getName.invoke(skill);

                        if (name != null && !name.isEmpty()) {
                            String normalized = (isAbility ? "vhskill:" : "vhtalent:") + name.toLowerCase().replace(" ", "_");

                            LOGGER.info("Checking {}: raw='{}' normalized='{}' UUID={}", isAbility ? "skill" : "talent", name, normalized, player.getUUID());

                            // Pass RAW name - APSkillLockManager will normalize it
                            boolean allowed = isAbility
                                    ? APSkillLockManager.isSkillUnlockedSilent(player, name)
                                    : APSkillLockManager.isTalentUnlockedSilent(player, name);

                            LOGGER.info("Result: allowed={} for UUID={}", allowed, player.getUUID());

                            if (!allowed) {
                                LOGGER.info("Unlocked talents in storage: {}", APSkillLockManager.getUnlockedTalents(player));
                            }

                            if (!allowed) {
                                toRemove.add(skill);
                            }
                        }
                    }
                } catch (Exception e) {}
            });

            UUID playerId = player.getUUID();
            Set<String> warned = warnedSkills.computeIfAbsent(playerId, k -> new HashSet<>());
            boolean anyRemoved = false;

            for (Object skill : toRemove) {
                Method getName = skill.getClass().getMethod("getName");
                String name = (String) getName.invoke(skill);
                String normalized = (isAbility ? "vhskill:" : "vhtalent:") + name.toLowerCase().replace(" ", "_");

                if (!warned.contains(normalized)) {
                    LOGGER.warn("Removing unauthorized {} '{}' from {}",
                            isAbility ? "skill" : "talent", name, player.getName().getString());

                    // Get the cost BEFORE removing
                    int pointCost = getSkillPointCost(skill);

                    // Try to remove the skill
                    boolean removed = forceRemoveSkill(skill, player);

                    if (removed) {
                        anyRemoved = true;

                        player.sendMessage(
                                new TextComponent("[AP] ").withStyle(ChatFormatting.RED)
                                        .append(new TextComponent(name + " is locked! It has been removed.")),
                                player.getUUID()
                        );

                        // Refund the actual cost
                        if (pointCost > 0) {
                            refundSkillPoints(player, pointCost);
                        }

                        // Don't add to warned - we want to keep checking in case they try again
                        // warned.add(normalized);
                    } else {
                        // Failed to remove, mark as warned so we don't spam
                        warned.add(normalized);
                    }

                    // Mark data as dirty to save changes
                    try {
                        Method setDirty = dataObject.getClass().getMethod("setDirty");
                        setDirty.invoke(dataObject);
                    } catch (Exception e) {
                        LOGGER.warn("Could not mark data as dirty");
                    }
                }
            }

            return anyRemoved;
        } catch (Exception e) {
            LOGGER.error("Error checking skills: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private static void checkAndRemoveUnauthorizedMods(Object researchTree, ServerPlayer player, Object dataObject) {
        try {
            List<Object> toRemove = new ArrayList<>();
            Method iterate = researchTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);

            Class<?> researchClass = Class.forName("iskallia.vault.research.ResearchTree");
            iterate.invoke(researchTree, researchClass, (java.util.function.Consumer<Object>) research -> {
                try {
                    Method isResearched = research.getClass().getMethod("isResearched");
                    if ((boolean) isResearched.invoke(research)) {
                        Method getName = research.getClass().getMethod("getName");
                        String name = (String) getName.invoke(research);

                        if (name != null && !name.isEmpty()) {
                            String normalized = "vhmod:" + name.toLowerCase().replace(" ", "_");

                            if (!APSkillLockManager.isModUnlockedSilent(player, normalized)) {
                                toRemove.add(research);
                            }
                        }
                    }
                } catch (Exception e) {}
            });

            UUID playerId = player.getUUID();
            Set<String> warned = warnedSkills.computeIfAbsent(playerId, k -> new HashSet<>());

            for (Object research : toRemove) {
                Method getName = research.getClass().getMethod("getName");
                String name = (String) getName.invoke(research);
                String normalized = "vhmod:" + name.toLowerCase().replace(" ", "_");

                if (!warned.contains(normalized)) {
                    LOGGER.warn("Removing unauthorized mod '{}' from {}", name, player.getName().getString());

                    // Reset the research
                    Method unresearch = research.getClass().getMethod("reset");
                    unresearch.invoke(research);

                    player.sendMessage(
                            new TextComponent("[AP] ").withStyle(ChatFormatting.RED)
                                    .append(new TextComponent(name + " is locked! It has been removed.")),
                            player.getUUID()
                    );

                    try {
                        Method setDirty = dataObject.getClass().getMethod("setDirty");
                        setDirty.invoke(dataObject);
                    } catch (Exception e) {
                        LOGGER.warn("Could not mark data as dirty");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking mods: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkAndRemoveUnauthorizedExpertises(Object expertiseTree, ServerPlayer player, Object dataObject) {
        try {
            List<Object> toRemove = new ArrayList<>();
            Method iterate = expertiseTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);

            Class<?> expertiseClass = Class.forName("iskallia.vault.skill.expertise.type.ExpertiseTree");
            iterate.invoke(expertiseTree, expertiseClass, (java.util.function.Consumer<Object>) expertise -> {
                try {
                    Method isLearned = expertise.getClass().getMethod("isLearned");
                    if ((boolean) isLearned.invoke(expertise)) {
                        Method getName = expertise.getClass().getMethod("getName");
                        String name = (String) getName.invoke(expertise);

                        if (name != null && !name.isEmpty()) {
                            String normalized = "vhexpertise:" + name.toLowerCase().replace(" ", "_");

                            if (!APSkillLockManager.isExpertiseUnlockedSilent(player, normalized)) {
                                toRemove.add(expertise);
                            }
                        }
                    }
                } catch (Exception e) {}
            });

            UUID playerId = player.getUUID();
            Set<String> warned = warnedSkills.computeIfAbsent(playerId, k -> new HashSet<>());

            for (Object expertise : toRemove) {
                Method getName = expertise.getClass().getMethod("getName");
                String name = (String) getName.invoke(expertise);
                String normalized = "vhexpertise:" + name.toLowerCase().replace(" ", "_");

                if (!warned.contains(normalized)) {
                    LOGGER.warn("Removing unauthorized expertise '{}' from {}", name, player.getName().getString());

                    // Reset the expertise
                    Method unlearn = expertise.getClass().getMethod("reset");
                    unlearn.invoke(expertise);

                    player.sendMessage(
                            new TextComponent("[AP] ").withStyle(ChatFormatting.RED)
                                    .append(new TextComponent(name + " is locked! It has been removed.")),
                            player.getUUID()
                    );

                    try {
                        Method setDirty = dataObject.getClass().getMethod("setDirty");
                        setDirty.invoke(dataObject);
                    } catch (Exception e) {
                        LOGGER.warn("Could not mark data as dirty");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking expertises: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the total skill points spent on this skill (including all tiers for TieredSkills)
     */
    private static int getSkillPointCost(Object skill) {
        try {
            // For TieredSkill, we need to get the spent points for all learned tiers
            Method getSpentLearnPoints = skill.getClass().getMethod("getSpentLearnPoints");
            return (int) getSpentLearnPoints.invoke(skill);
        } catch (NoSuchMethodException e) {
            // Not a TieredSkill, try getLearnPointCost
            try {
                Method getLearnPointCost = skill.getClass().getMethod("getLearnPointCost");
                return (int) getLearnPointCost.invoke(skill);
            } catch (Exception e2) {
                LOGGER.warn("Could not get skill cost");
                return 1; // Default to 1
            }
        } catch (Exception e) {
            LOGGER.warn("Error getting skill cost: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Force remove a skill by resetting tier AND calling cleanup methods
     */
    private static boolean forceRemoveSkill(Object skill, ServerPlayer player) {
        String className = skill.getClass().getSimpleName();
        LOGGER.info("Attempting to remove skill type: " + className);

        try {
            // Create SkillContext - may return null
            Object context = createSkillContext(player);

            // Special handling for SpecializedSkill (abilities)
            if (className.contains("SpecializedSkill")) {
                try {
                    Method getSpecialization = skill.getClass().getMethod("getSpecialization");
                    Object specialization = getSpecialization.invoke(skill);

                    if (specialization != null) {
                        LOGGER.info("Found specialization: " + specialization.getClass().getSimpleName());
                        boolean removed = resetTieredSkillAndCleanup(specialization, context);
                        if (removed) {
                            LOGGER.info("Successfully reset specialization");
                            return true;
                        } else {
                            LOGGER.warn("Failed to reset specialization");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to handle SpecializedSkill: " + e.getMessage());
                }
            }

            // For TieredSkill or Talents - reset tier AND call cleanup
            boolean removed = resetTieredSkillAndCleanup(skill, context);
            if (removed) {
                LOGGER.info("Successfully removed skill");
            } else {
                LOGGER.warn("Failed to remove skill");
            }
            return removed;

        } catch (Exception e) {
            LOGGER.error("Exception in forceRemoveSkill: " + e.getMessage());
            e.printStackTrace();

            // Last ditch effort - just reset tier without any context
            LOGGER.warn("Attempting simple tier reset as last resort");
            try {
                if (className.contains("SpecializedSkill")) {
                    Method getSpecialization = skill.getClass().getMethod("getSpecialization");
                    Object specialization = getSpecialization.invoke(skill);
                    if (specialization != null) {
                        return resetTieredSkill(specialization);
                    }
                }
                return resetTieredSkill(skill);
            } catch (Exception e2) {
                LOGGER.error("Even simple tier reset failed: " + e2.getMessage());
                return false;
            }
        }
    }

    /**
     * Create a SkillContext using multiple fallback methods
     */
    private static Object createSkillContext(ServerPlayer player) {
        // Method 1: Try using CommonEvents.Source
        try {
            LOGGER.info("Trying Method 1: CommonEvents.Source");
            Class<?> contextClass = Class.forName("iskallia.vault.skill.base.SkillContext");
            Class<?> sourceClass = Class.forName("iskallia.vault.core.event.CommonEvents$Source");

            // Try to create Source from player
            Method of = sourceClass.getDeclaredMethod("of", Object.class);
            of.setAccessible(true);
            Object source = of.invoke(null, player);

            // Create SkillContext
            Object context = contextClass.getConstructor(sourceClass).newInstance(source);
            LOGGER.info("Created SkillContext successfully with Method 1");
            return context;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Method 1: CommonEvents$Source class not found");
        } catch (NoSuchMethodException e) {
            LOGGER.warn("Method 1: Method 'of' not found on Source class");
        } catch (Exception e) {
            LOGGER.warn("Method 1 failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        // Method 2: Try creating SkillContext directly with ServerPlayer
        try {
            LOGGER.info("Trying Method 2: Direct ServerPlayer constructor");
            Class<?> contextClass = Class.forName("iskallia.vault.skill.base.SkillContext");
            Object context = contextClass.getConstructor(ServerPlayer.class).newInstance(player);
            LOGGER.info("Created SkillContext with Method 2");
            return context;
        } catch (NoSuchMethodException e) {
            LOGGER.warn("Method 2 failed: No constructor SkillContext(ServerPlayer)");
        } catch (Exception e) {
            LOGGER.warn("Method 2 failed: " + e.getClass().getSimpleName());
        }

        // Give up
        LOGGER.warn("All SkillContext creation methods failed, proceeding without context");
        return null;
    }

    /**
     * Reset a TieredSkill's tier AND call all cleanup methods
     */
    private static boolean resetTieredSkillAndCleanup(Object skill, Object context) {
        try {
            // If context is available, try cleanup methods
            if (context != null) {
                // STEP 1: Call onRemove BEFORE resetting (while skill is still "unlocked")
                try {
                    Method onRemove = skill.getClass().getMethod("onRemove", context.getClass());
                    onRemove.invoke(skill, context);
                    LOGGER.info("Called onRemove()");
                } catch (NoSuchMethodException e) {
                    // onRemove doesn't exist, that's okay
                } catch (Exception e) {
                    LOGGER.warn("onRemove failed: " + e.getMessage());
                }
            }

            // STEP 2: Reset the tier to 0 (works without context)
            boolean tierReset = resetTieredSkill(skill);

            if (!tierReset) {
                LOGGER.warn("Tier reset failed");
                return false;
            }

            // STEP 3: Call onAdd if context available
            if (context != null) {
                try {
                    Method onAdd = skill.getClass().getMethod("onAdd", context.getClass());
                    onAdd.invoke(skill, context);
                    LOGGER.info("Called onAdd() to clean up");
                } catch (NoSuchMethodException e) {
                    // onAdd doesn't exist, that's okay
                } catch (Exception e) {
                    LOGGER.warn("onAdd failed: " + e.getMessage());
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to reset and cleanup: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reset a TieredSkill by setting tier and bonusTier to 0
     */
    private static boolean resetTieredSkill(Object skill) {
        try {
            Field tierField = null;
            Field bonusTierField = null;

            // Walk up the class hierarchy to find the fields
            Class<?> currentClass = skill.getClass();
            while (currentClass != null && tierField == null) {
                try {
                    tierField = currentClass.getDeclaredField("tier");
                } catch (NoSuchFieldException e) {}
                currentClass = currentClass.getSuperclass();
            }

            // Also find bonusTier
            currentClass = skill.getClass();
            while (currentClass != null && bonusTierField == null) {
                try {
                    bonusTierField = currentClass.getDeclaredField("bonusTier");
                } catch (NoSuchFieldException e) {}
                currentClass = currentClass.getSuperclass();
            }

            boolean success = false;

            if (tierField != null) {
                tierField.setAccessible(true);
                int oldTier = tierField.getInt(skill);
                tierField.setInt(skill, 0);
                LOGGER.info("Reset tier from " + oldTier + " to 0");
                success = true;
            }

            if (bonusTierField != null) {
                bonusTierField.setAccessible(true);
                bonusTierField.setInt(skill, 0);
                LOGGER.info("Reset bonusTier to 0");
                success = true;
            }

            return success;
        } catch (Exception e) {
            LOGGER.warn("Failed to reset TieredSkill: " + e.getMessage());
            return false;
        }
    }

    private static void refundSkillPoints(ServerPlayer player, int points) {
        try {
            Class<?> playerVaultStatsClass = Class.forName("iskallia.vault.world.data.PlayerVaultStatsData");
            Method getData = playerVaultStatsClass.getMethod("get", player.getLevel().getClass());
            Object statsData = getData.invoke(null, player.getLevel());

            Method addSkillPoints = statsData.getClass().getMethod("addSkillPoints", ServerPlayer.class, int.class);
            addSkillPoints.invoke(statsData, player, points);

            LOGGER.info("Refunded {} skill points to {}", points, player.getName().getString());
            player.sendMessage(
                    new TextComponent("§e[AP] Refunded " + points + " skill point" + (points != 1 ? "s" : "")),
                    player.getUUID()
            );
        } catch (Exception e) {
            LOGGER.error("Failed to refund: " + e.getMessage());
        }
    }

    public static void clearWarnings(UUID playerId) {
        warnedSkills.remove(playerId);
    }
}