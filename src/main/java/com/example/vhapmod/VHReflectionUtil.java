package com.example.vhapmod;

import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class to access Vault Hunters data via reflection
 */
public class VHReflectionUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    // Cache commonly used classes/methods
    private static Class<?> playerVaultStatsClass;
    private static Method getVaultStatsMethod;

    static {
        try {
            // Try to find VH's player stats class
            // Common package patterns: iskallia.vault.world.data or iskallia.vault.player
            playerVaultStatsClass = Class.forName("iskallia.vault.world.data.PlayerVaultStatsData");
            LOGGER.info("Found VH PlayerVaultStatsData class!");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Could not find VH PlayerVaultStatsData class, trying alternate names...");

            // Try alternate class names
            String[] possibleNames = {
                    "iskallia.vault.player.VaultPlayerStats",
                    "iskallia.vault.data.PlayerVaultStats",
                    "iskallia.vault.world.PlayerVaultData"
            };

            for (String name : possibleNames) {
                try {
                    playerVaultStatsClass = Class.forName(name);
                    LOGGER.info("Found VH class: " + name);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            if (playerVaultStatsClass == null) {
                LOGGER.error("Could not find any VH player data class!");
            }
        }
    }

    /**
     * Get a player's VH level
     */
    public static int getPlayerLevel(ServerPlayer player) {
        try {
            // Try to access VH's level systemhttps://larian.com/support/baldur-s-gate-3
            // This is a guess - we'll need to explore VH's actual structure
            Object vaultStats = getPlayerVaultStats(player);
            if (vaultStats != null) {
                Method getLevelMethod = vaultStats.getClass().getMethod("getVaultLevel", player.getUUID().getClass());
                Object level = getLevelMethod.invoke(vaultStats, player.getUUID());
                if (level instanceof Integer) {
                    return (Integer) level;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get player level: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get player's vault stats object
     */
    private static Object getPlayerVaultStats(ServerPlayer player) {
        try {
            if (playerVaultStatsClass != null) {
                // Try common method names for getting player data
                Method getMethod = playerVaultStatsClass.getMethod("get", player.getLevel().getClass());
                return getMethod.invoke(null, player.getLevel());
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get vault stats: " + e.getMessage());
        }
        return null;
    }

    /**
     * Explore and log VH classes to help us understand the structure
     */
    public static void exploreVHStructure() {
        LOGGER.info("=== Exploring VH Class Structure ===");

        // Expanded search - VH likely stores player data in world.data
        String[] searchPatterns = {
                "iskallia.vault.skill.*",
                "iskallia.vault.research.*",
                "iskallia.vault.world.data.*",
                "iskallia.vault.gear.data.*"
        };

        // Try to find PlayerData classes
        String[] playerDataClasses = {
                "iskallia.vault.world.data.PlayerAbilitiesData",
                "iskallia.vault.world.data.PlayerTalentsData",
                "iskallia.vault.world.data.PlayerExpertisesData",
                "iskallia.vault.world.data.PlayerResearchesData",
                "iskallia.vault.world.data.PlayerVaultStatsData",
                "iskallia.vault.world.data.ServerVaults",
                "iskallia.vault.skill.base.SkillTree",
                "iskallia.vault.skill.base.TieredSkill",
                "iskallia.vault.skill.base.Skill"
        };

        LOGGER.info("Searching for player data classes...");
        for (String className : playerDataClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                LOGGER.info("  *** FOUND: " + clazz.getName() + " ***");

                // Log public methods (first 30)
                LOGGER.info("    Public Methods:");
                Method[] methods = clazz.getDeclaredMethods();
                int count = 0;
                for (Method method : methods) {
                    if (java.lang.reflect.Modifier.isPublic(method.getModifiers()) && count < 30) {
                        LOGGER.info("      - " + method.getName() + "(" + getParameterTypes(method) + ")");
                        count++;
                    }
                }

                // Log public/static fields
                LOGGER.info("    Public/Static Fields:");
                for (Field field : clazz.getDeclaredFields()) {
                    int mods = field.getModifiers();
                    if (java.lang.reflect.Modifier.isPublic(mods) || java.lang.reflect.Modifier.isStatic(mods)) {
                        String modStr = java.lang.reflect.Modifier.toString(mods);
                        LOGGER.info("      - " + modStr + " " + field.getName() + " : " + field.getType().getSimpleName());
                    }
                }
            } catch (ClassNotFoundException e) {
                // Class doesn't exist, skip
            }
        }

        LOGGER.info("=== End VH Structure Exploration ===");
    }

    /**
     * Helper to get readable parameter types for a method
     */
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
}