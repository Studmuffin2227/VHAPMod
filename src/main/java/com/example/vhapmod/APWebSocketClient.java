package com.example.vhapmod;

import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Clean WebSocket client for Archipelago
 * Handles connection, item receiving, and location checking
 */
public class APWebSocketClient implements WebSocket.Listener {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Gson gson = new Gson();
    private final Random random = new Random();

    private WebSocket webSocket;
    private VaultHuntersManager vhManager;
    private net.minecraft.server.MinecraftServer server;

    // Connection state
    private String slotName;
    private String password;
    private String game = "Vault Hunters";
    private int team = 0;
    private int slot = 0;
    private boolean connected = false;

    // Game state
    private Set<Long> checkedLocations = new HashSet<>();
    private Set<String> processedItems = new HashSet<>();
    private int goalLevel = 100;

    public APWebSocketClient(VaultHuntersManager manager) {
        this.vhManager = manager;
    }

    public void setServer(net.minecraft.server.MinecraftServer server) {
        this.server = server;
    }

    // ========== CONNECTION ==========

    public CompletableFuture<Void> connect(String host, int port, String slotName, String password) {
        this.slotName = slotName;
        this.password = password;

        String uri = String.format("ws://%s:%d", host, port);
        LOGGER.info("Connecting to AP server: {}", uri);

        HttpClient client = HttpClient.newHttpClient();

        return client.newWebSocketBuilder()
                .buildAsync(URI.create(uri), this)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    LOGGER.info("WebSocket connected, sending Connect packet");
                    sendConnect();
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to connect: {}", ex.getMessage());
                    return null;
                });
    }

    private void sendConnect() {
        JsonObject connectPacket = new JsonObject();
        connectPacket.addProperty("cmd", "Connect");
        connectPacket.addProperty("game", game);
        connectPacket.addProperty("name", slotName);
        connectPacket.addProperty("uuid", UUID.randomUUID().toString());
        connectPacket.add("version", getAPVersion());
        connectPacket.addProperty("items_handling", 7); // 0b111 = remote items
        connectPacket.addProperty("slot_data", true); // Request slot_data
        connectPacket.addProperty("password", password != null ? password : "");
        connectPacket.add("tags", new JsonArray());

        JsonArray wrapper = new JsonArray();
        wrapper.add(connectPacket);

        webSocket.sendText(gson.toJson(wrapper), true);
        LOGGER.info("Sent Connect packet");
    }

    private JsonObject getAPVersion() {
        JsonObject version = new JsonObject();
        version.addProperty("major", 0);
        version.addProperty("minor", 5);
        version.addProperty("build", 1);
        version.addProperty("class", "Version");
        return version;
    }

    public boolean isConnected() {
        return connected && webSocket != null;
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
            connected = false;
        }
    }

    // ========== WEBSOCKET LISTENER ==========

    @Override
    public void onOpen(WebSocket webSocket) {
        LOGGER.info("WebSocket opened");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    private StringBuilder messageBuffer = new StringBuilder();

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        // Accumulate the message
        messageBuffer.append(data);

        // Only process when we have the complete message
        if (last) {
            String completeMessage = messageBuffer.toString();
            messageBuffer.setLength(0); // Clear buffer for next message

            try {
                // Parse the complete JSON
                JsonElement element = JsonParser.parseString(completeMessage);

                if (element.isJsonArray()) {
                    JsonArray packets = element.getAsJsonArray();
                    for (int i = 0; i < packets.size(); i++) {
                        try {
                            JsonObject packet = packets.get(i).getAsJsonObject();
                            handlePacket(packet);
                        } catch (Exception e) {
                            LOGGER.error("Error processing packet {}: {}", i, e.getMessage());
                        }
                    }
                } else if (element.isJsonObject()) {
                    handlePacket(element.getAsJsonObject());
                } else {
                    LOGGER.warn("Unexpected JSON type: {}", element.getClass().getSimpleName());
                }
            } catch (Exception e) {
                LOGGER.error("Error parsing complete message: {}", e.getMessage());
                LOGGER.debug("Message was: {}", completeMessage.substring(0, Math.min(500, completeMessage.length())));
            }
        }

        webSocket.request(1);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        LOGGER.error("WebSocket error: {}", error.getMessage());
        connected = false;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        LOGGER.info("WebSocket closed: {} - {}", statusCode, reason);
        connected = false;
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    // ========== PACKET HANDLING ==========

    private void handlePacket(JsonObject packet) {
        if (!packet.has("cmd")) return;

        String cmd = packet.get("cmd").getAsString();

        switch (cmd) {
            case "RoomInfo":
                handleRoomInfo(packet);
                break;
            case "Connected":
                handleConnected(packet);
                break;
            case "ReceivedItems":
                handleReceivedItems(packet);
                break;
            case "LocationInfo":
                handleLocationInfo(packet);
                break;
            case "Print":
                handlePrint(packet);
                break;
            case "PrintJSON":
                handlePrintJSON(packet);
                break;
            case "ConnectionRefused":
                handleConnectionRefused(packet);
                break;
            case "Retrieved":
            case "SetReply":
            case "Bounced":
                // These are responses to Get/Set commands - ignore for now
                LOGGER.debug("Received {}", cmd);
                break;
            default:
                LOGGER.debug("Unhandled packet: {}", cmd);
        }
    }

    private void handleRoomInfo(JsonObject packet) {
        LOGGER.info("Received RoomInfo");
    }

    private void handleConnected(JsonObject packet) {
        connected = true;

        if (packet.has("slot")) {
            slot = packet.get("slot").getAsInt();
        }

        if (packet.has("team")) {
            team = packet.get("team").getAsInt();
        }

        LOGGER.info("✓ Connected to AP as slot {} on team {}", slot, team);

        // Read YAML settings from slot_data
        if (packet.has("slot_data")) {
            JsonObject slotData = packet.getAsJsonObject("slot_data");

            if (slotData.has("vault_chest_checks")) {
                int chestChecks = slotData.get("vault_chest_checks").getAsInt();
                //APAwareLootModifier.setChestCheckCount(chestChecks);
                LOGGER.info("✓ Chest checks: {}", chestChecks);
            }

            if (slotData.has("wooden_chest_weight")) {
                float weight = slotData.get("wooden_chest_weight").getAsFloat();
                //APAwareLootModifier.setWoodenChestWeight(weight);
                LOGGER.info("✓ Wooden chest weight: {}%", (int)(weight * 100));
            }

            if (slotData.has("normal_chest_weight")) {
                float weight = slotData.get("normal_chest_weight").getAsFloat();
                //APAwareLootModifier.setNormalChestWeight(weight);
                LOGGER.info("✓ Normal chest weight: {}%", (int)(weight * 100));
            }

            if (slotData.has("goal_level")) {
                goalLevel = slotData.get("goal_level").getAsInt();
                LOGGER.info("★ GOAL: Reach level {} to win!", goalLevel);
            }
        }

        // Send sync message to all players
        if (server != null) {
            sendConnectedMessageToPlayers();
        }
    }

    private void handleReceivedItems(JsonObject packet) {
        if (!packet.has("items")) return;

        JsonArray items = packet.getAsJsonArray("items");
        LOGGER.info("=== Receiving {} items ===", items.size());

        for (int i = 0; i < items.size(); i++) {
            JsonObject itemData = items.get(i).getAsJsonObject();

            long itemId = itemData.get("item").getAsLong();
            long locationId = itemData.get("location").getAsLong();

            // Prevent duplicate processing
            String uniqueKey = itemId + ":" + locationId;
            if (processedItems.contains(uniqueKey)) {
                continue;
            }
            processedItems.add(uniqueKey);

            // Mark location as found
            //APAwareLootModifier.markCheckFound(locationId);

            // Process item
            processReceivedItem(itemId, locationId);
        }
    }

    private void processReceivedItem(long itemId, long locationId) {
        // CRITICAL: Must run on server thread to send packets and interact with player
        if (server != null) {
            server.execute(() -> {
                ServerPlayer player = getCurrentPlayer();
                if (player == null) {
                    LOGGER.warn("No player available to receive item");
                    return;
                }

                LOGGER.info("Processing item {} from location {}", itemId, locationId);

                // Skills (43000-43026)
                if (itemId >= 43000 && itemId < 43100) {
                    String skillName = getSkillName(itemId);
                    vhManager.unlockSkill(player, skillName);
                }
                // Talents (43100-43199)
                else if (itemId >= 43100 && itemId < 43200) {
                    String talentName = getTalentName(itemId);
                    vhManager.unlockTalent(player, talentName);
                }
                // Mods (43200-43299)
                else if (itemId >= 43200 && itemId < 43300) {
                    String modName = getModName(itemId);
                    vhManager.unlockMod(player, modName);
                }
                // Expertises (43300-43399)
                else if (itemId >= 43300 && itemId < 43400) {
                    String expertiseName = getExpertiseName(itemId);
                    vhManager.unlockExpertise(player, expertiseName);
                }
                // Filler items (33700-33799)
                else if (itemId >= 33700 && itemId < 33800) {
                    giveFillerItem(player, itemId);
                }
            });
        }
    }

    private void handleLocationInfo(JsonObject packet) {
        // Handle location scout info if needed
    }

    private void handlePrint(JsonObject packet) {
        if (packet.has("text")) {
            String message = packet.get("text").getAsString();
            LOGGER.info("[AP] {}", message);

            ServerPlayer player = getCurrentPlayer();
            if (player != null) {
                player.sendMessage(
                        new TextComponent("[AP] " + message).withStyle(ChatFormatting.AQUA),
                        player.getUUID()
                );
            }
        }
    }

    private void handlePrintJSON(JsonObject packet) {
        // Handle rich text messages
        handlePrint(packet);
    }

    private void handleConnectionRefused(JsonObject packet) {
        String reason = "Unknown";
        if (packet.has("errors")) {
            JsonArray errors = packet.getAsJsonArray("errors");
            if (errors.size() > 0) {
                reason = errors.get(0).getAsString();
            }
        }

        LOGGER.error("Connection refused: {}", reason);
        connected = false;
    }

    // ========== SENDING PACKETS ==========

    public void sendLocationCheck(long locationId) {
        if (!isConnected()) {
            LOGGER.warn("Cannot send location check - not connected");
            return;
        }

        checkedLocations.add(locationId);
        //APAwareLootModifier.markCheckFound(locationId);

        JsonObject packet = new JsonObject();
        packet.addProperty("cmd", "LocationChecks");

        JsonArray locations = new JsonArray();
        locations.add((int)locationId);  // Cast to int - AP expects integers!
        packet.add("locations", locations);

        JsonArray wrapper = new JsonArray();
        wrapper.add(packet);

        LOGGER.info("Sending LocationChecks packet: {}", gson.toJson(wrapper));
        webSocket.sendText(gson.toJson(wrapper), true);
        LOGGER.info("✓ Sent location check: {}", locationId);
    }

    public void checkGoalReached(ServerPlayer player) {
        int currentLevel = VHDataReader.getPlayerLevel(player);

        if (currentLevel >= goalLevel) {
            LOGGER.info("=== GOAL REACHED! Level {} ===", currentLevel);
            sendGoalCompletion(player);
        }
    }

    private void sendGoalCompletion(ServerPlayer player) {
        if (!isConnected()) return;

        JsonObject packet = new JsonObject();
        packet.addProperty("cmd", "StatusUpdate");
        packet.addProperty("status", 30); // ClientStatus.CLIENT_GOAL

        JsonArray wrapper = new JsonArray();
        wrapper.add(packet);

        webSocket.sendText(gson.toJson(wrapper), true);

        // Celebrate!
        player.sendMessage(
                new TextComponent("★★★ ARCHIPELAGO GOAL COMPLETE! ★★★")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                player.getUUID()
        );

        LOGGER.info("✓ Sent goal completion!");
    }

    // ========== ITEM MAPPING ==========

    private String getSkillName(long itemId) {
        // Map item IDs to skill names (43000-43026)
        String[] skills = {
                "vhskill:nova", "vhskill:fireball", "vhskill:javelin", "vhskill:stonefall",
                "vhskill:ice_bolt", "vhskill:implode", "vhskill:shield_bash", "vhskill:arcane",
                "vhskill:earthquake", "vhskill:lightning_strike", "vhskill:dash", "vhskill:vein_miner",
                "vhskill:ghost_walk", "vhskill:rampage", "vhskill:mega_jump", "vhskill:shell",
                "vhskill:taunt", "vhskill:heal", "vhskill:angel", "vhskill:empower",
                "vhskill:hunter", "vhskill:smite", "vhskill:storm_arrow", "vhskill:battle_cry",
                "vhskill:rejuvenation_totem", "vhskill:mana_shield", "vhskill:chaos_cube"
        };

        int index = (int)(itemId - 43000);
        return index >= 0 && index < skills.length ? skills[index] : "vhskill:unknown";
    }

    private String getTalentName(long itemId) {
        String[] talents = {
                "speed", "haste", "strength", "intelligence",
                "nucleus", "daze", "last_stand", "berserking",
                "sorcery", "witchery", "frozen_impact", "frostbite",
                "methodical", "depleted", "prudent", "stoneskin",
                "blight", "toxic_reaction", "arcana", "blazing",
                "lucky_momentum", "frenzy", "lightning_finesse",
                "lightning_mastery", "prime_amplification", "hunter's_instinct",
                "purist", "farmer_twerker", "bountiful_harvest",
                "treasure_seeker", "horde_mastery", "champion_mastery",
                "assassin_mastery", "dungeon_mastery", "fatal_strike",
                "mana_steal", "life_leech", "cleave",
                "throw_power", "damage", "conduct", "ethereal"
        };

        int index = (int)(itemId - 43100);
        return index >= 0 && index < talents.length ? talents[index] : "unknown";
    }

    private String getModName(long itemId) {
        String[] mods = {
                "colossal_chests", "simple_storage_network", "drawers",
                "mekanism_qio", "refined_storage", "applied_energistics",
                "stack_upgrading", "auto_refill", "auto_feeding",
                "double_pouches", "belts", "backpacks",
                "big_backpacks", "soul_harvester", "junk_management",
                "iron_generators", "powah", "flux_networks",
                "thermal_dynamos", "mekanism_generators", "botania_flux_field",
                "building_gadgets", "weirding_gadgets", "mining_gadgets",
                "laser_bridges", "digital_miner", "entangled",
                "botania", "mekanism", "thermal_expansion",
                "create", "waystones", "torchmaster",
                "trashcans", "elevators", "altar_automation",
                "xnet", "modular_routers", "pipez",
                "iron_furnaces", "vault_filters", "dark_utilities",
                "automatic_genius", "easy_villagers", "easy_piglins",
                "botany_pots", "snad", "cagerium",
                "mob_spawners", "phytogenic_insulator", "potions",
                "mixtures", "brews", "vault_compass",
                "map_markers", "vault_map", "vault_decks"
        };

        int index = (int)(itemId - 43200);
        return index >= 0 && index < mods.length ? mods[index] : "unknown";
    }

    private String getExpertiseName(long itemId) {
        String[] expertises = {
                "lucky_altar", "fortuitous_finesse", "fortunate",
                "experienced", "infuser", "crystalmancer",
                "trinketer", "divine", "unbreakable",
                "marketer", "bounty_hunter", "angel",
                "jeweler", "artisan", "bartering",
                "companion's_loyalty"
        };

        int index = (int)(itemId - 43300);
        return index >= 0 && index < expertises.length ? expertises[index] : "unknown";
    }

    private void giveFillerItem(ServerPlayer player, long itemId) {
        int index = (int)(itemId - 33700);

        // Map to actual VH items/commands [itemId, baseCount]
        String[][] fillers = {
                // 0: Cooked Vault Steak (8)
                {"the_vault:cooked_vault_steak", "8"},
                // 1: Chromatic Iron Ingot (24)
                {"the_vault:chromatic_iron_ingot", "24"},
                // 2: Shulker Box (1)
                {"minecraft:shulker_box", "1"},
                // 3: Bottle O' Enchanting (20)
                {"minecraft:experience_bottle", "20"},
                // 4: Emerald (50)
                {"minecraft:emerald", "50"},
                // 5: Ender Pearl (1)
                {"minecraft:ender_pearl", "1"},
                // 6: Pouch (1)
                {"sophisticatedbackpacks:backpack", "1"},
                // 7: Pickup Upgrade (1)
                {"sophisticatedbackpacks:pickup_upgrade", "1"},
                // 8: Void Upgrade (1)
                {"sophisticatedbackpacks:void_upgrade", "1"},
                // 9: Bounty Pearl (10)
                {"the_vault:bounty_pearl", "10"},
                // 10: Large Chromatic Iron Stack (32)
                {"the_vault:chromatic_iron_ingot", "32"},
                // 11: Gemstone (2)
                {"the_vault:gemstone", "2"},
                // 12: Chromatic Steel Ingot (4)
                {"the_vault:chromatic_steel_ingot", "4"},
                // 13: Small Vault Gold (1)
                {"the_vault:vault_gold", "1"},
                // 14: Vault Plating (9)
                {"the_vault:vault_plating", "9"},
                // 15: Diamond (5)
                {"minecraft:diamond", "5"},
                // 16: Large Vault Bronze Stack (81)
                {"the_vault:vault_bronze", "81"},
                // 17: Magnetite Ingot (1)
                {"the_vault:magnetite_ingot", "1"},
                // 18: Vault Alloy (1)
                {"the_vault:vault_alloy", "1"},
                // 19: Wild Focus (1)
                {"the_vault:wild_focus", "1"},
                // 20: Large Vault Plating Stack (12)
                {"the_vault:vault_plating", "12"},
                // 21: Vault Bronze Stack (32)
                {"the_vault:vault_bronze", "32"},
                // 22: Vault Scrap (1)
                {"the_vault:vault_scrap", "1"},
                // 23: Vault Gold (1)
                {"the_vault:vault_gold", "1"},
                // 24: Vault Diamond (1)
                {"the_vault:vault_diamond", "1"},
                // 25: Hamburger (1)
                {"the_vault:plain_burger", "1"},
                // 26: Seal of the Scout (1)
                {"the_vault:crystal_seal_scout", "1"},
                // 27: Silver Scrap (1)
                {"the_vault:silver_scrap", "1"},
                // 28: Soul Shard (1)
                {"the_vault:soul_shard", "1"},
                // 29: Vault Helmet (1)
                {"the_vault:helmet", "1"},
                // 30: Vault Chestplate (1)
                {"the_vault:chestplate", "1"},
                // 31: Vault Leggings (1)
                {"the_vault:leggings", "1"},
                // 32: Vault Boots (1)
                {"the_vault:boots", "1"},
                // 33: Mod Box (1)
                {"the_vault:mod_box", "1"},
                // 34: MISSING - skipped
                {"minecraft:barrier", "0"},
                // 35: Seal of the Sage (1)
                {"the_vault:crystal_seal_sage", "1"},
                // 36: Phoenix Capstone (1)
                {"the_vault:phoenix_feather", "1"},
                // 37: Catalyst Fragment (1)
                {"the_vault:vault_catalyst_fragment", "1"},
                // 38: Inscription Piece (1)
                {"the_vault:inscription_piece", "1"},
                // 39: Unidentified Artifact (1)
                {"the_vault:unidentified_artifact", "1"},
                // 40: Tiny Vault Gold (4)
                {"the_vault:vault_gold", "4"},
                // 41: Vault Trinket (1)
                {"the_vault:trinket", "1"},
                // 42: Tiny Vault Diamond (4)
                {"the_vault:vault_diamond", "4"},
                // 43: Large Vault Gold (20)
                {"the_vault:vault_gold", "20"},
                // 44: POG (1)
                {"the_vault:gem_pog", "1"},
                // 45: Netherite Ingot (1)
                {"minecraft:netherite_ingot", "1"},
                // 46: Wardrobe (1)
                {"the_vault:wardrobe", "1"},
                // 47: Huge Vault Bronze Stack (64)
                {"the_vault:vault_bronze", "64"},
                // 48: Echo Gem (1)
                {"the_vault:gem_echo", "1"},
                // 49: Small Mod Box (2)
                {"the_vault:mod_box", "2"},
                // 50: Sour Orange (1)
                {"the_vault:sour_orange", "1"},
                // 51: Bamboo (1)
                {"minecraft:bamboo", "1"},
                // 52: Unidentified Noble Charm (1)
                {"the_vault:trinket", "1"},
                // 53: Huge Vault Gold Stack (64)
                {"the_vault:vault_gold", "64"},
                // 54: Neuralizer (1)
                {"the_vault:neuralizer", "1"},
                // 55: Perfect Echo Gem (1)
                {"the_vault:perfect_echo_gem", "1"},
                // 56: Lost Bounty (1)
                {"the_vault:lost_bounty", "1"},
                // 57: Ember (1)
                {"the_vault:ember", "1"},
                // 58: Omega POG (1)
                {"the_vault:omega_pog", "1"}
        };

        if (index >= 0 && index < fillers.length) {
            String itemId_str = fillers[index][0];
            int baseCount = Integer.parseInt(fillers[index][1]);

            // Skip barrier (index 34)
            if (itemId_str.equals("minecraft:barrier")) {
                LOGGER.warn("Attempted to give filler index 34 which is undefined");
                return;
            }

            // Special handling for vault gear (needs level)
            if (itemId_str.startsWith("the_vault:helmet") ||
                    itemId_str.startsWith("the_vault:chestplate") ||
                    itemId_str.startsWith("the_vault:leggings") ||
                    itemId_str.startsWith("the_vault:boots")) {

                String gearType = itemId_str.substring(itemId_str.indexOf(':') + 1);
                giveVaultGear(player, gearType);
                return;
            }

            // Apply random multiplier (1.0x, 1.25x, 1.5x, 1.75x, or 2.0x)
            float[] multipliers = {1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
            float multiplier = multipliers[new java.util.Random().nextInt(multipliers.length)];
            int finalCount = Math.max(1, (int)Math.floor(baseCount * multiplier));

            // Execute /give command on server thread
            String command = String.format("give %s %s %d",
                    player.getName().getString(), itemId_str, finalCount);

            player.getServer().getCommands().performCommand(
                    player.getServer().createCommandSourceStack()
                            .withSuppressedOutput()
                            .withPermission(2),
                    command
            );

            LOGGER.info("Gave {} x{} ({}x multiplier) to {}",
                    itemId_str, finalCount, multiplier, player.getName().getString());

            String message = String.format("[AP] Received: %dx %s", finalCount,
                    itemId_str.substring(itemId_str.indexOf(':') + 1));

            player.sendMessage(
                    new TextComponent(message).withStyle(ChatFormatting.GRAY),
                    player.getUUID()
            );
        } else {
            LOGGER.warn("Invalid filler item index: {}", index);
        }
    }


    private void giveVaultGear(ServerPlayer player, String gearType) {
        try {
            // Get player's vault level
            int playerLevel = VHDataReader.getPlayerLevel(player);

            // Get the item based on type

            Item item;
            switch (gearType) {
                case "helmet" -> {
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:helmet"));
                }
                case "chestplate" -> {
                     item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:chestplate"));
                }
                case "leggings" -> {
                     item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:leggings"));
                }
                case "boots" -> {
                     item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:boots"));
                }
                default -> {
                    LOGGER.warn("Unknown gear type: {}", gearType);
                    //default to boots or smth idk
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:boots"));
                }
            }


            // Create ItemStack
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);

            // Try to use VH's gear generation API
            try {
                // Look for VaultGear or similar class
                Class<?> vaultGearClass = Class.forName("iskallia.vault.gear.VaultGearHelper");

                // Try to find a method like "createGear" or "generateGear"
                java.lang.reflect.Method createGear = vaultGearClass.getMethod("createGear",
                        net.minecraft.world.item.ItemStack.class, int.class);

                // Generate gear at player's level
                createGear.invoke(null, stack, playerLevel);

                LOGGER.info("Created vault {} at level {} for {}", gearType, playerLevel, player.getName().getString());

            } catch (ClassNotFoundException | NoSuchMethodException e) {
                // VH API not found, try alternative approach
                LOGGER.warn("VH gear API not found, using fallback method");

                // Fallback: Just set the level in NBT manually
                net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
                tag.putInt("vaultGearLevel", playerLevel);

                // Add basic client cache so it displays correctly
                net.minecraft.nbt.CompoundTag clientCache = new net.minecraft.nbt.CompoundTag();
                clientCache.putInt("rarity", 0); // Common rarity
                clientCache.putByte("hasModifierLEGENDARY", (byte)0);
                tag.put("clientCache", clientCache);
            }

            // Give the item to the player
            boolean added = player.addItem(stack);

            if (!added) {
                // Inventory full, drop at feet
                player.drop(stack, false);
            }

            player.sendMessage(
                    new net.minecraft.network.chat.TextComponent(
                            String.format("[AP] Received: Vault %s (Level %d)", gearType, playerLevel))
                            .withStyle(net.minecraft.ChatFormatting.GRAY),
                    player.getUUID()
            );

        } catch (Exception e) {
            LOGGER.error("Failed to give vault gear: ", e);
            // Fallback: give level 0 gear
            String command = String.format("give %s the_vault:%s 1",
                    player.getName().getString(), gearType);
            player.getServer().getCommands().performCommand(
                    player.getServer().createCommandSourceStack()
                            .withSuppressedOutput()
                            .withPermission(2),
                    command
            );
        }
    }
    // ========== UTILITY ==========

    private ServerPlayer getCurrentPlayer() {
        if (server == null) return null;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.get(0);
    }

    private void sendConnectedMessageToPlayers() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendMessage(
                    new TextComponent("[AP] Connected as " + slotName)
                            .withStyle(ChatFormatting.GREEN),
                    player.getUUID()
            );
        }
    }

}