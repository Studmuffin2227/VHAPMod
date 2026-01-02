package com.example.vhapmod;

import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Reads actual VH player data (skills, talents, mods, etc.)
 */
public class VHDataReader {

    private static final Logger LOGGER = LogManager.getLogger();

    // Cached classes for performance
    private static Class<?> playerAbilitiesDataClass;
    private static Class<?> playerTalentsDataClass;
    private static Class<?> playerExpertisesDataClass;
    private static Class<?> playerResearchesDataClass;
    private static Class<?> playerVaultStatsDataClass;

    private static Class<?> abilityTreeClass;
    private static Class<?> talentTreeClass;
    private static Class<?> researchTreeClass;

    static {
        try {
            // Load all the data classes
            playerAbilitiesDataClass = Class.forName("iskallia.vault.world.data.PlayerAbilitiesData");
            playerTalentsDataClass = Class.forName("iskallia.vault.world.data.PlayerTalentsData");
            playerExpertisesDataClass = Class.forName("iskallia.vault.world.data.PlayerExpertisesData");
            playerResearchesDataClass = Class.forName("iskallia.vault.world.data.PlayerResearchesData");
            playerVaultStatsDataClass = Class.forName("iskallia.vault.world.data.PlayerVaultStatsData");

            // Load tree classes
            abilityTreeClass = Class.forName("iskallia.vault.skill.ability.AbilityTree");
            talentTreeClass = Class.forName("iskallia.vault.skill.talent.TalentTree");
            researchTreeClass = Class.forName("iskallia.vault.research.ResearchTree");

            LOGGER.info("Successfully loaded all VH data classes!");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to load VH classes: " + e.getMessage());
        }
    }

    /**
     * Get player's vault level
     */
    public static int getPlayerLevel(ServerPlayer player) {
        try {
            Method getData = playerVaultStatsDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            Method getStats = data.getClass().getMethod("getVaultStats", player.getUUID().getClass());
            Object stats = getStats.invoke(data, player.getUUID());

            Method getLevel = stats.getClass().getMethod("getVaultLevel");
            return (int) getLevel.invoke(stats);
        } catch (Exception e) {
            LOGGER.debug("Could not get player level: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get list of all unlocked ability names
     */
    public static List<String> getUnlockedAbilities(ServerPlayer player) {
        Set<String> unlockedSet = new HashSet<>(); // Use Set to auto-deduplicate
        try {
            Method getData = playerAbilitiesDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            Method getAbilities = data.getClass().getMethod("getAbilities", player.getUUID().getClass());
            Object abilityTree = getAbilities.invoke(data, player.getUUID());

            if (abilityTree != null) {
                // Use iterate method to get all skills
                Method iterate = abilityTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);

                List<Object> skills = new ArrayList<>();
                java.util.function.Consumer<Object> collector = skills::add;

                Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
                iterate.invoke(abilityTree, skillClass, collector);

                for (Object skill : skills) {
                    Method isUnlocked = skill.getClass().getMethod("isUnlocked");
                    boolean unlocked_status = (boolean) isUnlocked.invoke(skill);

                    Method getName = skill.getClass().getMethod("getName");
                    String name = (String) getName.invoke(skill);

                    // Only track skills with actual names
                    if (name != null && !name.isEmpty() && unlocked_status) {
                        unlockedSet.add(name); // Set automatically deduplicates
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not get abilities: " + e.getMessage(), e);
        }
        return new ArrayList<>(unlockedSet); // Convert back to List
    }

    private static String getParameterTypes(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * Get list of all unlocked talent names
     */
    public static List<String> getUnlockedTalents(ServerPlayer player) {
        Set<String> unlockedSet = new HashSet<>();
        try {
            Method getData = playerTalentsDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            Method getTalents = data.getClass().getMethod("getTalents", player.getUUID().getClass());
            Object talentTree = getTalents.invoke(data, player.getUUID());

            if (talentTree != null) {
                // Use iterate method like we did for abilities
                Method iterate = talentTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);

                List<Object> skills = new ArrayList<>();
                java.util.function.Consumer<Object> collector = skills::add;

                Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
                iterate.invoke(talentTree, skillClass, collector);

                for (Object skill : skills) {
                    Method isUnlocked = skill.getClass().getMethod("isUnlocked");
                    boolean unlocked_status = (boolean) isUnlocked.invoke(skill);

                    Method getName = skill.getClass().getMethod("getName");
                    String name = (String) getName.invoke(skill);

                    if (name != null && !name.isEmpty() && unlocked_status) {
                        // ADD THIS DEBUG LINE:
                        LOGGER.info("Found unlocked talent: '{}'", name);
                        unlockedSet.add(name);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get talents: " + e.getMessage());
        }
        return new ArrayList<>(unlockedSet);
    }

    /**
     * Get list of all unlocked expertise names
     */
    public static List<String> getUnlockedExpertises(ServerPlayer player) {
        Set<String> unlockedSet = new HashSet<>();
        try {
            Method getData = playerExpertisesDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            Method getExpertises = data.getClass().getMethod("getExpertises", player.getUUID().getClass());
            Object expertiseTree = getExpertises.invoke(data, player.getUUID());

            if (expertiseTree != null) {
                // Use iterate method like we did for abilities and talents
                Method iterate = expertiseTree.getClass().getMethod("iterate", Class.class, java.util.function.Consumer.class);

                List<Object> skills = new ArrayList<>();
                java.util.function.Consumer<Object> collector = skills::add;

                Class<?> skillClass = Class.forName("iskallia.vault.skill.base.Skill");
                iterate.invoke(expertiseTree, skillClass, collector);

                for (Object skill : skills) {
                    Method isUnlocked = skill.getClass().getMethod("isUnlocked");
                    boolean unlocked_status = (boolean) isUnlocked.invoke(skill);

                    Method getName = skill.getClass().getMethod("getName");
                    String name = (String) getName.invoke(skill);

                    if (name != null && !name.isEmpty() && unlocked_status) {
                        // Debug logging to see exact names
                        LOGGER.info("Found unlocked expertise: '{}'", name);
                        unlockedSet.add(name);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not get expertises: " + e.getMessage(), e);
        }
        return new ArrayList<>(unlockedSet);
    }

    /**
     * Get list of all researched mods
     */
    public static List<String> getResearchedMods(ServerPlayer player) {
        List<String> researched = new ArrayList<>();
        try {
            Method getData = playerResearchesDataClass.getMethod("get", player.getLevel().getClass());
            Object data = getData.invoke(null, player.getLevel());

            Method getResearches = data.getClass().getMethod("getResearches", player.getUUID().getClass());
            Object researchTree = getResearches.invoke(data, player.getUUID());

            if (researchTree != null) {
                Method getResearchesDone = researchTree.getClass().getMethod("getResearchesDone");
                Object researchSet = getResearchesDone.invoke(researchTree);

                if (researchSet instanceof Iterable) {
                    for (Object research : (Iterable<?>) researchSet) {
                        // Check if it's already a String or a Research object
                        if (research instanceof String) {
                            String modName = (String) research;
                            // ADD THIS DEBUG LINE:
                            LOGGER.info("Found researched mod (String): '{}'", modName);
                            researched.add(modName);
                        } else {
                            // It's a Research object, get the name
                            Method getName = research.getClass().getMethod("getName");
                            String name = (String) getName.invoke(research);
                            // ADD THIS DEBUG LINE:
                            LOGGER.info("Found researched mod (Object): '{}'", name);
                            researched.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not get researches: " + e.getMessage(), e);
        }
        return researched;
    }

    /**
     * Test method - logs all player progression
     */
    public static void logPlayerProgression(ServerPlayer player) {
        LOGGER.info("=== Player Progression for " + player.getName().getString() + " ===");
        LOGGER.info("Level: " + getPlayerLevel(player));

        List<String> abilities = getUnlockedAbilities(player);
        LOGGER.info("Unlocked Abilities (" + abilities.size() + "): " + abilities);

        List<String> talents = getUnlockedTalents(player);
        LOGGER.info("Unlocked Talents (" + talents.size() + "): " + talents);

        // ADD THIS:
        List<String> expertises = getUnlockedExpertises(player);
        LOGGER.info("Unlocked Expertises (" + expertises.size() + "): " + expertises);

        List<String> mods = getResearchedMods(player);
        LOGGER.info("Researched Mods (" + mods.size() + "): " + mods);

        LOGGER.info("=== End Player Progression ===");
    }
}