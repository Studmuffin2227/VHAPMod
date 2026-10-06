package studmuffin.vhapmod;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines all Vault Hunters locations (checks) and items for Archipelago integration.
 *
 * Location ID Ranges:
 * - 43000-43028: Skills (29 total)
 * - 43100-43174: Talents (75 total)
 * - 43200-43257: Mods (58 slots)
 * - 43300-43315: Expertises (16 slots)
 * - 43400-43408: Milestone checks (levels, first vault, etc.)
 */
public class VaultHuntersData {

    // ==================== LOCATION IDs (CHECKS) ====================

    // Base IDs for each category
    public static final long SKILL_BASE_ID = 43000L;
    public static final long TALENT_BASE_ID = 43100L;
    public static final long MOD_BASE_ID = 43200L;
    public static final long EXPERTISE_BASE_ID = 43300L;
    public static final long MILESTONE_BASE_ID = 43400L;
    public static final long GOAL_BASE_ID = 43750L;
    public static final long SPECIALIZATION_BASE_ID = 43800L;

    // Skills (29 total) - IDs 43000-43028
    private static final HashMap<String, Long> SKILL_LOCATIONS = new HashMap<>() {{
        put("vhskill:heal", SKILL_BASE_ID + 0);
        put("vhskill:dash", SKILL_BASE_ID + 1);
        put("vhskill:nova", SKILL_BASE_ID + 2);
        put("vhskill:execute", SKILL_BASE_ID + 3);
        put("vhskill:ghost_walk", SKILL_BASE_ID + 4);
        put("vhskill:mega_jump", SKILL_BASE_ID + 5);
        put("vhskill:rampage", SKILL_BASE_ID + 6);
        put("vhskill:smite", SKILL_BASE_ID + 7);
        put("vhskill:empower", SKILL_BASE_ID + 8);
        put("vhskill:vein_miner", SKILL_BASE_ID + 9);
        put("vhskill:hunter", SKILL_BASE_ID + 10);
        put("vhskill:farmer", SKILL_BASE_ID + 11);
        put("vhskill:mana_shield", SKILL_BASE_ID + 12);
        put("vhskill:retribution", SKILL_BASE_ID + 13);
        put("vhskill:implode", SKILL_BASE_ID + 14);
        put("vhskill:taunt", SKILL_BASE_ID + 15);
        put("vhskill:stonefall", SKILL_BASE_ID + 16);
        put("vhskill:totem", SKILL_BASE_ID + 17);
        put("vhskill:javelin", SKILL_BASE_ID + 18);
        put("vhskill:shell", SKILL_BASE_ID + 19);
        put("vhskill:fireball", SKILL_BASE_ID + 20);
        put("vhskill:storm_arrow", SKILL_BASE_ID + 21);
        put("vhskill:battle_cry", SKILL_BASE_ID + 22);
        put("vhskill:ice_bolt", SKILL_BASE_ID + 23);
        put("vhskill:arcane", SKILL_BASE_ID + 24);
        put("vhskill:earthquake", SKILL_BASE_ID + 25);
        put("vhskill:chain_lightning", SKILL_BASE_ID + 26);
        put("vhskill:grenade", SKILL_BASE_ID + 28);
        put("vhskill:heal_group", SPECIALIZATION_BASE_ID + 0);
        put("vhskill:heal_cleanse", SPECIALIZATION_BASE_ID + 1);
        put("vhskill:dash_damage", SPECIALIZATION_BASE_ID + 2);
        put("vhskill:dash_warp", SPECIALIZATION_BASE_ID + 3);
        put("vhskill:nova_slow", SPECIALIZATION_BASE_ID + 4);
        put("vhskill:nova_dot", SPECIALIZATION_BASE_ID + 5);
        put("vhskill:ghost_walk_spirit", SPECIALIZATION_BASE_ID + 6);
        put("vhskill:mega_jump_drill", SPECIALIZATION_BASE_ID + 7);
        put("vhskill:mega_dig", SPECIALIZATION_BASE_ID + 8);
        put("vhskill:rampage_leech", SPECIALIZATION_BASE_ID + 9);
        put("vhskill:rampage_chain", SPECIALIZATION_BASE_ID + 10);
        put("vhskill:smite_archon", SPECIALIZATION_BASE_ID + 11);
        put("vhskill:smite_thunderstorm", SPECIALIZATION_BASE_ID + 12);
        put("vhskill:empower_ice_armor", SPECIALIZATION_BASE_ID + 13);
        put("vhskill:empower_slowness_aura", SPECIALIZATION_BASE_ID + 14);
        put("vhskill:vein_miner_fortune", SPECIALIZATION_BASE_ID + 15);
        put("vhskill:vein_miner_durability", SPECIALIZATION_BASE_ID + 16);
        put("vhskill:vein_miner_void", SPECIALIZATION_BASE_ID + 17);
        put("vhskill:hunter_javelin", SPECIALIZATION_BASE_ID + 18);
        put("vhskill:mana_barrier", SPECIALIZATION_BASE_ID + 19);
        put("vhskill:shield_bash", SPECIALIZATION_BASE_ID + 20);
        put("vhskill:implode_life_tap", SPECIALIZATION_BASE_ID + 21);
        put("vhskill:taunt_repel", SPECIALIZATION_BASE_ID + 22);
        put("vhskill:taunt_charm", SPECIALIZATION_BASE_ID + 23);
        put("vhskill:heros_landing", SPECIALIZATION_BASE_ID + 24);
        put("vhskill:coldsnap", SPECIALIZATION_BASE_ID + 25);
        put("vhskill:wrath_totem", SPECIALIZATION_BASE_ID + 26);
        put("vhskill:spirit_totem", SPECIALIZATION_BASE_ID + 27);
        put("vhskill:hatred_totem", SPECIALIZATION_BASE_ID + 28);
        put("vhskill:javelin_piercing", SPECIALIZATION_BASE_ID + 29);
        put("vhskill:javelin_scatter", SPECIALIZATION_BASE_ID + 30);
        put("vhskill:shell_porcupine", SPECIALIZATION_BASE_ID + 31);
        put("vhskill:shell_quill", SPECIALIZATION_BASE_ID + 32);
        put("vhskill:fireball_volley", SPECIALIZATION_BASE_ID + 33);
        put("vhskill:fireball_fireshot", SPECIALIZATION_BASE_ID + 34);
        put("vhskill:storm_arrow_blizzard", SPECIALIZATION_BASE_ID + 35);
        put("vhskill:battle_cry_spectral_cry", SPECIALIZATION_BASE_ID + 36);
        put("vhskill:battle_cry_lucky_cry", SPECIALIZATION_BASE_ID + 37);
        put("vhskill:ice_bolt_blast", SPECIALIZATION_BASE_ID + 38);
        put("vhskill:arcane_rail", SPECIALIZATION_BASE_ID + 39);
        put("vhskill:earthquake_landmine", SPECIALIZATION_BASE_ID + 40);
        put("vhskill:ball_lightning", SPECIALIZATION_BASE_ID + 41);
        put("vhskill:toxic_vial", SPECIALIZATION_BASE_ID + 42);
    }};

    private static final java.util.Set<String> SPECIALIZATION_SKILLS = java.util.Set.of(
            "vhskill:heal_group", "vhskill:heal_cleanse", "vhskill:dash_damage",
            "vhskill:dash_warp", "vhskill:nova_slow", "vhskill:nova_dot",
            "vhskill:ghost_walk_spirit", "vhskill:mega_jump_drill",
            "vhskill:mega_dig", "vhskill:rampage_leech",
            "vhskill:rampage_chain", "vhskill:smite_archon",
            "vhskill:smite_thunderstorm", "vhskill:empower_ice_armor",
            "vhskill:empower_slowness_aura", "vhskill:vein_miner_fortune",
            "vhskill:vein_miner_durability", "vhskill:vein_miner_void",
            "vhskill:hunter_javelin", "vhskill:mana_barrier",
            "vhskill:shield_bash", "vhskill:implode_life_tap",
            "vhskill:taunt_repel", "vhskill:taunt_charm", "vhskill:heros_landing",
            "vhskill:coldsnap", "vhskill:wrath_totem",
            "vhskill:spirit_totem", "vhskill:hatred_totem",
            "vhskill:javelin_piercing", "vhskill:javelin_scatter",
            "vhskill:shell_porcupine", "vhskill:shell_quill",
            "vhskill:fireball_volley", "vhskill:fireball_fireshot",
            "vhskill:storm_arrow_blizzard", "vhskill:battle_cry_spectral_cry",
            "vhskill:battle_cry_lucky_cry", "vhskill:ice_bolt_blast",
            "vhskill:arcane_rail", "vhskill:earthquake_landmine",
            "vhskill:ball_lightning", "vhskill:toxic_vial",
            "vhskill:mega_jump_break_up", "vhskill:mega_jump_break_down",
            "vhskill:javelin_sight", "vhskill:mana_shield_retribution",
            "vhskill:stonefall_snow", "vhskill:stonefall_cold",
            "vhskill:totem_player_damage", "vhskill:totem_mana_regen",
            "vhskill:totem_mob_damage", "vhskill:battle_cry_spectral_strike",
            "vhskill:battle_cry_lucky_strike", "vhskill:chain_lightning_orbs",
            "vhskill:toxic_grenade"
    );

    private static final Map<String, String> SPECIALIZATION_PARENTS = Map.ofEntries(
            Map.entry("vhskill:heal_group", "vhskill:heal"),
            Map.entry("vhskill:heal_cleanse", "vhskill:heal"),
            Map.entry("vhskill:dash_damage", "vhskill:dash"),
            Map.entry("vhskill:dash_warp", "vhskill:dash"),
            Map.entry("vhskill:nova_slow", "vhskill:nova"),
            Map.entry("vhskill:nova_dot", "vhskill:nova"),
            Map.entry("vhskill:ghost_walk_spirit", "vhskill:ghost_walk"),
            Map.entry("vhskill:mega_jump_drill", "vhskill:mega_jump"),
            Map.entry("vhskill:mega_dig", "vhskill:mega_jump"),
            Map.entry("vhskill:rampage_leech", "vhskill:rampage"),
            Map.entry("vhskill:rampage_chain", "vhskill:rampage"),
            Map.entry("vhskill:smite_archon", "vhskill:smite"),
            Map.entry("vhskill:smite_thunderstorm", "vhskill:smite"),
            Map.entry("vhskill:empower_ice_armor", "vhskill:empower"),
            Map.entry("vhskill:empower_slowness_aura", "vhskill:empower"),
            Map.entry("vhskill:vein_miner_fortune", "vhskill:vein_miner"),
            Map.entry("vhskill:vein_miner_durability", "vhskill:vein_miner"),
            Map.entry("vhskill:vein_miner_void", "vhskill:vein_miner"),
            Map.entry("vhskill:hunter_javelin", "vhskill:hunter"),
            Map.entry("vhskill:mana_barrier", "vhskill:mana_shield"),
            Map.entry("vhskill:shield_bash", "vhskill:retribution"),
            Map.entry("vhskill:mana_shield_retribution", "vhskill:retribution"),
            Map.entry("vhskill:implode_life_tap", "vhskill:implode"),
            Map.entry("vhskill:taunt_repel", "vhskill:taunt"),
            Map.entry("vhskill:taunt_charm", "vhskill:taunt"),
            Map.entry("vhskill:heros_landing", "vhskill:stonefall"),
            Map.entry("vhskill:coldsnap", "vhskill:stonefall"),
            Map.entry("vhskill:wrath_totem", "vhskill:totem"),
            Map.entry("vhskill:spirit_totem", "vhskill:totem"),
            Map.entry("vhskill:hatred_totem", "vhskill:totem"),
            Map.entry("vhskill:javelin_piercing", "vhskill:javelin"),
            Map.entry("vhskill:javelin_scatter", "vhskill:javelin"),
            Map.entry("vhskill:shell_porcupine", "vhskill:shell"),
            Map.entry("vhskill:shell_quill", "vhskill:shell"),
            Map.entry("vhskill:fireball_volley", "vhskill:fireball"),
            Map.entry("vhskill:fireball_fireshot", "vhskill:fireball"),
            Map.entry("vhskill:storm_arrow_blizzard", "vhskill:storm_arrow"),
            Map.entry("vhskill:battle_cry_spectral_cry", "vhskill:battle_cry"),
            Map.entry("vhskill:battle_cry_lucky_cry", "vhskill:battle_cry"),
            Map.entry("vhskill:ice_bolt_blast", "vhskill:ice_bolt"),
            Map.entry("vhskill:arcane_rail", "vhskill:arcane"),
            Map.entry("vhskill:earthquake_landmine", "vhskill:earthquake"),
            Map.entry("vhskill:ball_lightning", "vhskill:chain_lightning"),
            Map.entry("vhskill:toxic_vial", "vhskill:grenade")
    );

    // ========== TALENT LOCATIONS HASHMAP (43100-43174) ==========
    // Based on locations.py order

    private static final HashMap<String, Long> TALENT_LOCATIONS = new HashMap<>() {{
        put("vhtalent:intelligence", TALENT_BASE_ID + 0);
        put("vhtalent:haste", TALENT_BASE_ID + 1);
        put("vhtalent:strength", TALENT_BASE_ID + 2);
        put("vhtalent:speed", TALENT_BASE_ID + 3);
        put("vhtalent:reach", TALENT_BASE_ID + 4);
        put("vhtalent:purist", TALENT_BASE_ID + 5);
        put("vhtalent:stone_skin", TALENT_BASE_ID + 6);
        put("vhtalent:critical_strike", TALENT_BASE_ID + 7);
        put("vhtalent:fatal_strike_chance", TALENT_BASE_ID + 8);
        put("vhtalent:fatal_strike_damage", TALENT_BASE_ID + 9);
        put("vhtalent:thorns_chance", TALENT_BASE_ID + 10);
        put("vhtalent:thorns_damage", TALENT_BASE_ID + 11);
        put("vhtalent:horde_mastery", TALENT_BASE_ID + 12);
        put("vhtalent:last_stand", TALENT_BASE_ID + 16);
        put("vhtalent:berserking", TALENT_BASE_ID + 17);
        put("vhtalent:sorcery", TALENT_BASE_ID + 18);
        put("vhtalent:prime_amplification", TALENT_BASE_ID + 19);
        put("vhtalent:bountiful_harvest", TALENT_BASE_ID + 20);
        put("vhtalent:lightning_damage", TALENT_BASE_ID + 21);
        put("vhtalent:lightning_stun", TALENT_BASE_ID + 22);
        put("vhtalent:treasure_seeker", TALENT_BASE_ID + 23);
        put("vhtalent:stoneskin", TALENT_BASE_ID + 24);
        put("vhtalent:lingering_fumes", TALENT_BASE_ID + 25);
        put("vhtalent:clean_escape", TALENT_BASE_ID + 26);
        put("vhtalent:witchery", TALENT_BASE_ID + 27);
        put("vhtalent:methodical", TALENT_BASE_ID + 28);
        put("vhtalent:depleted", TALENT_BASE_ID + 29);
        put("vhtalent:fatal_strike", TALENT_BASE_ID + 30);
        put("vhtalent:fatal_strike_ii", TALENT_BASE_ID + 31);
        put("vhtalent:mana_harvest", TALENT_BASE_ID + 32);
        put("vhtalent:heart_fragments", TALENT_BASE_ID + 33);
        put("vhtalent:mana_steal", TALENT_BASE_ID + 34);
        put("vhtalent:mana_steal_ii", TALENT_BASE_ID + 35);
        put("vhtalent:life_steal", TALENT_BASE_ID + 36);
        put("vhtalent:life_steal_ii", TALENT_BASE_ID + 37);
        put("vhtalent:cleave", TALENT_BASE_ID + 38);
        put("vhtalent:explosive", TALENT_BASE_ID + 39);
        put("vhtalent:prudent", TALENT_BASE_ID + 40);
        put("vhtalent:alchemist", TALENT_BASE_ID + 41);
        put("vhtalent:blizzard", TALENT_BASE_ID + 42);
        put("vhtalent:frostbite", TALENT_BASE_ID + 43);
        put("vhtalent:daze", TALENT_BASE_ID + 44);
        put("vhtalent:nucleus", TALENT_BASE_ID + 45);
        put("vhtalent:hunters_instinct", TALENT_BASE_ID + 46);
        put("vhtalent:arcana", TALENT_BASE_ID + 47);
        put("vhtalent:blazing", TALENT_BASE_ID + 48);
        put("vhtalent:lucky_momentum", TALENT_BASE_ID + 49);
        put("vhtalent:frenzy", TALENT_BASE_ID + 50);
        put("vhtalent:voltaic_impact", TALENT_BASE_ID + 51);
        put("vhtalent:axe_specialisation", TALENT_BASE_ID + 52);
        put("vhtalent:sword_specialisation", TALENT_BASE_ID + 53);
        put("vhtalent:blood_rush", TALENT_BASE_ID + 54);
        put("vhtalent:blight", TALENT_BASE_ID + 55);
        put("vhtalent:toxic_reaction", TALENT_BASE_ID + 56);
        put("vhtalent:farmer_twerker", TALENT_BASE_ID + 60);
        put("vhtalent:might_ii", TALENT_BASE_ID + 61);
        put("vhtalent:battle_trance", TALENT_BASE_ID + 62);
        put("vhtalent:intelligence_ii", TALENT_BASE_ID + 63);
        put("vhtalent:haste_ii", TALENT_BASE_ID + 64);
        put("vhtalent:speed_ii", TALENT_BASE_ID + 65);
        put("vhtalent:prudent_ii", TALENT_BASE_ID + 66);
        put("vhtalent:ethereal", TALENT_BASE_ID + 67);
        put("vhtalent:quickening", TALENT_BASE_ID + 68);
        put("vhtalent:trap_disarm", TALENT_BASE_ID + 69);
        put("vhtalent:treasure_sense", TALENT_BASE_ID + 70);
        put("vhtalent:executioner", TALENT_BASE_ID + 71);
        put("vhtalent:assassin_mastery", TALENT_BASE_ID + 72);
        put("vhtalent:dungeon_mastery", TALENT_BASE_ID + 73);
        put("vhtalent:champion_mastery", TALENT_BASE_ID + 74);
    }};

    // ========== MOD LOCATIONS HASHMAP (43200-43257) ==========
    // Based on locations.py order

    private static final HashMap<String, Long> MOD_LOCATIONS = new HashMap<>() {{
        // From locations.py - 58 mods total
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
        put("vhmod:augment_station", MOD_BASE_ID + 57);         // 43257
    }};

    // ========== EXPERTISE LOCATIONS HASHMAP (43300-43315) ==========
    // Based on locations.py - 16 expertises total

    private static final HashMap<String, Long> EXPERTISE_LOCATIONS = new HashMap<>() {{
        put("vhexpertise:lucky_altar", EXPERTISE_BASE_ID + 0);          // 43300
        put("vhexpertise:fortuitous_finesse", EXPERTISE_BASE_ID + 1);   // 43301
        put("vhexpertise:fortunate", EXPERTISE_BASE_ID + 2);            // 43302
        put("vhexpertise:experienced", EXPERTISE_BASE_ID + 3);          // 43303
        put("vhexpertise:infuser", EXPERTISE_BASE_ID + 4);              // 43304
        put("vhexpertise:mystic", EXPERTISE_BASE_ID + 5);               // 43305 - Crystalmancer
        put("vhexpertise:trinketer", EXPERTISE_BASE_ID + 6);            // 43306
        put("vhexpertise:divine", EXPERTISE_BASE_ID + 7);               // 43307
        put("vhexpertise:unbreakable", EXPERTISE_BASE_ID + 8);          // 43308
        put("vhexpertise:marketer", EXPERTISE_BASE_ID + 9);             // 43309
        put("vhexpertise:bounty_hunter", EXPERTISE_BASE_ID + 10);       // 43310
        put("vhexpertise:angel", EXPERTISE_BASE_ID + 11);               // 43311
        put("vhexpertise:jeweler", EXPERTISE_BASE_ID + 12);             // 43312
        put("vhexpertise:artisan", EXPERTISE_BASE_ID + 13);             // 43313
        put("vhexpertise:bartering", EXPERTISE_BASE_ID + 14);           // 43314
        put("vhexpertise:companion_loyalty", EXPERTISE_BASE_ID + 15); // 43315 - Companion's Loyalty
    }};

    // Milestone Checks - IDs 43400-43408
    private static final HashMap<String, Long> MILESTONE_LOCATIONS = new HashMap<>() {{
        put("vhmilestone:level_10", MILESTONE_BASE_ID + 0);
        put("vhmilestone:level_20", MILESTONE_BASE_ID + 1);
        put("vhmilestone:level_40", MILESTONE_BASE_ID + 2);
        put("vhmilestone:level_50", MILESTONE_BASE_ID + 3);
        put("vhmilestone:level_100", MILESTONE_BASE_ID + 4);
        put("vhmilestone:first_vault", MILESTONE_BASE_ID + 5);
        put("vhmilestone:complete_10_vaults", MILESTONE_BASE_ID + 6);
        put("vhmilestone:complete_25_vaults", MILESTONE_BASE_ID + 7);
        put("vhmilestone:complete_50_vaults", MILESTONE_BASE_ID + 8);
    }};

    private static final HashMap<String, Long> GOAL_LOCATIONS = new HashMap<>() {{
        put("Goal: Complete Artifact Puzzle", GOAL_BASE_ID);
        put("Goal: Defeat The Herald", GOAL_BASE_ID + 1);
    }};

    // ==================== ITEM IDs (REWARDS) ====================

    // Filler/Progressive Items
    public static final long ITEM_SKILL_POINT = 43707L;
    public static final long ITEM_EXPERTISE_POINT = 43708L;
    public static final long ITEM_KNOWLEDGE_STAR = 43709L;
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

    public static final long ITEM_PROGRESSIVE_LEVEL_CAP = 43700L;
    public static final long ITEM_PROGRESSIVE_XP_SCALING = 43701L;
    public static final long ITEM_PROGRESSIVE_LOOT_SCALING = 43702L;
    public static final long ITEM_PROGRESSIVE_GEAR_RARITY = 43703L;
    public static final long ITEM_PROGRESSIVE_ARTIFACT = 43704L;
    public static final long ITEM_PROGRESSIVE_VAULT_TIME = 43705L;
    public static final long ITEM_PROGRESSIVE_CRYSTAL_COST_REDUCTION = 43706L;
    public static final long ITEM_PROGRESSIVE_BOUNTY = 43710L;
    public static final long ITEM_PROGRESSIVE_BACKPACK_MOD = MOD_BASE_ID + 9;
    public static final long ITEM_TRAP_MANA_LOSS = 33800L;
    public static final long ITEM_TRAP_LOCKED_OUT = 33802L;
    public static final long ITEM_TRAP_BUFF_ENEMIES = 33803L;
    public static final long ITEM_TRAP_NERF_PLAYER = 33804L;
    public static final long ITEM_TRAP_SILENCED = 33805L;
    public static final long ITEM_TRAP_BOTTLE_DRAIN = 33806L;

    // ==================== DYNAMIC REGISTRATION ====================

    private static long nextTalentId = TALENT_BASE_ID + 75; // Start after defined talents
    private static long nextExpertiseId = EXPERTISE_BASE_ID + 16;
    private static long nextModId = MOD_BASE_ID + 58; // Start after defined mods

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
        all.putAll(GOAL_LOCATIONS);
        all.putAll(QUEST_LOCATIONS);
        all.putAll(BOUNTY_LOCATIONS);
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
        for (Map.Entry<String, Long> entry : GOAL_LOCATIONS.entrySet()) {
            if (entry.getValue() == locationId) return entry.getKey();
        }
        for (Map.Entry<String, Long> entry : QUEST_LOCATIONS.entrySet()) {
            if (entry.getValue() == locationId) return entry.getKey();
        }
        for (Map.Entry<String, Long> entry : BOUNTY_LOCATIONS.entrySet()) {
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
        if (SPECIALIZATION_SKILLS.contains(skillName)) {
            return 0L;
        }
        return SKILL_LOCATIONS.getOrDefault(skillName, 0L);
    }

    public static boolean isSpecializationSkill(String skillName) {
        return SPECIALIZATION_SKILLS.contains(skillName);
    }

    public static String getSpecializationParent(String specializationName) {
        return SPECIALIZATION_PARENTS.get(specializationName);
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

    // ========== TALENT MAPPINGS (43100-43174) ==========
// Use these in getTalentName() method

    private String getTalentName(long itemId) {
        String[] talents = {
                "vhtalent:intelligence", "vhtalent:haste", "vhtalent:strength",
                "vhtalent:speed", "vhtalent:reach", "vhtalent:purist",
                "vhtalent:stone_skin", "vhtalent:critical_strike",
                "vhtalent:fatal_strike_chance", "vhtalent:fatal_strike_damage",
                "vhtalent:thorns_chance", "vhtalent:thorns_damage",
                "vhtalent:horde_mastery", "vhtalent:unknown",
                "vhtalent:unknown", "vhtalent:unknown",
                "vhtalent:last_stand", "vhtalent:berserking",
                "vhtalent:sorcery", "vhtalent:prime_amplification",
                "vhtalent:bountiful_harvest", "vhtalent:lightning_damage",
                "vhtalent:lightning_stun", "vhtalent:treasure_seeker",
                "vhtalent:stoneskin", "vhtalent:lingering_fumes",
                "vhtalent:clean_escape", "vhtalent:witchery",
                "vhtalent:methodical", "vhtalent:depleted",
                "vhtalent:fatal_strike", "vhtalent:fatal_strike_ii",
                "vhtalent:mana_harvest", "vhtalent:heart_fragments",
                "vhtalent:mana_steal", "vhtalent:mana_steal_ii",
                "vhtalent:life_steal", "vhtalent:life_steal_ii",
                "vhtalent:cleave", "vhtalent:explosive", "vhtalent:prudent",
                "vhtalent:alchemist", "vhtalent:blizzard",
                "vhtalent:frostbite", "vhtalent:daze", "vhtalent:nucleus",
                "vhtalent:hunters_instinct", "vhtalent:arcana",
                "vhtalent:blazing", "vhtalent:lucky_momentum",
                "vhtalent:frenzy", "vhtalent:voltaic_impact",
                "vhtalent:axe_specialisation", "vhtalent:sword_specialisation",
                "vhtalent:blood_rush", "vhtalent:blight",
                "vhtalent:toxic_reaction", "vhtalent:unknown",
                "vhtalent:unknown", "vhtalent:unknown",
                "vhtalent:farmer_twerker", "vhtalent:might_ii",
                "vhtalent:battle_trance", "vhtalent:intelligence_ii",
                "vhtalent:haste_ii", "vhtalent:speed_ii",
                "vhtalent:prudent_ii", "vhtalent:ethereal",
                "vhtalent:quickening", "vhtalent:trap_disarm",
                "vhtalent:treasure_sense", "vhtalent:executioner",
                "vhtalent:assassin_mastery", "vhtalent:dungeon_mastery",
                "vhtalent:champion_mastery"
        };

        int index = (int)(itemId - 43100);
        return index >= 0 && index < talents.length ? talents[index] : "vhtalent:unknown";
    }

// ========== MOD MAPPINGS (43200-43257) ==========
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

                // 43250-43257
                "vhmod:potions",                   // 43250
                "vhmod:mixtures",                  // 43251
                "vhmod:brews",                     // 43252
                "vhmod:vault_compass",             // 43253
                "vhmod:map_markers",               // 43254
                "vhmod:vault_map",                 // 43255
                "vhmod:vault_decks",               // 43256
                "vhmod:augment_station"            // 43257
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
                "vhexpertise:mystic",                // 43305 - Crystalmancer
                "vhexpertise:trinketer",             // 43306
                "vhexpertise:divine",                // 43307
                "vhexpertise:unbreakable",           // 43308
                "vhexpertise:marketer",              // 43309
                "vhexpertise:bounty_hunter",         // 43310
                "vhexpertise:angel",                 // 43311
                "vhexpertise:jeweler",               // 43312
                "vhexpertise:artisan",               // 43313
                "vhexpertise:bartering",             // 43314
                "vhexpertise:companion_loyalty"     // 43315 - Companion's Loyalty
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
        if (GOAL_LOCATIONS.containsKey(locationName)) {
            return GOAL_LOCATIONS.get(locationName);
        }
        if (QUEST_LOCATIONS.containsKey(locationName)) {
            return QUEST_LOCATIONS.get(locationName);
        }
        if (BOUNTY_LOCATIONS.containsKey(locationName)) {
            return BOUNTY_LOCATIONS.get(locationName);
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
        for (int i = 0; i < 76; i++) {
            put("Quest Completion " + (i + 1), 43500L + i);
        }
    }};
    public static final Map<String, Long> BOUNTY_LOCATIONS = new HashMap<String, Long>() {{
        for (int i = 0; i < 200; i++) {
            put("Bounty Completion " + (i + 1), i < 50 ? 43650L + i : 44500L + i - 50);
        }
    }};
    public static final Map<String, Long> TRINKET_LOCATIONS = new HashMap<>() {{
        put("Trinket: Carapace", 43600L);
        put("Trinket: Portable Cat", 43601L);
        put("Trinket: Cat in a Jar", 43601L);
        put("Trinket: Slimey", 43602L);
        put("Trinket: Prismatic Feather", 43603L);
        put("Trinket: Chromatic Feather", 43603L);
        put("Trinket: Ender Anchor", 43604L);
        put("Trinket: Elvish Air", 43605L);
        put("Trinket: Elven Socks", 43605L);
        put("Trinket: Gluttony Pendant", 43606L);
        put("Trinket: Golden Burger", 43607L);
        put("Trinket: The Frog", 43608L);
        put("Trinket: Healing Salve", 43608L);
        put("Trinket: Treasure Goggles", 43609L);
        put("Trinket: Night Vision Goggles", 43609L);
        put("Trinket: Velara's Petal", 43610L);
        put("Trinket: Wendarr's Hourglass", 43611L);
        put("Trinket: Stone of Jordan", 43612L);
        put("Trinket: Wings", 43613L);
        put("Trinket: Chromatic Powder", 43614L);
        put("Trinket: Clover", 43615L);
        put("Trinket: Cufflinks", 43616L);
        put("Trinket: Spellbook", 43617L);
        put("Trinket: Crystal Ball", 43618L);
        put("Trinket: Giant's Heart", 43619L);
        put("Trinket: Idona's Pendant", 43620L);
        put("Trinket: Idona's Blade", 43620L);
        put("Trinket: Picture of a Lucky Goose", 43621L);
        put("Trinket: Lucky Goose", 43621L);
        put("Trinket: Phylactery", 43622L);
        put("Trinket: Tenos' Necklace", 43623L);
        put("Trinket: Tenos Charm", 43623L);
        put("Trinket: Tenos' Charm", 43623L);
    }};

}
