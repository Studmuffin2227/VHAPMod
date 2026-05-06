package com.example.vhapmod;

import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

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
    private String goalType = "level";
    private final Map<Integer, String> playerNames = new HashMap<>();
    private final Map<Integer, String> playerGames = new HashMap<>();
    private final Map<String, Map<Long, String>> itemNamesByGame = new HashMap<>();
    private final Map<String, Map<Long, String>> locationNamesByGame = new HashMap<>();
    private final List<JsonObject> pendingItemMessages = new ArrayList<>();
    private boolean requestedDataPackage = false;

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
        this.connected = false;

        String uri = buildWebSocketUri(host, port);
        LOGGER.info("Connecting to AP server: {}", uri);

        HttpClient client = HttpClient.newHttpClient();

        return client.newWebSocketBuilder()
                .buildAsync(URI.create(uri), this)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    LOGGER.info("WebSocket connected, sending Connect packet");
                    sendConnect();
                })
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                    LOGGER.error("Failed to connect: {}", reason);
                    this.connected = false;
                    broadcastStatusToPlayers("[AP] Connection failed: " + reason, ChatFormatting.RED);
                    throw new CompletionException(cause);
                });
    }

    private String buildWebSocketUri(String host, int port) {
        String trimmedHost = host == null ? "" : host.trim();

        if (trimmedHost.startsWith("ws://") || trimmedHost.startsWith("wss://")) {
            if (trimmedHost.matches("^wss?://.*:\\d+.*$")) {
                return trimmedHost;
            }
            return trimmedHost + ":" + port;
        }

        String lowerHost = trimmedHost.toLowerCase(Locale.ROOT);
        boolean localHost = lowerHost.equals("localhost")
                || lowerHost.equals("127.0.0.1")
                || lowerHost.equals("0.0.0.0")
                || lowerHost.startsWith("192.168.")
                || lowerHost.startsWith("10.")
                || lowerHost.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");

        String scheme = localHost ? "ws" : "wss";
        return String.format("%s://%s:%d", scheme, trimmedHost, port);
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
        version.addProperty("minor", 6);
        version.addProperty("build", 6);
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
        // Required for java.net.http.WebSocket flow control:
        // request the first inbound message immediately, otherwise no packets are delivered.
        webSocket.request(1);
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
            case "DataPackage":
                handleDataPackage(packet);
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
        if (!packet.has("players") || !packet.get("players").isJsonArray()) {
            return;
        }

        JsonArray players = packet.getAsJsonArray("players");
        for (JsonElement element : players) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject player = element.getAsJsonObject();
            if (player.has("slot")) {
                int playerSlot = player.get("slot").getAsInt();
                String name = player.has("alias")
                        ? player.get("alias").getAsString()
                        : player.has("name") ? player.get("name").getAsString() : "Player " + playerSlot;
                playerNames.put(playerSlot, name);
                if (player.has("game")) {
                    playerGames.put(playerSlot, player.get("game").getAsString());
                }
            }
        }

        requestDataPackage();
    }

    private void handleConnected(JsonObject packet) {
        connected = true;

        if (packet.has("slot")) {
            slot = packet.get("slot").getAsInt();
            playerNames.putIfAbsent(slot, slotName);
            playerGames.putIfAbsent(slot, game);
        }

        if (packet.has("team")) {
            team = packet.get("team").getAsInt();
        }

        LOGGER.info("Connected to AP as slot {} on team {}", slot, team);

        // Read YAML settings from slot_data
        if (packet.has("slot_data")) {
            JsonObject slotData = packet.getAsJsonObject("slot_data");

            if (slotData.has("goal_level")) {
                goalLevel = slotData.get("goal_level").getAsInt();
                LOGGER.info("Goal level: {}", goalLevel);
                vhManager.setGoalLevel(goalLevel);
            }

            if (slotData.has("goal_type")) {
                goalType = slotData.get("goal_type").getAsString();
                LOGGER.info("Goal type: {}", goalType);
            }

            int questChecks = slotData.has("quest_check_count")
                    ? slotData.get("quest_check_count").getAsInt()
                    : getQuestChecksForGoalLevel(goalLevel);
            vhManager.setTotalQuestChecks(questChecks);
            LOGGER.info("Quest checks: {}", questChecks);

            if (slotData.has("vault_chest_checks")) {
                int chestChecks = slotData.get("vault_chest_checks").getAsInt();
                vhManager.setTotalChestChecks(chestChecks);
                LOGGER.info("Chest checks: {}", chestChecks);
            }

            if (slotData.has("wooden_chest_weight")) {
                float weight = slotData.get("wooden_chest_weight").getAsFloat();
                vhManager.setWoodenChestWeight(weight);
                LOGGER.info("Wooden chest weight: {}%", (int)(weight * 100));
            }

            if (slotData.has("normal_chest_weight")) {
                float weight = slotData.get("normal_chest_weight").getAsFloat();
                vhManager.setNormalChestWeight(weight);
                LOGGER.info("Normal chest weight: {}%", (int)(weight * 100));
            }

            if (slotData.has("lock_specializations")) {
                boolean lockSpecializations = slotData.get("lock_specializations").getAsBoolean();
                APSkillLockManager.setLockSpecializations(lockSpecializations);
            }

            if (slotData.has("max_xp_scaling")) {
                vhManager.setMaxXpScaling(slotData.get("max_xp_scaling").getAsInt());
            }

            if (slotData.has("max_loot_scaling")) {
                vhManager.setMaxLootScaling(slotData.get("max_loot_scaling").getAsInt());
            }

            if (slotData.has("start_with_vault_kit")) {
                vhManager.setStartWithVaultKit(slotData.get("start_with_vault_kit").getAsBoolean());
            }

            if (server != null) {
                vhManager.initializeWorldProgression(server);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    vhManager.grantStartingKitIfEnabled(player);
                }
            }
        }

        if (packet.has("slot_info") && packet.get("slot_info").isJsonObject()) {
            JsonObject slotInfo = packet.getAsJsonObject("slot_info");
            for (Map.Entry<String, JsonElement> entry : slotInfo.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject info = entry.getValue().getAsJsonObject();
                int playerSlot = parsePositiveInt(entry.getKey(), -1);
                if (playerSlot >= 0 && info.has("game")) {
                    playerGames.put(playerSlot, info.get("game").getAsString());
                }
                if (playerSlot >= 0 && info.has("name")) {
                    playerNames.putIfAbsent(playerSlot, info.get("name").getAsString());
                }
            }
            requestDataPackage();
        }
        requestDataPackage();

        // Send sync message to all players
        if (server != null) {
            sendConnectedMessageToPlayers();
        }
    }

    private int getQuestChecksForGoalLevel(int level) {
        if (level <= 10) return 25;
        if (level <= 20) return 37;
        if (level <= 40) return 56;
        if (level <= 50) return 58;
        return 76;
    }

    private void handleReceivedItems(JsonObject packet) {
        if (!packet.has("items")) return;
        if (server == null) {
            LOGGER.warn("Server is null, cannot process items");
            return;
        }

        // Get persistent storage
        ReceivedItemsData receivedData = ReceivedItemsData.get(server);

        JsonArray items = packet.getAsJsonArray("items");
        LOGGER.info("=== Receiving {} items ===", items.size());

        for (int i = 0; i < items.size(); i++) {
            JsonObject itemData = items.get(i).getAsJsonObject();

            long itemId = itemData.get("item").getAsLong();
            long locationId = itemData.get("location").getAsLong();
            int sendingPlayer = itemData.has("player") ? itemData.get("player").getAsInt() : -1;

            // Prevent duplicate processing
            String uniqueKey = itemId + ":" + locationId;
            if (receivedData.hasProcessed(uniqueKey)) {
                LOGGER.debug("Already processed: {}", uniqueKey);
                continue;
            }

            // Mark as processed and save
            receivedData.markProcessed(uniqueKey);

            // Process item
            processReceivedItem(itemId, locationId, getPlayerName(sendingPlayer));
        }
    }

    private void processReceivedItem(long itemId, long locationId, String sourcePlayerName) {
        // CRITICAL: Must run on server thread to send packets and interact with player
        if (server != null) {
            server.execute(() -> {
                ServerPlayer player = getCurrentPlayer();
                if (player == null) {
                    LOGGER.warn("No player available to receive item");
                    return;
                }

                LOGGER.info("Processing item {} from location {}", itemId, locationId);

                if (vhManager.applyProgressionItem(itemId, player)) {
                    sendFoundItemMessage(player, resolveItemNameForGame(itemId, game));
                    return;
                }

                String unlockName = VaultHuntersData.getLocationNameById(itemId);
                if (unlockName != null) {
                    if (unlockName.startsWith("vhskill:")) {
                        vhManager.unlockSkill(player, unlockName);
                        sendFoundUnlockMessage(player, "skill", unlockName);
                    } else if (unlockName.startsWith("vhtalent:")) {
                        vhManager.unlockTalent(player, unlockName);
                        sendFoundUnlockMessage(player, "talent", unlockName);
                    } else if (unlockName.startsWith("vhmod:")) {
                        vhManager.unlockMod(player, unlockName);
                        sendFoundUnlockMessage(player, "mod", unlockName);
                    } else if (unlockName.startsWith("vhexpertise:")) {
                        vhManager.unlockExpertise(player, unlockName);
                        sendFoundUnlockMessage(player, "expertise", unlockName);
                    }
                }
                // Filler items (33700-33799)
                else if (itemId >= 33700 && itemId < 33800) {
                    giveFillerItem(player, itemId, sourcePlayerName);
                }
            });
        }
    }

    private void handleLocationInfo(JsonObject packet) {
        // Handle location scout info if needed
    }

    private void handleDataPackage(JsonObject packet) {
        if (!packet.has("data") || !packet.get("data").isJsonObject()) {
            return;
        }
        JsonObject data = packet.getAsJsonObject("data");
        if (!data.has("games") || !data.get("games").isJsonObject()) {
            return;
        }

        JsonObject games = data.getAsJsonObject("games");
        for (Map.Entry<String, JsonElement> gameEntry : games.entrySet()) {
            if (!gameEntry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject gameData = gameEntry.getValue().getAsJsonObject();
            readNameToIdMap(gameData, "item_name_to_id", itemNamesByGame.computeIfAbsent(gameEntry.getKey(), k -> new HashMap<>()));
            readNameToIdMap(gameData, "location_name_to_id", locationNamesByGame.computeIfAbsent(gameEntry.getKey(), k -> new HashMap<>()));
        }
        LOGGER.info("Loaded AP DataPackage for {} games", games.size());
        if (!pendingItemMessages.isEmpty()) {
            List<JsonObject> pending = new ArrayList<>(pendingItemMessages);
            pendingItemMessages.clear();
            pending.forEach(this::sendFormattedItemMessage);
        }
    }

    private void requestDataPackage() {
        if (!isConnected() || webSocket == null || requestedDataPackage || playerGames.isEmpty()) {
            return;
        }

        JsonObject packet = new JsonObject();
        packet.addProperty("cmd", "GetDataPackage");
        JsonArray games = new JsonArray();
        playerGames.values().stream().filter(Objects::nonNull).distinct().forEach(games::add);
        if (games.size() == 0) {
            games.add(game);
        }
        packet.add("games", games);

        JsonArray wrapper = new JsonArray();
        wrapper.add(packet);
        webSocket.sendText(gson.toJson(wrapper), true);
        requestedDataPackage = true;
        LOGGER.info("Requested AP DataPackage for {} games", games.size());
    }

    private void readNameToIdMap(JsonObject gameData, String key, Map<Long, String> target) {
        if (!gameData.has(key) || !gameData.get(key).isJsonObject()) {
            return;
        }

        JsonObject nameToId = gameData.getAsJsonObject(key);
        for (Map.Entry<String, JsonElement> entry : nameToId.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                target.put(entry.getValue().getAsLong(), entry.getKey());
            }
        }
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
        if ("ItemSend".equals(getString(packet, "type"))) {
            sendFormattedItemMessage(packet);
            return;
        }

        if (packet.has("data") && packet.get("data").isJsonArray()) {
            StringBuilder message = new StringBuilder();
            for (JsonElement element : packet.getAsJsonArray("data")) {
                if (element.isJsonObject() && element.getAsJsonObject().has("text")) {
                    message.append(element.getAsJsonObject().get("text").getAsString());
                }
            }
            if (message.length() > 0) {
                broadcastStatusToPlayers(message.toString(), ChatFormatting.AQUA);
            }
        }
    }

    private void sendFormattedItemMessage(JsonObject packet) {
        String rawItemName = findPrintPartText(packet, "item_id", 0);
        String rawSenderName = findPrintPartText(packet, "player_id", 0);
        String rawReceiverName = findPrintPartText(packet, "player_id", 1);
        String rawLocationName = findPrintPartText(packet, "location_id", 0);
        int senderSlot = parsePositiveInt(rawSenderName, -1);
        int receiverSlot = parsePositiveInt(rawReceiverName, packet.has("receiving") ? packet.get("receiving").getAsInt() : -1);
        long itemId = parsePositiveLong(rawItemName, Long.MIN_VALUE);
        String itemName = resolveItemName(rawItemName, receiverSlot);
        String senderName = resolvePlayerName(rawSenderName);
        String receiverName = resolvePlayerName(rawReceiverName);
        String locationName = resolveLocationName(rawLocationName, senderSlot);

        int receivingSlot = packet.has("receiving") ? packet.get("receiving").getAsInt() : -1;
        boolean receivingHere = receivingSlot == slot || receiverSlot == slot || (receiverName != null && receiverName.equals(slotName));
        boolean sentByHere = senderSlot == slot || (senderName != null && senderName.equals(slotName));
        boolean selfFound = senderSlot > 0 && senderSlot == receiverSlot;
        boolean found = packet.has("found") && packet.get("found").getAsBoolean();
        if (receivingHere && isLocalVhapItem(itemId)) {
            return;
        }
        if (isUnknownDataPackageName(itemName) && !isLocalVhapItem(itemId)) {
            if (pendingItemMessages.size() < 100) {
                pendingItemMessages.add(packet.deepCopy());
            }
            requestDataPackage();
            return;
        }

        if (itemName == null || itemName.isBlank()) {
            itemName = "Item";
        }
        if (senderName == null || senderName.isBlank()) {
            senderName = "Player";
        }
        if (receiverName == null || receiverName.isBlank()) {
            receiverName = receivingHere ? slotName : "Player";
        }

        MutableComponent message;
        if (found || selfFound || (receivingHere && sentByHere)) {
            message = colored("You", ChatFormatting.YELLOW)
                    .append(colored(" found your ", ChatFormatting.WHITE))
                    .append(quoted(itemName, ChatFormatting.AQUA, false));
        } else if (receivingHere) {
            message = new TextComponent("Received ")
                    .append(quoted(itemName, ChatFormatting.AQUA, false))
                    .append(new TextComponent(" from "))
                    .append(colored(senderName, ChatFormatting.YELLOW));
        } else {
            message = colored(senderName, ChatFormatting.YELLOW)
                    .append(colored(" sent ", ChatFormatting.WHITE))
                    .append(colored(itemName, ChatFormatting.AQUA))
                    .append(colored(" to ", ChatFormatting.WHITE))
                    .append(colored(receiverName, ChatFormatting.YELLOW));
            if (locationName != null && !locationName.isBlank()) {
                message.append(colored(" (", ChatFormatting.WHITE))
                        .append(colored(locationName, ChatFormatting.GREEN))
                        .append(colored(")", ChatFormatting.WHITE));
            }
        }

        broadcastComponentToPlayers(message);
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
        broadcastStatusToPlayers("[AP] Connection refused: " + reason, ChatFormatting.RED);
    }

    // ========== SENDING PACKETS ==========

    public void sendLocationCheck(long locationId) {
        if (!isConnected()) {
            LOGGER.warn("Cannot send location check - not connected");
            return;
        }

        if (!checkedLocations.add(locationId)) {
            LOGGER.warn("Skipping duplicate location check send for {}", locationId);
            return;
        }
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
        LOGGER.info("Sent location check: {}", locationId);
    }

    public void checkGoalReached(ServerPlayer player) {
        if (!"level".equals(goalType)) {
            return;
        }
        int currentLevel = VHDataReader.getPlayerLevel(player);

        if (currentLevel >= goalLevel) {
            LOGGER.info("=== GOAL REACHED! Level {} ===", currentLevel);
            sendGoalCompletion(player);
        }
    }

    public void checkSpecialGoalReached(ServerPlayer player, String completedGoalType) {
        if (goalType.equals(completedGoalType)) {
            LOGGER.info("=== GOAL REACHED! {} ===", completedGoalType);
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
                new TextComponent("*** ARCHIPELAGO GOAL COMPLETE! ***")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                player.getUUID()
        );

        LOGGER.info("Sent goal completion");
    }

    // ========== ITEM MAPPING ==========

    private String getSkillName(long itemId) {
        // Map item IDs to skill names (43000-43026)
        String[] skills = {
                "nova", "fireball", "javelin", "stonefall",
                "ice_bolt", "implode", "shield_bash", "arcane",
                "earthquake", "lightning_strike", "dash", "vein_miner",
                "ghost_walk", "rampage", "mega_jump", "shell",
                "taunt", "heal", "angel", "empower",
                "hunter", "smite", "storm_arrow", "battle_cry",
                "rejuvenation_totem", "mana_shield", "chaos_cube"
        };

        int index = (int)(itemId - 43000);
        return index >= 0 && index < skills.length ? skills[index] : "unknown";
    }

    private String getTalentName(long itemId) {
        String[] talents = {
                "speed", "haste", "strength", "intelligence",
                "nucleus", "daze", "last_stand", "berserking",
                "sorcery", "witchery", "frozen_impact", "frostbite",
                "methodical", "depleted", "prudent", "stoneskin",
                "blight", "toxic_reaction", "arcana", "blazing",
                "lucky_momentum", "frenzy", "lightning_finesse",
                "lightning_mastery", "prime_amplification", "hunters_instinct",
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
                "companions_loyalty"
        };

        int index = (int)(itemId - 43300);
        return index >= 0 && index < expertises.length ? expertises[index] : "unknown";
    }

    private void giveFillerItem(ServerPlayer player, long itemId, String sourcePlayerName) {
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
                {"minecraft:ender_pearl", "2"},
                // 6: Pouch (1)
                {"sophisticatedbackpacks:backpack", "1"},
                // 7: Pickup Upgrade (1)
                {"sophisticatedbackpacks:backpack", "1"},
                // 8: Void Upgrade (1)
                {"sophisticatedbackpacks:backpack", "1"},
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
                // 17: Vault Alloy (1)
                {"the_vault:vault_alloy", "1"},
                // 18: Wild Focus (1)
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
                // 34: Diamond (20)
                {"minecraft:diamond", "20"},
                // 35: Seal of the Sage (1)
                {"the_vault:crystal_seal_sage", "1"},
                // 36: Phoenix Capstone (1)
                {"the_vault:phoenix_feather", "1"},
                // 37: Catalyst Fragment (1)
                {"the_vault:vault_catalyst_fragment", "64"},
                // 38: Inscription Piece (1)
                {"the_vault:inscription_piece", "32"},
                // 39: Unidentified Artifact (1)
                {"the_vault:unidentified_artifact", "1"},
                // 40: Tiny Vault Gold (4)
                {"the_vault:vault_gold", "4"},
                // 41: Vault Trinket (1)
                {"the_vault:trinket", "1"},
                // 42: Tiny Vault Diamond (4)
                {"the_vault:vault_diamond", "16"},
                // 43: Large Vault Gold (20)
                {"the_vault:vault_gold", "20"},
                // 44: POG (1)
                {"the_vault:gem_pog", "1"},
                // 45: Netherite Ingot (1)
                {"minecraft:netherite_ingot", "2"},
                // 46: Wardrobe (1)
                {"the_vault:wardrobe", "1"},
                // 47: Huge Vault Bronze Stack (64)
                {"the_vault:vault_bronze", "64"},
                // 48: Echo Gem (1)
                {"the_vault:gem_echo", "1"},
                // 49: Small Bundle Mod Box (2)
                {"the_vault:mod_box", "2"},
                // 50: Sour Orange (1)
                {"the_vault:sour_orange", "1"},
                // 51: Bamboo (1)
                {"minecraft:bamboo", "1"},
                // 52: Unidentified Trinket (1)
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

        if (itemId >= 33700L && itemId < 33800L) {
            index = random.nextInt(fillers.length);
        }

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
                giveVaultGear(player, gearType, sourcePlayerName);
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

            sendFoundItemMessage(player, finalCount + "x " + displayItemName(itemId_str));
        } else {
            LOGGER.warn("Invalid filler item index: {}", index);
        }
    }


    private void giveVaultGear(ServerPlayer player, String gearType, String sourcePlayerName) {
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
                case "sword" -> {
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:sword"));
                }
                case "axe" -> {
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:axe"));
                }
                case "wand" -> {
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:wand"));
                }
                case "shield" -> {
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:shield"));
                }
                case "focus" -> {
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:focus"));
                }
                default -> {
                    LOGGER.warn("Unknown gear type: {}", gearType);
                    //default to boots or smth idk
                    item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("the_vault:boots"));
                }
            }


            if (item == null) {
                LOGGER.warn("Could not resolve vault gear item for type {}", gearType);
                return;
            }

            // Create ItemStack
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);

            // Create unidentified vault gear at the target level.
            try {
                Class<?> vaultGearStateClass = Class.forName("iskallia.vault.gear.VaultGearState");
                Class<?> gearRollHelperClass = Class.forName("iskallia.vault.gear.GearRollHelper");
                Class<?> vaultGearDataClass = Class.forName("iskallia.vault.gear.data.VaultGearData");
                java.lang.reflect.Method read = vaultGearDataClass.getMethod(
                        "read",
                        net.minecraft.world.item.ItemStack.class
                );
                Object gearData = read.invoke(null, stack);

                java.lang.reflect.Method setItemLevel = vaultGearDataClass.getMethod("setItemLevel", int.class);
                setItemLevel.invoke(gearData, playerLevel);

                Object unidentifiedState = Enum.valueOf((Class<Enum>) vaultGearStateClass, "UNIDENTIFIED");
                java.lang.reflect.Method setState = vaultGearDataClass.getMethod("setState", vaultGearStateClass);
                setState.invoke(gearData, unidentifiedState);

                java.lang.reflect.Method write = vaultGearDataClass.getMethod(
                        "write",
                        net.minecraft.world.item.ItemStack.class
                );
                write.invoke(gearData, stack);

                java.lang.reflect.Method tickGearRoll = gearRollHelperClass.getMethod(
                        "tickGearRoll",
                        net.minecraft.world.item.ItemStack.class,
                        net.minecraft.world.entity.player.Player.class
                );
                tickGearRoll.invoke(null, stack, player);

                Object allowedRarity = APGearRarityManager.getForcedRarity();
                java.lang.reflect.Method setRarity = vaultGearDataClass.getMethod(
                        "setRarity",
                        Class.forName("iskallia.vault.gear.VaultGearRarity")
                );
                setRarity.invoke(gearData, allowedRarity);
                write.invoke(gearData, stack);

                LOGGER.info("Created unidentified vault {} at level {} rarity {} for {}",
                        gearType, playerLevel, allowedRarity, player.getName().getString());
            } catch (Exception e) {
                LOGGER.warn("Failed to initialize unidentified VH gear, using command fallback: {}", e.getMessage());
            }

            // Give the item to the player
            boolean added = player.addItem(stack);

            if (!added) {
                // Inventory full, drop at feet
                player.drop(stack, false);
            }

            sendFoundItemMessage(player, String.format("Vault %s (Level %d)", gearType, playerLevel));

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

    private String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private String findPrintPartText(JsonObject packet, String type, int occurrence) {
        if (!packet.has("data") || !packet.get("data").isJsonArray()) {
            return null;
        }

        int seen = 0;
        for (JsonElement element : packet.getAsJsonArray("data")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject part = element.getAsJsonObject();
            if (type.equals(getString(part, "type"))) {
                if (seen == occurrence) {
                    return getString(part, "text");
                }
                seen++;
            }
        }
        return null;
    }

    private String getPlayerName(int playerSlot) {
        if (playerSlot == slot) {
            return slotName;
        }
        return playerNames.getOrDefault(playerSlot, playerSlot >= 0 ? "Player " + playerSlot : "Archipelago");
    }

    private String resolvePlayerName(String value) {
        int playerSlot = parsePositiveInt(value, -1);
        if (playerSlot >= 0) {
            return getPlayerName(playerSlot);
        }
        return value;
    }

    private String resolveItemName(String value, int receiverSlot) {
        long itemId = parsePositiveLong(value, Long.MIN_VALUE);
        if (itemId == Long.MIN_VALUE) {
            return value;
        }

        String gameName = playerGames.getOrDefault(receiverSlot, game);
        String dataPackageName = resolveItemNameForGame(itemId, gameName);
        if (dataPackageName != null) {
            return dataPackageName;
        }

        return resolveItemNameForGame(itemId, game);
    }

    private String resolveItemNameForGame(long itemId, String gameName) {
        if (gameName != null) {
            Map<Long, String> dataPackageItems = itemNamesByGame.get(gameName);
            if (dataPackageItems != null && dataPackageItems.containsKey(itemId)) {
                return dataPackageItems.get(itemId);
            }
        }

        String progressionName = getProgressionItemName(itemId);
        if (progressionName != null) {
            return progressionName;
        }

        String unlockName = VaultHuntersData.getLocationNameById(itemId);
        if (unlockName != null) {
            return displayUnlockName(unlockName);
        }

        if (itemId >= 33700L && itemId < 33800L) {
            return "Filler";
        }
        if (itemId >= 44000L && itemId < 44500L) {
            return "Chest Check " + (itemId - 44000L + 1L);
        }
        return "Unknown Item [" + itemId + "]";
    }

    private String resolveLocationName(String value, int senderSlot) {
        long locationId = parsePositiveLong(value, Long.MIN_VALUE);
        if (locationId == Long.MIN_VALUE) {
            return value;
        }

        String gameName = playerGames.get(senderSlot);
        if (gameName != null) {
            Map<Long, String> dataPackageLocations = locationNamesByGame.get(gameName);
            if (dataPackageLocations != null && dataPackageLocations.containsKey(locationId)) {
                return dataPackageLocations.get(locationId);
            }
        }

        Long localId = VaultHuntersData.getLocationId(value);
        if (localId != null) {
            return value;
        }
        return "Unknown Location [" + locationId + "]";
    }

    private String getProgressionItemName(long itemId) {
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_LEVEL_CAP) return "Progressive Level Cap";
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_XP_SCALING) return "Progressive XP Scaling";
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_LOOT_SCALING) return "Progressive Loot Scaling";
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_GEAR_RARITY) return "Progressive Gear Rarity";
        return null;
    }

    private boolean isLocalVhapItem(long itemId) {
        if (itemId == Long.MIN_VALUE) {
            return false;
        }
        if (itemId >= 33700L && itemId < 33800L) {
            return true;
        }
        if (getProgressionItemName(itemId) != null) {
            return true;
        }
        return VaultHuntersData.getLocationNameById(itemId) != null;
    }

    private boolean isUnknownDataPackageName(String itemName) {
        return itemName != null && itemName.startsWith("Unknown Item [");
    }

    private String displayUnlockName(String unlockName) {
        int separator = unlockName.indexOf(':');
        String name = separator >= 0 ? unlockName.substring(separator + 1) : unlockName;
        return displayItemName("vhap:" + name);
    }

    private int parsePositiveInt(String value, int fallback) {
        long parsed = parsePositiveLong(value, fallback);
        if (parsed > Integer.MAX_VALUE) {
            return fallback;
        }
        return (int) parsed;
    }

    private long parsePositiveLong(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed >= 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String displayItemName(String itemId) {
        String name = itemId.substring(itemId.indexOf(':') + 1).replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder display = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (display.length() > 0) {
                display.append(' ');
            }
            display.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return display.toString();
    }

    private MutableComponent quoted(String text, ChatFormatting color, boolean bold) {
        MutableComponent component = new TextComponent("\"" + text + "\"").withStyle(color);
        if (bold) {
            component.withStyle(ChatFormatting.BOLD);
        }
        return component;
    }

    private MutableComponent colored(String text, ChatFormatting color) {
        return new TextComponent(text).withStyle(color);
    }

    private void sendFoundItemMessage(ServerPlayer player, String itemName) {
        MutableComponent message = colored("You", ChatFormatting.YELLOW)
                .append(colored(" found your ", ChatFormatting.WHITE))
                .append(quoted(itemName, ChatFormatting.AQUA, false));
        player.sendMessage(message, player.getUUID());
    }

    private void sendFoundUnlockMessage(ServerPlayer player, String type, String unlockName) {
        MutableComponent message = colored("You", ChatFormatting.YELLOW)
                .append(colored(" found your ", ChatFormatting.WHITE))
                .append(colored(capitalize(type), ChatFormatting.AQUA))
                .append(colored(": ", ChatFormatting.WHITE))
                .append(colored(displayUnlockName(unlockName), ChatFormatting.AQUA));
        player.sendMessage(message, player.getUUID());
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void broadcastComponentToPlayers(MutableComponent message) {
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendMessage(message, player.getUUID());
        }
    }

    private ServerPlayer getCurrentPlayer() {
        if (server == null) return null;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.get(0);
    }

    private void sendConnectedMessageToPlayers() {
        broadcastStatusToPlayers("[AP] Connected as " + slotName, ChatFormatting.GREEN);
    }

    private void broadcastStatusToPlayers(String message, ChatFormatting color) {
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendMessage(
                    new TextComponent(message).withStyle(color),
                    player.getUUID()
            );
        }
    }

}
