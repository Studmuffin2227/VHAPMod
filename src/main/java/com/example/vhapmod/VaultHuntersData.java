package com.example.vhapmod;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines all Vault Hunters locations (checks) and items for Archipelago integration.
 *
 * Location ID Ranges:
 * - 43000-43025: Skills (26 total)
 * - 43100-43200: Talents (~100 slots)
 * - 43200-43250: Expertises (~50 slots)
 * - 43300-43400: Mods (~100 slots)
 * - 43400-43408: Milestone checks (levels, first vault, etc.)
 */
public class VaultHuntersData {

    // ==================== LOCATION IDs (CHECKS) ====================

    // Base IDs for each category
    public static final long SKILL_BASE_ID = 43000L;
    public static final long TALENT_BASE_ID = 43100L;
    public static final long EXPERTISE_BASE_ID = 43200L;
    public static final long MOD_BASE_ID = 43300L;
    public static final long MILESTONE_BASE_ID = 43400L;

    // Skills (26 total) - IDs 43000-43025
    private static final HashMap<String, Long> SKILL_LOCATIONS = new HashMap<>() {{
        // Combat Skills
        put("vhskill:nova", SKILL_BASE_ID + 0);
        put("vhskill:fireball", SKILL_BASE_ID + 1);
        put("vhskill:javelin", SKILL_BASE_ID + 2);
        put("vhskill:stonefall", SKILL_BASE_ID + 3);
        put("vhskill:ice_bolt", SKILL_BASE_ID + 4);
        put("vhskill:implode", SKILL_BASE_ID + 5);
        put("vhskill:shield_bash", SKILL_BASE_ID + 6);
        put("vhskill:arcane", SKILL_BASE_ID + 7);
        put("vhskill:earthquake", SKILL_BASE_ID + 8);
        put("vhskill:lightning_strike", SKILL_BASE_ID + 9);
        put("vhskill:chaos_cube", SKILL_BASE_ID + 10);

        // Utility Skills
        put("vhskill:vein_miner", SKILL_BASE_ID + 11);
        put("vhskill:ghost_walk", SKILL_BASE_ID + 12);
        put("vhskill:heal", SKILL_BASE_ID + 13);
        put("vhskill:dash", SKILL_BASE_ID + 14);
        put("vhskill:hunter", SKILL_BASE_ID + 15);
        put("vhskill:mega_jump", SKILL_BASE_ID + 16);
        put("vhskill:mana_shield", SKILL_BASE_ID + 17);
        put("vhskill:taunt", SKILL_BASE_ID + 18);

        // Powerup Skills
        put("vhskill:battle_cry", SKILL_BASE_ID + 19);
        put("vhskill:smite", SKILL_BASE_ID + 20);
        put("vhskill:empower", SKILL_BASE_ID + 21);
        put("vhskill:rejuvenation_totem", SKILL_BASE_ID + 22);
        put("vhskill:shell", SKILL_BASE_ID + 23);
        put("vhskill:rampage", SKILL_BASE_ID + 24);

        //Ultimate Skills
        put("vhskill:storm_arrow", SKILL_BASE_ID + 25);
    }};

    // ========== TALENT LOCATIONS HASHMAP (43100-43141) ==========
    // Based on locations.py order

    private static final HashMap<String, Long> TALENT_LOCATIONS = new HashMap<>() {{
        // From locations.py - 42 talents total
        put("vhtalent:speed", TALENT_BASE_ID + 0);              // 43100
        put("vhtalent:haste", TALENT_BASE_ID + 1);              // 43101
        put("vhtalent:strength", TALENT_BASE_ID + 2);           // 43102
        put("vhtalent:intelligence", TALENT_BASE_ID + 3);       // 43103
        put("vhtalent:nucleus", TALENT_BASE_ID + 4);            // 43104
        put("vhtalent:daze", TALENT_BASE_ID + 5);               // 43105
        put("vhtalent:last_stand", TALENT_BASE_ID + 6);         // 43106
        put("vhtalent:berserking", TALENT_BASE_ID + 7);         // 43107
        put("vhtalent:sorcery", TALENT_BASE_ID + 8);            // 43108
        put("vhtalent:witchery", TALENT_BASE_ID + 9);           // 43109
        put("vhtalent:frozen_impact", TALENT_BASE_ID + 10);     // 43110
        put("vhtalent:frostbite", TALENT_BASE_ID + 11);         // 43111
        put("vhtalent:methodical", TALENT_BASE_ID + 12);        // 43112
        put("vhtalent:depleted", TALENT_BASE_ID + 13);          // 43113
        put("vhtalent:prudent", TALENT_BASE_ID + 14);           // 43114
        put("vhtalent:stoneskin", TALENT_BASE_ID + 15);         // 43115
        put("vhtalent:blight", TALENT_BASE_ID + 16);            // 43116
        put("vhtalent:toxic_reaction", TALENT_BASE_ID + 17);    // 43117
        put("vhtalent:arcana", TALENT_BASE_ID + 18);            // 43118
        put("vhtalent:blazing", TALENT_BASE_ID + 19);           // 43119
        put("vhtalent:lucky_momentum", TALENT_BASE_ID + 20);    // 43120
        put("vhtalent:frenzy", TALENT_BASE_ID + 21);            // 43121
        put("vhtalent:lightning_finesse", TALENT_BASE_ID + 22); // 43122
        put("vhtalent:lightning_mastery", TALENT_BASE_ID + 23); // 43123
        put("vhtalent:prime_amplification", TALENT_BASE_ID + 24); // 43124
        put("vhtalent:hunter's_instinct", TALENT_BASE_ID + 25);   // 43125
        put("vhtalent:purist", TALENT_BASE_ID + 26);            // 43126
        put("vhtalent:farmer_twerker", TALENT_BASE_ID + 27);    // 43127
        put("vhtalent:bountiful_harvest", TALENT_BASE_ID + 28); // 43128
        put("vhtalent:treasure_seeker", TALENT_BASE_ID + 29);   // 43129
        put("vhtalent:horde_mastery", TALENT_BASE_ID + 30);     // 43130
        put("vhtalent:champion_mastery", TALENT_BASE_ID + 31);  // 43131
        put("vhtalent:assassin_mastery", TALENT_BASE_ID + 32);  // 43132
        put("vhtalent:dungeon_mastery", TALENT_BASE_ID + 33);   // 43133
        put("vhtalent:fatal_strike", TALENT_BASE_ID + 34);      // 43134
        put("vhtalent:mana_steal", TALENT_BASE_ID + 35);        // 43135
        put("vhtalent:life_leech", TALENT_BASE_ID + 36);        // 43136
        put("vhtalent:cleave", TALENT_BASE_ID + 37);            // 43137
        put("vhtalent:throw_power", TALENT_BASE_ID + 38);       // 43138
        put("vhtalent:damage", TALENT_BASE_ID + 39);            // 43139
        put("vhtalent:conduct", TALENT_BASE_ID + 40);           // 43140
        put("vhtalent:ethereal", TALENT_BASE_ID + 41);          // 43141
    }};

    // ========== MOD LOCATIONS HASHMAP (43200-43256) ==========
    // Based on locations.py order

    private static final HashMap<String, Long> MOD_LOCATIONS = new HashMap<>() {{
        // From locations.py - 57 mods total
        put("vhmod:colossal_chests", MOD_BASE_ID + 0);          // 43200
        put("vhmod:simple_storage_network", MOD_BASE_ID + 1);   // 43201
        put("vhmod:drawers", MOD_BASE_ID + 2);                  // 43202
        put("vhmod:mekanism_qio", MOD_BASE_ID + 3);             // 43203
        put("vhmod:refined_storage", MOD_BASE_ID + 4);          // 43204
        put("vhmod:applied_energistics", MOD_BASE_ID + 5);      // 43205
        put("vhmod:stack_upgrading", MOD_BASE_ID + 6);          // 43206
        put("vhmod:auto_refill", MOD_BASE_ID + 7);              // 43207
        put("vhmod:auto_feeding", MOD_BASE_ID + 8);             // 43208
        put("vhmod:double_pouches", MOD_BASE_ID + 9);           // 43209
        put("vhmod:belts", MOD_BASE_ID + 10);                   // 43210
        put("vhmod:backpacks", MOD_BASE_ID + 11);               // 43211
        put("vhmod:big_backpacks", MOD_BASE_ID + 12);           // 43212
        put("vhmod:soul_harvester", MOD_BASE_ID + 13);          // 43213
        put("vhmod:junk_management", MOD_BASE_ID + 14);         // 43214
        put("vhmod:iron_generators", MOD_BASE_ID + 15);         // 43215
        put("vhmod:powah", MOD_BASE_ID + 16);                   // 43216
        put("vhmod:flux_networks", MOD_BASE_ID + 17);           // 43217
        put("vhmod:thermal_dynamos", MOD_BASE_ID + 18);         // 43218
        put("vhmod:mekanism_generators", MOD_BASE_ID + 19);     // 43219
        put("vhmod:botania_flux_field", MOD_BASE_ID + 20);      // 43220
        put("vhmod:building_gadgets", MOD_BASE_ID + 21);        // 43221
        put("vhmod:weirding_gadgets", MOD_BASE_ID + 22);        // 43222
        put("vhmod:mining_gadgets", MOD_BASE_ID + 23);          // 43223
        put("vhmod:laser_bridges", MOD_BASE_ID + 24);           // 43224
        put("vhmod:digital_miner", MOD_BASE_ID + 25);           // 43225
        put("vhmod:entangled", MOD_BASE_ID + 26);               // 43226
        put("vhmod:botania", MOD_BASE_ID + 27);                 // 43227
        put("vhmod:mekanism", MOD_BASE_ID + 28);                // 43228
        put("vhmod:thermal_expansion", MOD_BASE_ID + 29);       // 43229
        put("vhmod:create", MOD_BASE_ID + 30);                  // 43230
        put("vhmod:waystones", MOD_BASE_ID + 31);               // 43231
        put("vhmod:torchmaster", MOD_BASE_ID + 32);             // 43232
        put("vhmod:trashcans", MOD_BASE_ID + 33);               // 43233
        put("vhmod:elevators", MOD_BASE_ID + 34);               // 43234
        put("vhmod:altar_automation", MOD_BASE_ID + 35);        // 43235
        put("vhmod:xnet", MOD_BASE_ID + 36);                    // 43236
        put("vhmod:modular_routers", MOD_BASE_ID + 37);         // 43237
        put("vhmod:pipez", MOD_BASE_ID + 38);                   // 43238
        put("vhmod:iron_furnaces", MOD_BASE_ID + 39);           // 43239
        put("vhmod:vault_filters", MOD_BASE_ID + 40);           // 43240
        put("vhmod:dark_utilities", MOD_BASE_ID + 41);          // 43241
        put("vhmod:automatic_genius", MOD_BASE_ID + 42);        // 43242
        put("vhmod:easy_villagers", MOD_BASE_ID + 43);          // 43243
        put("vhmod:easy_piglins", MOD_BASE_ID + 44);            // 43244
        put("vhmod:botany_pots", MOD_BASE_ID + 45);             // 43245
        put("vhmod:snad", MOD_BASE_ID + 46);                    // 43246
        put("vhmod:cagerium", MOD_BASE_ID + 47);                // 43247
        put("vhmod:mob_spawners", MOD_BASE_ID + 48);            // 43248
        put("vhmod:phytogenic_insulator", MOD_BASE_ID + 49);    // 43249
        put("vhmod:potions", MOD_BASE_ID + 50);                 // 43250
        put("vhmod:mixtures", MOD_BASE_ID + 51);                // 43251
        put("vhmod:brews", MOD_BASE_ID + 52);                   // 43252
        put("vhmod:vault_compass", MOD_BASE_ID + 53);           // 43253
        put("vhmod:map_markers", MOD_BASE_ID + 54);             // 43254
        put("vhmod:vault_map", MOD_BASE_ID + 55);               // 43255
        put("vhmod:vault_decks", MOD_BASE_ID + 56);             // 43256
    }};

    // ========== EXPERTISE LOCATIONS HASHMAP (43300-43315) ==========
    // Based on locations.py - 16 expertises total

    private static final HashMap<String, Long> EXPERTISE_LOCATIONS = new HashMap<>() {{
        put("vhexpertise:lucky_altar", EXPERTISE_BASE_ID + 0);          // 43300
        put("vhexpertise:fortuitous_finesse", EXPERTISE_BASE_ID + 1);   // 43301
        put("vhexpertise:fortunate", EXPERTISE_BASE_ID + 2);            // 43302
        put("vhexpertise:experienced", EXPERTISE_BASE_ID + 3);          // 43303
        put("vhexpertise:infuser", EXPERTISE_BASE_ID + 4);              // 43304
        put("vhexpertise:crystalmancer", EXPERTISE_BASE_ID + 5);        // 43305
        put("vhexpertise:trinketer", EXPERTISE_BASE_ID + 6);            // 43306
        put("vhexpertise:divine", EXPERTISE_BASE_ID + 7);               // 43307
        put("vhexpertise:unbreakable", EXPERTISE_BASE_ID + 8);          // 43308
        put("vhexpertise:marketer", EXPERTISE_BASE_ID + 9);             // 43309
        put("vhexpertise:bounty_hunter", EXPERTISE_BASE_ID + 10);       // 43310
        put("vhexpertise:angel", EXPERTISE_BASE_ID + 11);               // 43311
        put("vhexpertise:jeweler", EXPERTISE_BASE_ID + 12);             // 43312
        put("vhexpertise:artisan", EXPERTISE_BASE_ID + 13);             // 43313
        put("vhexpertise:bartering", EXPERTISE_BASE_ID + 14);           // 43314
        put("vhexpertise:companion's_loyalty", EXPERTISE_BASE_ID + 15); // 43315
    }};

    // Milestone Checks - IDs 43400-43408
    private static final HashMap<String, Long> MILESTONE_LOCATIONS = new HashMap<>() {{
        put("vhmilestone:level_10", MILESTONE_BASE_ID + 0);
        put("vhmilestone:level_25", MILESTONE_BASE_ID + 1);
        put("vhmilestone:level_50", MILESTONE_BASE_ID + 2);
        put("vhmilestone:level_75", MILESTONE_BASE_ID + 3);
        put("vhmilestone:level_100", MILESTONE_BASE_ID + 4);
        put("vhmilestone:first_vault", MILESTONE_BASE_ID + 5);
        put("vhmilestone:complete_10_vaults", MILESTONE_BASE_ID + 6);
        put("vhmilestone:complete_25_vaults", MILESTONE_BASE_ID + 7);
        put("vhmilestone:complete_50_vaults", MILESTONE_BASE_ID + 8);
    }};

    // ==================== ITEM IDs (REWARDS) ====================

    // Filler/Progressive Items
    public static final long ITEM_SKILL_POINT = 43500L;
    public static final long ITEM_EXPERTISE_POINT = 43501L;
    public static final long ITEM_KNOWLEDGE_STAR = 43502L;
    public static final long ITEM_VAULT_BRONZE = 43503L;
    public static final long ITEM_VAULT_SILVER = 43504L;
    public static final long ITEM_VAULT_GOLD = 43505L;

    // VH Gamerule Changes - XP Scaling
    public static final long ITEM_XP_NORMAL = 43510L;
    public static final long ITEM_XP_DOUBLE = 43511L;
    public static final long ITEM_XP_TRIPLE = 43512L;

    // VH Gamerule Changes - Loot Scaling
    public static final long ITEM_LOOT_NORMAL = 43520L;
    public static final long ITEM_LOOT_PLENTY = 43521L;
    public static final long ITEM_LOOT_EXTREME = 43522L;

    // ==================== DYNAMIC REGISTRATION ====================

    private static long nextTalentId = TALENT_BASE_ID + 45; // Start after defined talents
    private static long nextExpertiseId = EXPERTISE_BASE_ID;
    private static long nextModId = MOD_BASE_ID + 57; // Start after defined mods

    /**
     * Dynamically register a new talent if it doesn't exist
     */
    public static synchronized Long registerTalent(String talentName) {
        if (!TALENT_LOCATIONS.containsKey(talentName)) {
            Long newId = nextTalentId++;
            TALENT_LOCATIONS.put(talentName, newId);
            return newId;
        }
        return TALENT_LOCATIONS.get(talentName);
    }

    /**
     * Dynamically register a new expertise if it doesn't exist
     */
    public static synchronized Long registerExpertise(String expertiseName) {
        if (!EXPERTISE_LOCATIONS.containsKey(expertiseName)) {
            Long newId = nextExpertiseId++;
            EXPERTISE_LOCATIONS.put(expertiseName, newId);
            return newId;
        }
        return EXPERTISE_LOCATIONS.get(expertiseName);
    }

    /**
     * Dynamically register a new mod if it doesn't exist
     */
    public static synchronized Long registerMod(String modName) {
        if (!MOD_LOCATIONS.containsKey(modName)) {
            Long newId = nextModId++;
            MOD_LOCATIONS.put(modName, newId);
            return newId;
        }
        return MOD_LOCATIONS.get(modName);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get all location IDs (for registering with AP)
     */
    public static Map<String, Long> getAllLocations() {
        Map<String, Long> all = new HashMap<>();
        all.putAll(SKILL_LOCATIONS);
        all.putAll(TALENT_LOCATIONS);
        all.putAll(MOD_LOCATIONS);
        all.putAll(EXPERTISE_LOCATIONS);
        all.putAll(MILESTONE_LOCATIONS);
        all.putAll(QUEST_LOCATIONS);
        all.putAll(TRINKET_LOCATIONS);
        return all;
    }

    public static String getLocationNameById(long locationId) {
        // Check each location map
        for (Map.Entry<String, Long> entry : SKILL_LOCATIONS.entrySet()) {
            if (entry.getValue() == locationId) return entry.getKey();
        }
        for (Map.Entry<String, Long> entry : TALENT_LOCATIONS.entrySet()) {
            if (entry.getValue() == locationId) return entry.getKey();
        }
        for (Map.Entry<String, Long> entry : MOD_LOCATIONS.entrySet()) {
            if (entry.getValue() == locationId) return entry.getKey();
        }
        for (Map.Entry<String, Long> entry : EXPERTISE_LOCATIONS.entrySet()) {
            if (entry.getValue() == locationId) return entry.getKey();
        }
        for (Map.Entry<String, Long> entry : MILESTONE_LOCATIONS.entrySet()) {
            if (entry.getValue() == locationId) return entry.getKey();
        }
        for (Map.Entry<String, Long> entry : QUEST_LOCATIONS.entrySet()) {
            if (entry.getValue() == locationId) return entry.getKey();
        }
        for (Map.Entry<String, Long> entry : TRINKET_LOCATIONS.entrySet()) {
            if (entry.getValue() == locationId) return entry.getKey();
        }
        return null;
    }

    /**
     * Get location ID for a skill
     */
    public static Long getSkillLocationId(String skillName) {
        return SKILL_LOCATIONS.getOrDefault(skillName, 0L);
    }

    /**
     * Get location ID for a talent (with dynamic registration)
     */
    public static Long getTalentLocationId(String talentName) {
        Long id = TALENT_LOCATIONS.get(talentName);
        if (id == null) {
            // Dynamically register new talents
            id = registerTalent(talentName);
        }
        return id;
    }

    /**
     * Get location ID for an expertise (with dynamic registration)
     */
    public static Long getExpertiseLocationId(String expertiseName) {
        Long id = EXPERTISE_LOCATIONS.get(expertiseName);
        if (id == null) {
            // Dynamically register new expertises
            id = registerExpertise(expertiseName);
        }
        return id;
    }

    /**
     * Get location ID for a mod (with dynamic registration)
     */
    public static Long getModLocationId(String modName) {
        Long id = MOD_LOCATIONS.get(modName);
        if (id == null) {
            // Dynamically register new mods
            id = registerMod(modName);
        }
        return id;
    }

    // ========== TALENT MAPPINGS (43100-43141) ==========
// Use these in getTalentName() method

    private String getTalentName(long itemId) {
        String[] talents = {
                // 43100-43109
                "vhtalent:speed",              // 43100
                "vhtalent:haste",              // 43101
                "vhtalent:strength",           // 43102
                "vhtalent:intelligence",       // 43103
                "vhtalent:nucleus",            // 43104
                "vhtalent:daze",               // 43105
                "vhtalent:last_stand",         // 43106
                "vhtalent:berserking",         // 43107
                "vhtalent:sorcery",            // 43108
                "vhtalent:witchery",           // 43109

                // 43110-43119
                "vhtalent:frozen_impact",      // 43110
                "vhtalent:frostbite",          // 43111
                "vhtalent:methodical",         // 43112
                "vhtalent:depleted",           // 43113
                "vhtalent:prudent",            // 43114
                "vhtalent:stoneskin",          // 43115
                "vhtalent:blight",             // 43116
                "vhtalent:toxic_reaction",     // 43117
                "vhtalent:arcana",             // 43118
                "vhtalent:blazing",            // 43119

                // 43120-43129
                "vhtalent:lucky_momentum",     // 43120
                "vhtalent:frenzy",             // 43121
                "vhtalent:lightning_finesse",  // 43122
                "vhtalent:lightning_mastery",  // 43123
                "vhtalent:prime_amplification",// 43124
                "vhtalent:hunter's_instinct",  // 43125
                "vhtalent:purist",             // 43126
                "vhtalent:farmer_twerker",     // 43127
                "vhtalent:bountiful_harvest",  // 43128
                "vhtalent:treasure_seeker",    // 43129

                // 43130-43141
                "vhtalent:horde_mastery",      // 43130
                "vhtalent:champion_mastery",   // 43131
                "vhtalent:assassin_mastery",   // 43132
                "vhtalent:dungeon_mastery",    // 43133
                "vhtalent:fatal_strike",       // 43134
                "vhtalent:mana_steal",         // 43135
                "vhtalent:life_leech",         // 43136
                "vhtalent:cleave",             // 43137
                "vhtalent:throw_power",        // 43138
                "vhtalent:damage",             // 43139
                "vhtalent:conduct",            // 43140
                "vhtalent:ethereal"            // 43141
        };

        int index = (int)(itemId - 43100);
        return index >= 0 && index < talents.length ? talents[index] : "vhtalent:unknown";
    }

// ========== MOD MAPPINGS (43200-43256) ==========
// Use these in getModName() method

    private String getModName(long itemId) {
        String[] mods = {
                // 43200-43209
                "vhmod:colossal_chests",           // 43200
                "vhmod:simple_storage_network",    // 43201
                "vhmod:drawers",                   // 43202
                "vhmod:mekanism_qio",              // 43203
                "vhmod:refined_storage",           // 43204
                "vhmod:applied_energistics",       // 43205
                "vhmod:stack_upgrading",           // 43206
                "vhmod:auto_refill",               // 43207
                "vhmod:auto_feeding",              // 43208
                "vhmod:double_pouches",            // 43209

                // 43210-43219
                "vhmod:belts",                     // 43210
                "vhmod:backpacks",                 // 43211
                "vhmod:big_backpacks",             // 43212
                "vhmod:soul_harvester",            // 43213
                "vhmod:junk_management",           // 43214
                "vhmod:iron_generators",           // 43215
                "vhmod:powah",                     // 43216
                "vhmod:flux_networks",             // 43217
                "vhmod:thermal_dynamos",           // 43218
                "vhmod:mekanism_generators",       // 43219

                // 43220-43229
                "vhmod:botania_flux_field",        // 43220
                "vhmod:building_gadgets",          // 43221
                "vhmod:weirding_gadgets",          // 43222
                "vhmod:mining_gadgets",            // 43223
                "vhmod:laser_bridges",             // 43224
                "vhmod:digital_miner",             // 43225
                "vhmod:entangled",                 // 43226
                "vhmod:botania",                   // 43227
                "vhmod:mekanism",                  // 43228
                "vhmod:thermal_expansion",         // 43229

                // 43230-43239
                "vhmod:create",                    // 43230
                "vhmod:waystones",                 // 43231
                "vhmod:torchmaster",               // 43232
                "vhmod:trashcans",                 // 43233
                "vhmod:elevators",                 // 43234
                "vhmod:altar_automation",          // 43235
                "vhmod:xnet",                      // 43236
                "vhmod:modular_routers",           // 43237
                "vhmod:pipez",                     // 43238
                "vhmod:iron_furnaces",             // 43239

                // 43240-43249
                "vhmod:vault_filters",             // 43240
                "vhmod:dark_utilities",            // 43241
                "vhmod:automatic_genius",          // 43242
                "vhmod:easy_villagers",            // 43243
                "vhmod:easy_piglins",              // 43244
                "vhmod:botany_pots",               // 43245
                "vhmod:snad",                      // 43246
                "vhmod:cagerium",                  // 43247
                "vhmod:mob_spawners",              // 43248
                "vhmod:phytogenic_insulator",      // 43249

                // 43250-43256
                "vhmod:potions",                   // 43250
                "vhmod:mixtures",                  // 43251
                "vhmod:brews",                     // 43252
                "vhmod:vault_compass",             // 43253
                "vhmod:map_markers",               // 43254
                "vhmod:vault_map",                 // 43255
                "vhmod:vault_decks"                // 43256
        };

        int index = (int)(itemId - 43200);
        return index >= 0 && index < mods.length ? mods[index] : "vhmod:unknown";
    }

// ========== EXPERTISE MAPPINGS (43300-43315) ==========
// Use these in getExpertiseName() method

    private String getExpertiseName(long itemId) {
        String[] expertises = {
                "vhexpertise:lucky_altar",           // 43300
                "vhexpertise:fortuitous_finesse",    // 43301
                "vhexpertise:fortunate",             // 43302
                "vhexpertise:experienced",           // 43303
                "vhexpertise:infuser",               // 43304
                "vhexpertise:crystalmancer",         // 43305
                "vhexpertise:trinketer",             // 43306
                "vhexpertise:divine",                // 43307
                "vhexpertise:unbreakable",           // 43308
                "vhexpertise:marketer",              // 43309
                "vhexpertise:bounty_hunter",         // 43310
                "vhexpertise:angel",                 // 43311
                "vhexpertise:jeweler",               // 43312
                "vhexpertise:artisan",               // 43313
                "vhexpertise:bartering",             // 43314
                "vhexpertise:companion's_loyalty"    // 43315
        };

        int index = (int)(itemId - 43300);
        return index >= 0 && index < expertises.length ? expertises[index] : "vhexpertise:unknown";
    }

    /**
     * Get location ID for a milestone
     */
    public static Long getMilestoneLocationId(String milestoneName) {
        return MILESTONE_LOCATIONS.getOrDefault(milestoneName, 0L);
    }

    /**
     * Get XP gamerule string for a given item ID
     */
    public static String getXPGamerule(long itemId) {
        if (itemId == ITEM_XP_NORMAL) return "NORMAL";
        if (itemId == ITEM_XP_DOUBLE) return "DOUBLE";
        if (itemId == ITEM_XP_TRIPLE) return "TRIPLE";
        return "NORMAL";
    }

    /**
     * Get Loot gamerule string for a given item ID
     */
    public static String getLootGamerule(long itemId) {
        if (itemId == ITEM_LOOT_NORMAL) return "NORMAL";
        if (itemId == ITEM_LOOT_PLENTY) return "PLENTY";
        if (itemId == ITEM_LOOT_EXTREME) return "EXTREME";
        return "NORMAL";
    }

    /**
     * Check if an item ID is an XP gamerule change
     */
    public static boolean isXPGamerule(long itemId) {
        return itemId == ITEM_XP_NORMAL || itemId == ITEM_XP_DOUBLE || itemId == ITEM_XP_TRIPLE;
    }

    /**
     * Check if an item ID is a Loot gamerule change
     */
    public static boolean isLootGamerule(long itemId) {
        return itemId == ITEM_LOOT_NORMAL || itemId == ITEM_LOOT_PLENTY || itemId == ITEM_LOOT_EXTREME;
    }

    /**
     * Get location ID for any location name
     */
    public static Long getLocationId(String locationName) {
        // Check each map
        if (SKILL_LOCATIONS.containsKey(locationName)) {
            return SKILL_LOCATIONS.get(locationName);
        }
        if (TALENT_LOCATIONS.containsKey(locationName)) {
            return TALENT_LOCATIONS.get(locationName);
        }
        if (MOD_LOCATIONS.containsKey(locationName)) {
            return MOD_LOCATIONS.get(locationName);
        }
        if (EXPERTISE_LOCATIONS.containsKey(locationName)) {
            return EXPERTISE_LOCATIONS.get(locationName);
        }
        if (MILESTONE_LOCATIONS.containsKey(locationName)) {
            return MILESTONE_LOCATIONS.get(locationName);
        }
        if (QUEST_LOCATIONS.containsKey(locationName)) {
            return QUEST_LOCATIONS.get(locationName);
        }
        if (TRINKET_LOCATIONS.containsKey(locationName)) {
            return TRINKET_LOCATIONS.get(locationName);
        }
        return null;
    }
    /**
     * Get total number of locations
     */
    public static int getTotalLocationCount() {
        return SKILL_LOCATIONS.size()
                + TALENT_LOCATIONS.size()
                + EXPERTISE_LOCATIONS.size()
                + MOD_LOCATIONS.size()
                + MILESTONE_LOCATIONS.size();
    }
    public static final Map<String, Long> QUEST_LOCATIONS = new HashMap<String, Long>() {{
        for (int i = 0; i < 99; i++) {
            put("Quest Completion " + (i + 1), 43500L + i);
        }
    }};
    public static final Map<String, Long> TRINKET_LOCATIONS = new HashMap<>() {{
        put("Trinket: Carapace", 43600L);
        put("Trinket: Portable Cat", 43601L);
        put("Trinket: Slimey", 43602L);
        put("Trinket: Prismatic Feather", 43603L);
        put("Trinket: Ender Anchor", 43604L);
        put("Trinket: Elvish Air", 43605L);
        put("Trinket: Gluttony Pendant", 43606L);
        put("Trinket: Golden Burger", 43607L);
        put("Trinket: The Frog", 43608L);
        put("Trinket: Treasure Goggles", 43609L);
        put("Trinket: Velara's Petal", 43610L);
        put("Trinket: Wendarr's Hourglass", 43611L);
        put("Trinket: Stone of Jordan", 43612L);
        put("Trinket: Wings", 43613L);
        put("Trinket: Chromatic Powder", 43614L);
        put("Trinket: Clover", 43615L);
        put("Trinket: Cufflings", 43616L);
        put("Trinket: Spellbook", 43617L);
        put("Trinket: Crystal Ball", 43618L);
        put("Trinket: Giant's Heart", 43619L);
        put("Trinket: Idona's Pendant", 43620L);
        put("Trinket: Picture of a Lucky Goose", 43621L);
        put("Trinket: Phylactery", 43622L);
        put("Trinket: Tenos' Necklace", 43623L);
    }};

}