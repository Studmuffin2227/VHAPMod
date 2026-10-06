package studmuffin.vhapmod;

import com.google.gson.*;
import iskallia.vault.world.data.DiscoveredArtifactsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studmuffin.vhapmod.menu.CheckTableMenu;

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
    private static final int PROGRESSION_RGB = 0xAE98EE;
    private final Gson gson = new Gson();
    private WebSocket webSocket;
    private VaultHuntersManager vhManager;
    private net.minecraft.server.MinecraftServer server;

    // Connection state
    private String slotName;
    private String password;
    private String game = "Vault Hunters";
    private int team = 0;
    private int slot = 0;
    private String roomSeedName = "";
    private boolean connected = false;
    private boolean connecting = false;
    private UUID targetPlayerId;
    private boolean checksBehindSkillsEnabled = false;
    private boolean autoLearnSkillsEnabled = false;
    private boolean deathLinkEnabled = false;
    private boolean deathLinkVaultsOnly = false;
    private boolean deathLinkVaultCurse = false;
    private boolean ringLinkEnabled = false;
    private int ringLinkSource = new Random().nextInt();
    private double pendingRingLinkAmount = 0.0D;
    private int suppressNextDeathLinks = 0;
    private double lastReceivedDeathLinkTime = -1.0D;
    private String shopCheckRewardDisplay = "all";
    private String bountyCheckRewardDisplay = "all";
    private String fillerRecipientMode = "single";

    // Game state
    private Set<Long> checkedLocations = new HashSet<>();
    private Set<String> processedItems = new HashSet<>();
    private int goalLevel = 20;
    private String goalType = "level";
    private int artifactRequirement = 25;
    private final Map<Integer, String> playerNames = new HashMap<>();
    private final Map<Integer, String> playerGames = new HashMap<>();
    private final Map<String, Map<Long, String>> itemNamesByGame = new HashMap<>();
    private final Map<String, Map<Long, String>> locationNamesByGame = new HashMap<>();
    private final Map<Long, String> scoutedRewardsByLocation = new HashMap<>();
    private final Map<Long, Integer> scoutedRewardFlagsByLocation = new HashMap<>();
    private final Map<Long, String> scoutedRewardReceiversByLocation = new HashMap<>();
    private final Map<Long, Boolean> scoutedRewardIsLocalByLocation = new HashMap<>();
    private final Map<Long, UUID> scoutTargetsByLocation = new HashMap<>();
    private final List<JsonObject> pendingItemMessages = new ArrayList<>();
    private boolean requestedDataPackage = false;
    private static final Map<Long, String[]> PROGRESSIVE_TALENTS = Map.ofEntries(
            Map.entry(43129L, new String[]{"vhtalent:depleted", "vhtalent:methodical", "vhtalent:blood_rush"}),
            Map.entry(43121L, new String[]{"vhtalent:lightning_damage", "vhtalent:lightning_stun"}),
            Map.entry(43130L, new String[]{"vhtalent:fatal_strike", "vhtalent:fatal_strike_ii"}),
            Map.entry(43134L, new String[]{"vhtalent:mana_steal", "vhtalent:mana_steal_ii"}),
            Map.entry(43136L, new String[]{"vhtalent:life_steal", "vhtalent:life_steal_ii"}),
            Map.entry(43118L, new String[]{"vhtalent:sorcery", "vhtalent:arcana"}),
            Map.entry(43103L, new String[]{"vhtalent:speed", "vhtalent:speed_ii", "vhtalent:blazing"}),
            Map.entry(43100L, new String[]{"vhtalent:intelligence", "vhtalent:intelligence_ii"}),
            Map.entry(43101L, new String[]{"vhtalent:haste", "vhtalent:haste_ii"}),
            Map.entry(43140L, new String[]{"vhtalent:prudent", "vhtalent:prudent_ii"})
    );
    private static final Map<String, String> UNLOCK_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("vhskill:chain_lightning", "Lightning Strike"),
            Map.entry("vhskill:grenade", "Chaos Cube"),
            Map.entry("vhskill:heal_group", "Group Heal"),
            Map.entry("vhskill:dash_damage", "Bullet"),
            Map.entry("vhskill:nova_slow", "Frost Nova"),
            Map.entry("vhskill:nova_dot", "Poison Nova"),
            Map.entry("vhskill:ghost_walk_spirit", "Spirit Walk"),
            Map.entry("vhskill:rampage_leech", "Vampiric"),
            Map.entry("vhskill:rampage_chain", "Chaining"),
            Map.entry("vhskill:smite_archon", "Archon"),
            Map.entry("vhskill:empower_slowness_aura", "Entropic Bind"),
            Map.entry("vhskill:vein_miner_durability", "Finesse Miner"),
            Map.entry("vhskill:vein_miner_void", "Void Miner"),
            Map.entry("vhskill:shield_bash", "Shield Bash"),
            Map.entry("vhskill:implode_life_tap", "Life Tap"),
            Map.entry("vhskill:taunt_repel", "Fear"),
            Map.entry("vhskill:taunt_charm", "Charm"),
            Map.entry("vhskill:storm_arrow_blizzard", "Blizzard Arrow (Legacy)"),
            Map.entry("vhskill:ice_bolt_blast", "Glacial Blast"),
            Map.entry("vhskill:arcane_rail", "Rail"),
            Map.entry("vhskill:earthquake_landmine", "Landmine"),
            Map.entry("vhtalent:strength", "Might"),
            Map.entry("vhtalent:lightning_damage", "Lightning Finesse"),
            Map.entry("vhtalent:lightning_stun", "Lightning Mastery"),
            Map.entry("vhtalent:fatal_strike_ii", "Greater Fatal Strike"),
            Map.entry("vhtalent:mana_steal_ii", "Greater Mana Steal"),
            Map.entry("vhtalent:life_steal", "Life Leech"),
            Map.entry("vhtalent:life_steal_ii", "Greater Life Leech"),
            Map.entry("vhtalent:blizzard", "Frozen Impact"),
            Map.entry("vhtalent:frostbite", "Frost Bite"),
            Map.entry("vhtalent:hunters_instinct", "Hunter's Instinct"),
            Map.entry("vhtalent:farmer_twerker", "Twerker"),
            Map.entry("vhtalent:might_ii", "Greater Might"),
            Map.entry("vhtalent:intelligence_ii", "Greater Intelligence"),
            Map.entry("vhtalent:haste_ii", "Greater Haste"),
            Map.entry("vhtalent:speed_ii", "Greater Speed"),
            Map.entry("vhtalent:prudent_ii", "Greater Prudent")
    );
    private static final String[] FILLER_REWARD_NAMES = {
            "Cooked Vault Steaks",
            "Chromatic Iron Ingots",
            "Shulker Box",
            "Bottles O' Enchanting",
            "Emeralds",
            "Ender Pearls",
            "Backpack",
            "Bounty Pearls",
            "Gemstones",
            "Chromatic Steel Ingots",
            "Vault Gold",
            "Vault Silver",
            "Vault Bronze Cache",
            "Diamonds",
            "Vault Alloy",
            "Wild Focus",
            "Vault Platings",
            "Vault Scrap",
            "Vault Gold Cache",
            "Vault Diamond",
            "Hamburger",
            "Blank Seal",
            "Silver Scrap",
            "Soul Shard",
            "Vault Helmet",
            "Vault Chestplate",
            "Vault Leggings",
            "Vault Boots",
            "Mod Box",
            "Diamond Cache",
            "Phoenix Capstone",
            "Catalyst Fragments",
            "Inscription Pieces",
            "Unidentified Artifact",
            "Vault Trinket",
            "Vault Diamond Cache",
            "Vault Gold Hoard",
            "Vault Silver Cache",
            "Vault Silver Hoard",
            "POG",
            "Netherite Ingot",
            "Wardrobe",
            "Vault Bronze Hoard",
            "Echo Gem",
            "Sour Orange",
            "Bamboo",
            "Vault Gold Crate",
            "Vault Silver Crate",
            "Vault Bronze Crate",
            "Neuralizer",
            "Perfect Echo Gem",
            "Lost Bounty",
            "Ember",
            "Omega POG",
            "Blessing"
    };

    public APWebSocketClient(VaultHuntersManager manager) {
        this.vhManager = manager;
    }

    public void setServer(net.minecraft.server.MinecraftServer server) {
        this.server = server;
        APConnectionConfig config = APConnectionConfig.load();
        setFillerRecipientMode(config.fillerRecipientMode, false);
    }

    public void setTargetPlayer(ServerPlayer player) {
        this.targetPlayerId = player.getUUID();
        LOGGER.info("AP rewards will target Minecraft player {}", player.getGameProfile().getName());
    }

    public synchronized void claimTargetPlayerIfUnset(ServerPlayer player) {
        if (targetPlayerId != null) {
            return;
        }
        setTargetPlayer(player);
        APConnectionConfig config = APConnectionConfig.load();
        config.targetPlayerUuid = player.getUUID().toString();
        config.save();
        LOGGER.info("Persisted {} as the AP target player for auto-connect", player.getGameProfile().getName());
    }

    public void setTargetPlayerId(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return;
        }
        try {
            this.targetPlayerId = UUID.fromString(playerUuid);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ignoring invalid configured AP target player UUID: {}", playerUuid);
        }
    }

    public boolean shouldCountQuestChecksFor(ServerPlayer player) {
        return player != null;
    }

    public boolean areSkillUnlockChecksEnabled() {
        return checksBehindSkillsEnabled;
    }

    public String getFillerRecipientMode() {
        return fillerRecipientMode;
    }

    public boolean setFillerRecipientMode(String mode) {
        return setFillerRecipientMode(mode, true);
    }

    private boolean setFillerRecipientMode(String mode, boolean save) {
        String normalized = normalizeFillerRecipientMode(mode);
        if (normalized == null) {
            return false;
        }
        fillerRecipientMode = normalized;
        if (save) {
            APConnectionConfig config = APConnectionConfig.load();
            config.fillerRecipientMode = normalized;
            config.save();
        }
        return true;
    }

    private String normalizeFillerRecipientMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "single";
        }
        return switch (mode.trim().toLowerCase(Locale.ROOT)) {
            case "single", "target", "player" -> "single";
            case "online" -> "online";
            case "all", "offline" -> "all";
            default -> null;
        };
    }

    // ========== CONNECTION ==========

    public CompletableFuture<Void> connect(String host, int port, String slotName, String password) {
        this.slotName = slotName;
        this.password = password;
        this.connected = false;
        this.connecting = true;
        this.requestedDataPackage = false;
        this.ringLinkSource = new Random().nextInt();
        this.pendingRingLinkAmount = 0.0D;
        if (vhManager != null) {
            vhManager.markSlotSettingsUnloaded();
        }

        String uri = buildWebSocketUri(host, port);
        String fallbackUri = buildFallbackWebSocketUri(host, port, uri);
        LOGGER.info("Connecting to AP server: {}", uri);

        HttpClient client = HttpClient.newHttpClient();

        return connectToUri(client, uri)
                .exceptionallyCompose(ex -> {
                    if (fallbackUri == null) {
                        return failedConnection(ex);
                    }

                    Throwable cause = unwrap(ex);
                    LOGGER.warn("AP connection to {} failed: {}. Retrying with {}", uri, describe(cause), fallbackUri);
                    return connectToUri(client, fallbackUri).exceptionallyCompose(this::failedConnection);
                });
    }

    private CompletableFuture<Void> connectToUri(HttpClient client, String uri) {
        return client.newWebSocketBuilder()
                .buildAsync(URI.create(uri), this)
                .thenAccept(ws -> {
                    if (!connecting) {
                        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
                        return;
                    }
                    this.webSocket = ws;
                    LOGGER.info("WebSocket connected, sending Connect packet");
                    sendConnect();
                })
                .orTimeout(10, TimeUnit.SECONDS);
    }

    private CompletableFuture<Void> failedConnection(Throwable ex) {
        Throwable cause = unwrap(ex);
        String reason = describe(cause);
        LOGGER.error("Failed to connect: {}", reason);
        this.connected = false;
        this.connecting = false;
        broadcastStatusToPlayers("[AP] Connection failed: " + reason, ChatFormatting.RED);
        return CompletableFuture.failedFuture(cause);
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

    private String buildFallbackWebSocketUri(String host, int port, String primaryUri) {
        String trimmedHost = host == null ? "" : host.trim();
        if (trimmedHost.startsWith("ws://") || trimmedHost.startsWith("wss://")) {
            return null;
        }
        if (!primaryUri.startsWith("wss://")) {
            return null;
        }
        return String.format("ws://%s:%d", trimmedHost, port);
    }

    private Throwable unwrap(Throwable ex) {
        Throwable cause = ex;
        while ((cause instanceof CompletionException || cause instanceof java.util.concurrent.ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String describe(Throwable cause) {
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
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
        JsonArray tags = new JsonArray();
        if (deathLinkEnabled) {
            tags.add("DeathLink");
        }
        if (ringLinkEnabled) {
            tags.add("RingLink");
        }
        connectPacket.add("tags", tags);

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

    public boolean isConnectionActive() {
        return isConnected() || connecting;
    }

    public void disconnect() {
        disconnect("Client disconnecting");
    }

    public void disconnect(String reason) {
        connected = false;
        connecting = false;
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, reason);
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
        connecting = false;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        LOGGER.info("WebSocket closed: {} - {}", statusCode, reason);
        connected = false;
        connecting = false;
        if (this.webSocket == webSocket) {
            this.webSocket = null;
        }
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
            case "RoomUpdate":
                if (syncCheckedLocations(packet, false)) {
                    refreshOpenCheckTables();
                }
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
                if ("Bounced".equals(cmd)) {
                    handleBounced(packet);
                } else {
                    LOGGER.debug("Received {}", cmd);
                }
                break;
            default:
                LOGGER.debug("Unhandled packet: {}", cmd);
        }
    }

    private void handleRoomInfo(JsonObject packet) {
        LOGGER.info("Received RoomInfo");
        if (packet.has("seed_name")) {
            roomSeedName = packet.get("seed_name").getAsString();
            LOGGER.info("AP room seed: {}", roomSeedName);
        }
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
        connecting = false;

        if (packet.has("slot")) {
            slot = packet.get("slot").getAsInt();
            playerNames.putIfAbsent(slot, slotName);
            playerGames.putIfAbsent(slot, game);
        }

        if (packet.has("team")) {
            team = packet.get("team").getAsInt();
        }

        syncCheckedLocations(packet, true);

        LOGGER.info("Connected to AP as slot {} on team {}", slot, team);
        syncReceivedItemCacheScope();

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
                vhManager.setGoalType(goalType);
            }

            if (slotData.has("artifact_requirement")) {
                artifactRequirement = Math.max(1, slotData.get("artifact_requirement").getAsInt());
                LOGGER.info("Artifact requirement: {}", artifactRequirement);
                vhManager.setArtifactRequirement(artifactRequirement);
            }

            if (slotData.has("level_cap_steps") && slotData.get("level_cap_steps").isJsonArray()) {
                JsonArray values = slotData.getAsJsonArray("level_cap_steps");
                int[] steps = new int[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    steps[i] = values.get(i).getAsInt();
                }
                vhManager.setLevelCapSteps(steps);
            }

        deathLinkEnabled = slotData.has("death_link") && slotData.get("death_link").getAsBoolean();
        deathLinkVaultsOnly = slotData.has("death_link_vaults_only") && slotData.get("death_link_vaults_only").getAsBoolean();
        deathLinkVaultCurse = slotData.has("death_link_vault_curse") && slotData.get("death_link_vault_curse").getAsBoolean();
        ringLinkEnabled = slotData.has("ring_link") && slotData.get("ring_link").getAsBoolean();
        if (deathLinkEnabled || ringLinkEnabled) {
            sendConnectUpdate();
        }

            int questChecks = slotData.has("quest_check_count")
                    ? slotData.get("quest_check_count").getAsInt()
                    : getQuestChecksForGoalLevel(goalLevel);
            vhManager.setTotalQuestChecks(questChecks);
            LOGGER.info("Quest checks: {}", questChecks);

            int bountyChecks = slotData.has("bounty_check_count")
                    ? slotData.get("bounty_check_count").getAsInt()
                    : 0;
            vhManager.setTotalBountyChecks(bountyChecks);
            LOGGER.info("Bounty checks: {}", bountyChecks);

            if (slotData.has("bounty_checks_per_progressive")) {
                vhManager.setBountyChecksPerProgressive(slotData.get("bounty_checks_per_progressive").getAsInt());
            }

            shopCheckRewardDisplay = readRewardDisplayMode(slotData, "display_shop_check_rewards", "all");
            bountyCheckRewardDisplay = readRewardDisplayMode(slotData, "display_bounty_check_rewards", "all");
            LOGGER.info("Shop check reward display: {}; Bounty check reward display: {}", shopCheckRewardDisplay, bountyCheckRewardDisplay);

            int shopLevel = slotData.has("progression_goal_level")
                    ? slotData.get("progression_goal_level").getAsInt()
                    : slotData.has("max_shop_level") ? slotData.get("max_shop_level").getAsInt() : goalLevel;
            int maxChestChecksForGoal = VaultHuntersManager.getChestChecksForGoalLevel(shopLevel);
            int chestChecks = slotData.has("vault_chest_checks")
                    ? Math.min(slotData.get("vault_chest_checks").getAsInt(), maxChestChecksForGoal)
                    : maxChestChecksForGoal;
            vhManager.setTotalChestChecks(chestChecks);
            LOGGER.info("Chest checks: {}", chestChecks);

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

            if (slotData.has("checks_behind_skills")) {
                checksBehindSkillsEnabled = slotData.get("checks_behind_skills").getAsBoolean();
            } else if (slotData.has("skill_checks")) {
                checksBehindSkillsEnabled = slotData.get("skill_checks").getAsBoolean();
            }
            if (slotData.has("auto_learn_skills")) {
                autoLearnSkillsEnabled = slotData.get("auto_learn_skills").getAsBoolean();
            } else if (slotData.has("skill_checks")) {
                autoLearnSkillsEnabled = !slotData.get("skill_checks").getAsBoolean();
            }
            if (checksBehindSkillsEnabled && autoLearnSkillsEnabled) {
                LOGGER.warn("Both Checks Behind Skills and Auto Learn Skills are enabled; disabling Auto Learn Skills to avoid immediate self-checks");
                autoLearnSkillsEnabled = false;
            }
            APSkillLockManager.setUnlockChecksEnabled(checksBehindSkillsEnabled);
            LOGGER.info("Checks Behind Skills: {}; Auto Learn Skills: {}", checksBehindSkillsEnabled, autoLearnSkillsEnabled);

            if (slotData.has("max_xp_scaling")) {
                vhManager.setMaxXpScaling(slotData.get("max_xp_scaling").getAsInt());
            }

            if (slotData.has("max_loot_scaling")) {
                vhManager.setMaxLootScaling(slotData.get("max_loot_scaling").getAsInt());
            }

            if (slotData.has("progressive_vault_time")) {
                vhManager.setProgressiveVaultTime(slotData.get("progressive_vault_time").getAsInt());
            }

            vhManager.setProgressiveCrystalCostReduction(-1);

            if (slotData.has("start_with_vault_kit")) {
                vhManager.setStartWithVaultKit(slotData.get("start_with_vault_kit").getAsBoolean());
            }

            vhManager.markSlotSettingsLoaded();

            if (server != null) {
                runOnServerThread(() -> {
                    vhManager.initializeWorldProgression(server);
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        vhManager.grantStartingKitIfEnabled(player);
                        if (autoLearnSkillsEnabled) {
                            vhManager.repairAutoLearnedUnlocks(player);
                        }
                    }
                    APBountyCheckManager.refreshExistingRewards(server);
                });
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

    private boolean syncCheckedLocations(JsonObject packet, boolean replace) {
        if (!packet.has("checked_locations") || !packet.get("checked_locations").isJsonArray()) {
            return false;
        }

        if (replace) {
            checkedLocations.clear();
        }
        int synced = 0;
        boolean changed = false;
        for (JsonElement element : packet.getAsJsonArray("checked_locations")) {
            try {
                long locationId = element.getAsLong();
                if (checkedLocations.add(locationId)) {
                    changed = true;
                }
                if (vhManager != null && !vhManager.isLocationChecked(locationId)) {
                    vhManager.markLocationChecked(locationId);
                }
                synced++;
            } catch (Exception ignored) {
            }
        }
        LOGGER.info("Synced {} checked AP locations", synced);
        return changed;
    }

    private int getQuestChecksForGoalLevel(int level) {
        if (level <= 5) return 18;
        if (level <= 10) return 25;
        if (level <= 20) return 37;
        if (level <= 40) return 56;
        if (level <= 50) return 59;
        if (level <= 60) return 73;
        return 76;
    }

    private void sendConnectUpdate() {
        if (webSocket == null) {
            return;
        }
        JsonObject packet = new JsonObject();
        packet.addProperty("cmd", "ConnectUpdate");
        JsonArray tags = new JsonArray();
        if (deathLinkEnabled) {
            tags.add("DeathLink");
        }
        if (ringLinkEnabled) {
            tags.add("RingLink");
        }
        packet.add("tags", tags);
        sendPacket(packet);
    }

    public boolean isRingLinkEnabled() {
        return ringLinkEnabled;
    }

    public void sendRingLinkManaDelta(float manaDelta) {
        if (!ringLinkEnabled || Math.abs(manaDelta) < 0.001F) {
            return;
        }

        pendingRingLinkAmount += manaDelta;
        int amount = (int) pendingRingLinkAmount;
        if (amount == 0) {
            return;
        }
        pendingRingLinkAmount -= amount;
        sendRingLinkAmount(amount);
    }

    private void sendRingLinkAmount(int amount) {
        if (!ringLinkEnabled || !isConnected() || webSocket == null || amount == 0) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("time", System.currentTimeMillis() / 1000.0D);
        data.addProperty("source", ringLinkSource);
        data.addProperty("amount", amount);

        JsonObject packet = new JsonObject();
        packet.addProperty("cmd", "Bounce");
        JsonArray tags = new JsonArray();
        tags.add("RingLink");
        packet.add("tags", tags);
        packet.add("data", data);
        sendPacket(packet);
    }

    public void onPlayerDeath(ServerPlayer player) {
        if (!deathLinkEnabled || !shouldCountQuestChecksFor(player)) {
            return;
        }
        if (suppressNextDeathLinks > 0) {
            suppressNextDeathLinks--;
            return;
        }
        if (deathLinkVaultsOnly && !isInVault(player)) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("time", System.currentTimeMillis() / 1000.0D);
        data.addProperty("source", slotName);
        data.addProperty("cause", player.getGameProfile().getName() + " died in Vault Hunters");

        JsonObject packet = new JsonObject();
        packet.addProperty("cmd", "Bounce");
        JsonArray tags = new JsonArray();
        tags.add("DeathLink");
        packet.add("tags", tags);
        packet.add("data", data);
        sendPacket(packet);
    }

    private void handleBounced(JsonObject packet) {
        if (ringLinkEnabled && hasTag(packet, "RingLink") && packet.has("data")) {
            handleRingLinkBounced(packet.getAsJsonObject("data"));
            return;
        }
        if (!deathLinkEnabled || !hasTag(packet, "DeathLink") || !packet.has("data")) {
            return;
        }
        JsonObject data = packet.getAsJsonObject("data");
        if (data.has("source") && slotName != null && slotName.equals(data.get("source").getAsString())) {
            return;
        }
        double deathTime = data.has("time") ? data.get("time").getAsDouble() : 0.0D;
        if (deathTime > 0.0D && deathTime <= lastReceivedDeathLinkTime) {
            return;
        }
        lastReceivedDeathLinkTime = deathTime;
        String sourceName = data.has("source") ? data.get("source").getAsString() : "Archipelago";
        if (server == null) {
            return;
        }
        server.execute(() -> {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) {
                return;
            }

            ReceivedItemsData receivedData = ReceivedItemsData.get(server);
            for (ServerPlayer player : players) {
                receivedData.addKnownPlayer(player.getUUID());
                if (!deathLinkVaultCurse && deathLinkVaultsOnly && !isInVault(player)) {
                    continue;
                }
                if (deathLinkVaultCurse) {
                    DeathLinkCurseManager.receiveDeathLink(player, sourceName);
                    continue;
                }
                player.sendMessage(
                        new TextComponent("DeathLink from " + sourceName + " killed you")
                        .withStyle(ChatFormatting.RED),
                        player.getUUID()
                );
                suppressNextDeathLinks++;
                player.kill();
            }

            if (deathLinkVaultCurse) {
                APProgressData progressData = APProgressData.get(server);
                for (UUID playerId : receivedData.getKnownPlayers()) {
                    if (server.getPlayerList().getPlayer(playerId) == null) {
                        progressData.addPendingDeathLinkCurse(playerId);
                    }
                }
            }
        });
    }

    private void handleRingLinkBounced(JsonObject data) {
        if (data.has("source") && data.get("source").getAsInt() == ringLinkSource) {
            return;
        }
        if (!data.has("amount") || server == null) {
            return;
        }
        int amount = data.get("amount").getAsInt();
        server.execute(() -> APRingLinkManager.receiveRemoteAmount(server, amount));
    }

    private boolean hasTag(JsonObject packet, String expected) {
        if (!packet.has("tags") || !packet.get("tags").isJsonArray()) {
            return false;
        }
        for (JsonElement tag : packet.getAsJsonArray("tags")) {
            if (expected.equals(tag.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private boolean isInVault(ServerPlayer player) {
        ResourceLocation dimension = player.level.dimension().location();
        return "the_vault".equals(dimension.getNamespace()) || dimension.getPath().contains("vault");
    }

    private void sendPacket(JsonObject packet) {
        if (webSocket == null) {
            return;
        }
        JsonArray wrapper = new JsonArray();
        wrapper.add(packet);
        webSocket.sendText(gson.toJson(wrapper), true);
    }

    private void runOnServerThread(Runnable action) {
        if (server == null) {
            return;
        }
        if (server.isSameThread()) {
            action.run();
        } else {
            server.execute(action);
        }
    }

    public boolean sendChat(String text) {
        if (!isConnected() || webSocket == null || text == null || text.isBlank()) {
            return false;
        }
        JsonObject packet = new JsonObject();
        packet.addProperty("cmd", "Say");
        packet.addProperty("text", text);
        sendPacket(packet);
        return true;
    }

    private void syncReceivedItemCacheScope() {
        if (server == null) {
            return;
        }
        if (!server.isSameThread()) {
            runOnServerThread(this::syncReceivedItemCacheScope);
            return;
        }
        ReceivedItemsData receivedData = ReceivedItemsData.get(server);
        if (receivedData.syncConnectionSignature(getConnectionSignature())) {
            APProgressData.get(server).resetSlotScopedProgress();
            LOGGER.info("Reset VHAP slot-scoped progression for new AP connection signature");
        }
    }

    private String getConnectionSignature() {
        String seed = roomSeedName == null || roomSeedName.isBlank() ? "unknown-seed" : roomSeedName;
        String name = slotName == null || slotName.isBlank() ? "unknown-slot-name" : slotName;
        return seed + "|team:" + team + "|slot:" + slot + "|name:" + name;
    }

    private void handleReceivedItems(JsonObject packet) {
        if (!packet.has("items")) return;
        if (server == null) {
            LOGGER.warn("Server is null, cannot process items");
            return;
        }
        if (!server.isSameThread()) {
            JsonObject copy = packet.deepCopy();
            runOnServerThread(() -> handleReceivedItems(copy));
            return;
        }

        // Get persistent storage
        ReceivedItemsData receivedData = ReceivedItemsData.get(server);
        receivedData.syncConnectionSignature(getConnectionSignature());

        JsonArray items = packet.getAsJsonArray("items");
        int firstItemIndex = packet.has("index") ? packet.get("index").getAsInt() : -1;
        LOGGER.info("=== Receiving {} items from AP index {} ===", items.size(), firstItemIndex);

        for (int i = 0; i < items.size(); i++) {
            JsonObject itemData = items.get(i).getAsJsonObject();

            long itemId = itemData.get("item").getAsLong();
            long locationId = itemData.get("location").getAsLong();
            int sendingPlayer = itemData.has("player") ? itemData.get("player").getAsInt() : -1;
            int itemFlags = itemData.has("flags") ? itemData.get("flags").getAsInt() : 0;

            // Prevent duplicate processing
            String legacyKey = itemId + ":" + locationId;
            String uniqueKey = firstItemIndex >= 0 ? "index:" + (firstItemIndex + i) : legacyKey;
            if (receivedData.hasProcessed(uniqueKey)) {
                LOGGER.info("Skipping already processed AP item {} from location {} using key {}",
                        itemId, locationId, uniqueKey);
                continue;
            }

            // Process item
            processReceivedItem(uniqueKey, itemId, locationId, sendingPlayer, itemFlags);
        }
    }

    public void flushPendingReceivedItems() {
        if (server == null) {
            return;
        }

        server.execute(() -> {
            ReceivedItemsData receivedData = ReceivedItemsData.get(server);
            for (ReceivedItemsData.PendingItem item : receivedData.getPendingItems()) {
                if (receivedData.hasProcessed(item.uniqueKey)) {
                    receivedData.removePending(item.uniqueKey);
                    continue;
                }
                processReceivedItem(item.uniqueKey, item.itemId, item.locationId, item.sendingPlayer, item.flags);
            }
        });
    }

    public void registerKnownPlayer(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return;
        }
        ReceivedItemsData.get(player.getServer()).addKnownPlayer(player.getUUID());
    }

    public void flushPendingFillerItems(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return;
        }
        ReceivedItemsData receivedData = ReceivedItemsData.get(player.getServer());
        for (ReceivedItemsData.PendingPlayerItem item : receivedData.getPendingItemsForPlayer(player.getUUID())) {
            processReceivedItemForPlayer(player, item.itemId, item.locationId, item.sendingPlayer, item.flags);
            receivedData.removePendingForPlayer(player.getUUID(), item.uniqueKey);
        }
    }

    private void processReceivedItem(String uniqueKey, long itemId, long locationId, int sendingPlayer, int itemFlags) {
        // CRITICAL: Must run on server thread to send packets and interact with player
        if (server != null) {
            server.execute(() -> {
                ReceivedItemsData receivedData = ReceivedItemsData.get(server);
                if (receivedData.hasProcessed(uniqueKey)) {
                    return;
                }

                if (!expandReceivedItemToRecipients(receivedData, uniqueKey, itemId, locationId, sendingPlayer, itemFlags)) {
                    receivedData.addPending(uniqueKey, itemId, locationId, sendingPlayer, itemFlags);
                    LOGGER.warn("No player available to receive item {}; queued for next login", uniqueKey);
                    return;
                }
                receivedData.markProcessed(uniqueKey);
            });
        }
    }

    private boolean expandReceivedItemToRecipients(
            ReceivedItemsData receivedData,
            String uniqueKey,
            long itemId,
            long locationId,
            int sendingPlayer,
            int itemFlags
    ) {
        List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();
        if (isServerProgressionItem(itemId)) {
            ServerPlayer player = getCurrentPlayer();
            if (player == null) {
                return false;
            }
            LOGGER.info("Processing server-wide AP item {} from location {}", itemId, locationId);
            if (vhManager.applyProgressionItem(itemId, player)) {
                for (ServerPlayer recipient : onlinePlayers) {
                    sendReceivedItemMessage(recipient, locationId, sendingPlayer, resolveItemNameForGame(itemId, game), getItemColor(itemFlags, itemId));
                }
            }
            return true;
        }

        Set<UUID> recipients = new HashSet<>();
        if (isFillerItem(itemId) && "single".equals(fillerRecipientMode)) {
            ServerPlayer player = getCurrentPlayer();
            if (player == null) {
                return false;
            }
            recipients.add(player.getUUID());
        } else if (isFillerItem(itemId) && "online".equals(fillerRecipientMode)) {
            for (ServerPlayer recipient : onlinePlayers) {
                recipients.add(recipient.getUUID());
            }
        } else {
            recipients.addAll(receivedData.getKnownPlayers());
            for (ServerPlayer recipient : onlinePlayers) {
                recipients.add(recipient.getUUID());
            }
        }

        if (recipients.isEmpty()) {
            return false;
        }

        int delivered = 0;
        int queued = 0;
        for (UUID playerId : recipients) {
            ServerPlayer recipient = server.getPlayerList().getPlayer(playerId);
            if (recipient != null) {
                receivedData.addKnownPlayer(recipient.getUUID());
                processReceivedItemForPlayer(recipient, itemId, locationId, sendingPlayer, itemFlags);
                delivered++;
            } else {
                receivedData.addPendingForPlayer(playerId, uniqueKey, itemId, locationId, sendingPlayer, itemFlags);
                queued++;
            }
        }

        LOGGER.info("Expanded AP item {} to {} online players and {} offline player queues", itemId, delivered, queued);
        return true;
    }

    private void processReceivedItemForPlayer(ServerPlayer player, long itemId, long locationId, int sendingPlayer, int itemFlags) {
        LOGGER.info("Processing item {} from location {} for {}", itemId, locationId, player.getName().getString());

        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_BACKPACK_MOD) {
            String unlockedMod = vhManager.unlockNextBackpackMod(player);
            sendReceivedUnlockMessage(locationId, sendingPlayer, "mod", unlockedMod);
            return;
        }

        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_ARTIFACT) {
            giveProgressiveArtifact(player, locationId, sendingPlayer, itemFlags);
            return;
        }

        String trapId = APTrapManager.trapIdForItem(itemId);
        if (trapId != null) {
            APTrapManager.receiveTrap(player, trapId);
            sendReceivedItemMessage(player, locationId, sendingPlayer, APTrapManager.displayName(trapId), getItemColor(itemFlags, itemId));
            return;
        }

        if (itemId == APBlessingManager.ITEM_BLESSING) {
            APBlessingManager.receiveBlessing(player, getPlayerName(sendingPlayer));
            return;
        }

        if (vhManager.applyProgressionItem(itemId, player)) {
            sendReceivedItemMessage(player, locationId, sendingPlayer, resolveItemNameForGame(itemId, game), getItemColor(itemFlags, itemId));
            return;
        }

        String unlockName = VaultHuntersData.getLocationNameById(itemId);
        if (unlockName != null) {
            if (unlockName.startsWith("vhskill:")) {
                String deliveredSkill = resolveReceivedSkillUnlock(player, unlockName);
                sendReceivedUnlockMessage(locationId, sendingPlayer,
                        VaultHuntersData.isSpecializationSkill(deliveredSkill) ? "specialization" : "skill",
                        deliveredSkill);
            } else if (unlockName.startsWith("vhtalent:")) {
                String deliveredTalent = resolveReceivedTalentUnlock(player, itemId, unlockName);
                if (shouldAutoActivateUnlocks()) {
                    vhManager.buyTalentRankOne(player, deliveredTalent);
                }
                sendReceivedUnlockMessage(locationId, sendingPlayer, "talent", deliveredTalent);
            } else if (unlockName.startsWith("vhmod:")) {
                if (shouldAutoActivateUnlocks()) {
                    unlockModWithPrerequisites(player, unlockName);
                } else {
                    vhManager.unlockMod(player, unlockName);
                }
                sendReceivedUnlockMessage(locationId, sendingPlayer, "mod", unlockName);
            } else if (unlockName.startsWith("vhexpertise:")) {
                vhManager.unlockExpertise(player, unlockName);
                if (shouldAutoActivateUnlocks()) {
                    vhManager.buyExpertiseRankOne(player, unlockName);
                }
                sendReceivedUnlockMessage(locationId, sendingPlayer, "expertise", unlockName);
            }
        } else if (isFillerItem(itemId)) {
            giveFillerItem(player, itemId, getPlayerName(sendingPlayer), locationId, sendingPlayer);
        }
    }

    private boolean isFillerItem(long itemId) {
        return itemId >= 33700 && itemId < 33800;
    }

    public List<String> getKnownPlayerNames() {
        List<String> names = new ArrayList<>();
        for (String name : playerNames.values()) {
            if (name != null && !name.isBlank() && !names.contains(name)) {
                names.add(name);
            }
        }
        if (slotName != null && !slotName.isBlank() && !names.contains(slotName)) {
            names.add(slotName);
        }
        return names;
    }

    private boolean isServerProgressionItem(long itemId) {
        return itemId == VaultHuntersData.ITEM_PROGRESSIVE_LEVEL_CAP
                || itemId == VaultHuntersData.ITEM_PROGRESSIVE_XP_SCALING
                || itemId == VaultHuntersData.ITEM_PROGRESSIVE_LOOT_SCALING
                || itemId == VaultHuntersData.ITEM_PROGRESSIVE_GEAR_RARITY
                || itemId == VaultHuntersData.ITEM_PROGRESSIVE_VAULT_TIME
                || itemId == VaultHuntersData.ITEM_PROGRESSIVE_BOUNTY
                || itemId == VaultHuntersData.ITEM_PROGRESSIVE_CRYSTAL_COST_REDUCTION;
    }

    private String resolveReceivedSkillUnlock(ServerPlayer player, String receivedSkill) {
        String canonical = APSkillLockManager.canonicalSkillKey(receivedSkill);
        APUnlockData unlockData = APUnlockData.get(player.getServer());
        String parent = VaultHuntersData.getSpecializationParent(canonical);

        if (parent != null && !APSkillLockManager.getUnlockedSkills(player).contains(parent)) {
            vhManager.unlockSkill(player, parent);
            unlockData.setPendingSpecializationSwap(parent, canonical);
            if (shouldAutoActivateUnlocks()) {
                vhManager.autoActivateSkillUnlock(player, parent);
            }
            player.sendMessage(
                    new TextComponent("[AP] Received " + displayUnlockName(canonical)
                            + " before " + displayUnlockName(parent)
                            + "; unlocked the base skill first. The base-skill item will grant this specialization.")
                            .withStyle(ChatFormatting.AQUA),
                    player.getUUID()
            );
            return parent;
        }

        if (parent == null) {
            String replacedSpecialization = unlockData.consumePendingSpecializationSwap(canonical);
            if (replacedSpecialization != null) {
                vhManager.unlockSkill(player, replacedSpecialization);
                if (shouldAutoActivateUnlocks()) {
                    vhManager.autoActivateSkillUnlock(player, replacedSpecialization);
                }
                return replacedSpecialization;
            }
        }

        vhManager.unlockSkill(player, canonical);
        if (shouldAutoActivateUnlocks()) {
            vhManager.autoActivateSkillUnlock(player, canonical);
        }
        return canonical;
    }

    private String resolveReceivedTalentUnlock(ServerPlayer player, long itemId, String receivedTalent) {
        String talentToUnlock = APSkillLockManager.canonicalTalentKey(receivedTalent);
        String[] progressiveTalents = PROGRESSIVE_TALENTS.get(itemId);
        if (progressiveTalents != null) {
            Set<String> unlocked = APSkillLockManager.getUnlockedTalents(player);
            for (String talent : progressiveTalents) {
                String canonical = APSkillLockManager.canonicalTalentKey(talent);
                if (!unlocked.contains(canonical)) {
                    talentToUnlock = canonical;
                    break;
                }
            }
        }
        vhManager.unlockTalent(player, talentToUnlock);
        return talentToUnlock;
    }

    private boolean shouldAutoActivateUnlocks() {
        return autoLearnSkillsEnabled;
    }

    private void handleLocationInfo(JsonObject packet) {
        if (!packet.has("locations") || !packet.get("locations").isJsonArray()) {
            return;
        }

        for (JsonElement element : packet.getAsJsonArray("locations")) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject info = element.getAsJsonObject();
            long locationId = info.has("location") ? info.get("location").getAsLong() : Long.MIN_VALUE;
            long itemId = info.has("item") ? info.get("item").getAsLong() : Long.MIN_VALUE;
            int receiverSlot = info.has("player") ? info.get("player").getAsInt() : slot;
            int flags = info.has("flags") ? info.get("flags").getAsInt() : 0;

            if (locationId == Long.MIN_VALUE || itemId == Long.MIN_VALUE) {
                continue;
            }

            String itemName = resolveItemNameForGame(itemId, playerGames.getOrDefault(receiverSlot, game));
            scoutedRewardsByLocation.put(locationId, itemName);
            scoutedRewardFlagsByLocation.put(locationId, flags);
            scoutedRewardReceiversByLocation.put(locationId, getPlayerName(receiverSlot));
            scoutedRewardIsLocalByLocation.put(locationId, receiverSlot == slot);
            updateCheckItemRewardInfo(locationId, itemName, flags, getPlayerName(receiverSlot), receiverSlot == slot);
            refreshOpenCheckTables();
            LOGGER.info("Scouted AP location {} -> {}", locationId, itemName);
        }
    }

    private void refreshOpenCheckTables() {
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu instanceof CheckTableMenu menu) {
                    menu.refreshShop();
                    menu.broadcastChanges();
                }
            }
        });
    }

    private void unlockModWithPrerequisites(ServerPlayer player, String modName) {
        String actualModName = "vhmod:automatic_genius".equals(modName)
                ? chooseAutomaticGeniusReplacement(player)
                : modName;

        for (String prerequisite : getModPrerequisites(player, actualModName)) {
            vhManager.unlockMod(player, prerequisite);
            vhManager.researchMod(player, prerequisite);
        }
        vhManager.unlockMod(player, actualModName);
        vhManager.researchMod(player, actualModName);
    }

    private String chooseAutomaticGeniusReplacement(ServerPlayer player) {
        List<String> candidates = new ArrayList<>(Arrays.asList(
                "vhmod:colossal_chests", "vhmod:simple_storage_network", "vhmod:drawers",
                "vhmod:mekanism_qio", "vhmod:refined_storage", "vhmod:applied_energistics",
                "vhmod:stack_upgrading", "vhmod:auto_refill", "vhmod:auto_feeding",
                "vhmod:double_pouches", "vhmod:belts", "vhmod:backpacks", "vhmod:big_backpacks",
                "vhmod:soul_harvester", "vhmod:junk_management", "vhmod:iron_generators",
                "vhmod:powah", "vhmod:flux_networks", "vhmod:thermal_dynamos",
                "vhmod:mekanism_generators", "vhmod:botania_flux_field", "vhmod:building_gadgets",
                "vhmod:weirding_gadgets", "vhmod:mining_gadgets", "vhmod:laser_bridges",
                "vhmod:digital_miner", "vhmod:entangled", "vhmod:botania", "vhmod:mekanism",
                "vhmod:thermal_expansion", "vhmod:create", "vhmod:waystones", "vhmod:torchmaster",
                "vhmod:trashcans", "vhmod:elevators", "vhmod:altar_automation", "vhmod:xnet",
                "vhmod:modular_routers", "vhmod:pipez", "vhmod:iron_furnaces", "vhmod:vault_filters",
                "vhmod:dark_utilities", "vhmod:easy_villagers", "vhmod:easy_piglins",
                "vhmod:botany_pots", "vhmod:snad", "vhmod:cagerium", "vhmod:mob_spawners",
                "vhmod:phytogenic_insulator", "vhmod:potions", "vhmod:mixtures", "vhmod:brews",
                "vhmod:vault_compass", "vhmod:map_markers", "vhmod:vault_map", "vhmod:vault_decks",
                "vhmod:augment_station"
        ));
        Collections.shuffle(candidates);
        Set<String> unlocked = APSkillLockManager.getUnlockedMods(player);
        for (String candidate : candidates) {
            if (!unlocked.contains(APSkillLockManager.canonicalModKey(candidate))) {
                LOGGER.info("Automatic Genius replaced with missing mod {}", candidate);
                return candidate;
            }
        }
        return "vhmod:automatic_genius";
    }

    private List<String> getModPrerequisites(ServerPlayer player, String modName) {
        return switch (modName) {
            case "vhmod:mekanism_qio", "vhmod:mekanism_generators", "vhmod:digital_miner" ->
                    List.of("vhmod:mekanism");
            case "vhmod:belts" -> List.of("vhmod:double_pouches");
            case "vhmod:backpacks" -> List.of("vhmod:double_pouches", "vhmod:belts");
            case "vhmod:big_backpacks" -> List.of("vhmod:double_pouches", "vhmod:belts", "vhmod:backpacks");
            case "vhmod:thermal_dynamos", "vhmod:phytogenic_insulator" -> List.of("vhmod:thermal_expansion");
            case "vhmod:botania_flux_field" -> List.of("vhmod:botania");
            case "vhmod:altar_automation" -> hasMod(player, "vhmod:refined_storage")
                    ? List.of("vhmod:refined_storage")
                    : List.of("vhmod:applied_energistics");
            case "vhmod:mixtures" -> List.of("vhmod:potions");
            case "vhmod:brews" -> List.of("vhmod:potions", "vhmod:mixtures");
            default -> List.of();
        };
    }

    private boolean hasMod(ServerPlayer player, String modName) {
        return player != null && APSkillLockManager.getUnlockedMods(player).contains(APSkillLockManager.canonicalModKey(modName));
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
            broadcastStatusToPlayers("[AP] " + message, ChatFormatting.AQUA);
        }
    }

    private void handlePrintJSON(JsonObject packet) {
        if ("Hint".equals(getString(packet, "type"))) {
            handleHintPrintJson(packet);
            sendFormattedItemMessage(packet);
            return;
        }

        if ("ItemSend".equals(getString(packet, "type"))) {
            sendFormattedItemMessage(packet);
            return;
        }

        if (packet.has("data") && packet.get("data").isJsonArray()) {
            MutableComponent message = formatPrintJsonMessage(packet);
            if (!message.getString().isBlank()) {
                broadcastComponentToPlayers(colored("[AP] ", ChatFormatting.AQUA).append(message));
            }
        }
    }

    private void handleHintPrintJson(JsonObject packet) {
        if (!packet.has("item") || !packet.get("item").isJsonObject()) {
            return;
        }

        JsonObject item = packet.getAsJsonObject("item");
        long locationId = getLong(item, "location", Long.MIN_VALUE);
        long itemId = getLong(item, "item", Long.MIN_VALUE);
        int receiverSlot = getInt(packet, "receiving", getInt(item, "receiving_player", slot));
        int findingSlot = getInt(item, "player", getInt(item, "finding_player", slot));
        int flags = getInt(item, "flags", getInt(item, "item_flags", 0));

        if (locationId == Long.MIN_VALUE || itemId == Long.MIN_VALUE) {
            return;
        }

        markCheckRewardHinted(locationId);

        String itemName = resolveItemNameForGame(itemId, playerGames.getOrDefault(receiverSlot, game));
        String receiverName = getPlayerName(receiverSlot);
        boolean localReward = receiverSlot == slot;
        scoutedRewardsByLocation.put(locationId, itemName);
        scoutedRewardFlagsByLocation.put(locationId, flags);
        scoutedRewardReceiversByLocation.put(locationId, receiverName);
        scoutedRewardIsLocalByLocation.put(locationId, localReward);
        updateCheckItemRewardInfo(locationId, itemName, flags, receiverName, localReward);
        refreshOpenCheckTables();
        LOGGER.info("Recorded AP hint for location {} from player {}", locationId, findingSlot);
    }

    private MutableComponent formatPrintJsonMessage(JsonObject packet) {
        MutableComponent message = new TextComponent("");
        for (JsonElement element : packet.getAsJsonArray("data")) {
            if (element.isJsonObject()) {
                message.append(formatPrintJsonPart(element.getAsJsonObject()));
            }
        }
        return message;
    }

    private MutableComponent formatPrintJsonPart(JsonObject part) {
        String text = getString(part, "text");
        String type = getString(part, "type");
        if (type == null || type.isBlank()) {
            return colored(text, ChatFormatting.AQUA);
        }

        switch (type) {
            case "player_id":
                return colored(resolvePlayerName(text), ChatFormatting.YELLOW);
            case "item_id": {
                int receiverSlot = getInt(part, "player", slot);
                long itemId = parsePositiveLong(text, Long.MIN_VALUE);
                return colored(resolveItemName(text, receiverSlot), getItemColor(part, itemId));
            }
            case "location_id": {
                int senderSlot = getInt(part, "player", slot);
                return colored(resolveLocationName(text, senderSlot), ChatFormatting.GREEN);
            }
            default:
                return colored(text, ChatFormatting.AQUA);
        }
    }

    private void sendFormattedItemMessage(JsonObject packet) {
        JsonObject itemPart = findPrintPart(packet, "item_id", 0);
        JsonObject locationPart = findPrintPart(packet, "location_id", 0);
        String rawItemName = getString(itemPart, "text");
        String rawSenderName = findPrintPartText(packet, "player_id", 0);
        String rawReceiverName = findPrintPartText(packet, "player_id", 1);
        String rawLocationName = getString(locationPart, "text");
        int receivingSlot = packet.has("receiving") ? packet.get("receiving").getAsInt() : -1;
        int senderSlot = getInt(locationPart, "player", parsePositiveInt(rawSenderName, -1));
        int receiverSlot = getInt(itemPart, "player", receivingSlot);
        if (receiverSlot < 0) {
            receiverSlot = parsePositiveInt(rawReceiverName, -1);
        }
        long itemId = parsePositiveLong(rawItemName, Long.MIN_VALUE);
        String itemName = resolveItemName(rawItemName, receiverSlot);
        TextColor itemColor = getItemColor(itemPart, itemId);
        String senderName = senderSlot >= 0 ? getPlayerName(senderSlot) : resolvePlayerName(rawSenderName);
        String receiverName = receiverSlot >= 0 ? getPlayerName(receiverSlot) : resolvePlayerName(rawReceiverName);
        String locationName = resolveLocationName(rawLocationName, senderSlot);

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
        if (receivingHere && (sentByHere || selfFound)) {
            message = colored(localSlotDisplayName(), ChatFormatting.YELLOW)
                    .append(colored(" found their ", ChatFormatting.WHITE))
                    .append(quoted(itemName, itemColor, false));
        } else if (receivingHere) {
            message = colored(senderName, ChatFormatting.YELLOW)
                    .append(colored(" sent ", ChatFormatting.WHITE))
                    .append(quoted(itemName, itemColor, false))
                    .append(colored(" to you", ChatFormatting.WHITE));
        } else if (selfFound || found) {
            message = colored(senderName, ChatFormatting.YELLOW)
                    .append(colored(" found their ", ChatFormatting.WHITE))
                    .append(quoted(itemName, itemColor, false));
        } else {
            message = colored(senderName, ChatFormatting.YELLOW)
                    .append(colored(" sent ", ChatFormatting.WHITE))
                    .append(colored(itemName, itemColor))
                    .append(colored(" to ", ChatFormatting.WHITE))
                    .append(colored(receiverName, ChatFormatting.YELLOW));
        }

        if (locationName != null && !locationName.isBlank()) {
            message.append(colored(" (", ChatFormatting.WHITE))
                    .append(colored(locationName, ChatFormatting.GREEN))
                    .append(colored(")", ChatFormatting.WHITE));
        }

        broadcastComponentToPlayers(message);
    }

    private JsonObject findPrintPart(JsonObject packet, String type, int occurrence) {
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
                    return part;
                }
                seen++;
            }
        }
        return null;
    }

    private int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private long getLong(JsonObject object, String key, long fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return object.get(key).getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private TextColor getItemColor(JsonObject itemPart, long itemId) {
        int flags = getInt(itemPart, "flags", 0);
        return getItemColor(flags, itemId);
    }

    private TextColor getItemColor(int flags, long itemId) {
        if ((flags & 4) != 0) {
            return legacyTextColor(ChatFormatting.RED);
        }
        if ((flags & 1) != 0) {
            return progressionTextColor();
        }
        if ((flags & 2) != 0) {
            return legacyTextColor(ChatFormatting.AQUA);
        }
        return getLocalItemColor(itemId);
    }

    private TextColor getLocalItemColor(long itemId) {
        if (getProgressionItemName(itemId) != null || VaultHuntersData.getLocationNameById(itemId) != null) {
            return progressionTextColor();
        }
        if (itemId >= 33700L && itemId < 33800L) {
            return legacyTextColor(ChatFormatting.AQUA);
        }
        if (APTrapManager.trapIdForItem(itemId) != null) {
            return legacyTextColor(ChatFormatting.RED);
        }
        return legacyTextColor(ChatFormatting.AQUA);
    }

    private TextColor progressionTextColor() {
        return TextColor.fromRgb(PROGRESSION_RGB);
    }

    private TextColor legacyTextColor(ChatFormatting color) {
        TextColor textColor = TextColor.fromLegacyFormat(color);
        return textColor != null ? textColor : TextColor.fromRgb(color.getColor() != null ? color.getColor() : 0xFFFFFF);
    }

    private void sendReceivedItemMessage(ServerPlayer player, long locationId, int sendingPlayer, String itemName) {
        sendReceivedItemMessage(player, locationId, sendingPlayer, itemName, legacyTextColor(ChatFormatting.AQUA));
    }

    private void sendReceivedItemMessage(ServerPlayer player, long locationId, int sendingPlayer, String itemName, TextColor itemColor) {
        String senderName = getPlayerName(sendingPlayer);
        MutableComponent message;
        if (sendingPlayer == slot || senderName == null || senderName.isBlank() || senderName.equals(slotName)) {
            message = colored(localSlotDisplayName(), ChatFormatting.YELLOW)
                    .append(colored(" found their ", ChatFormatting.WHITE))
                    .append(quoted(itemName, itemColor, false));
        } else {
            message = colored(senderName, ChatFormatting.YELLOW)
                    .append(colored(" sent ", ChatFormatting.WHITE))
                    .append(quoted(itemName, itemColor, false))
                    .append(colored(" to you", ChatFormatting.WHITE));
        }

        appendSourceLocation(message, locationId, sendingPlayer);
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
        if (vhManager != null) {
            vhManager.markLocationChecked(locationId);
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

    public boolean isLocationChecked(long locationId) {
        return checkedLocations.contains(locationId);
    }

    public void scoutLocation(ServerPlayer player, long locationId) {
        scoutLocation(player, locationId, false);
    }

    public void hintUsefulCheckTablePage(ServerPlayer player, int selectedSphere, int totalChecks) {
        player.sendMessage(new TextComponent("Check Table reward hints are disabled; externally hinted checks will reveal full details").withStyle(ChatFormatting.YELLOW), player.getUUID());
    }

    private void scoutLocation(ServerPlayer player, long locationId, boolean createAsHint) {
        if (!isConnected() || webSocket == null) {
            return;
        }

        if (player != null) {
            scoutTargetsByLocation.put(locationId, player.getUUID());
        }

        JsonObject packet = new JsonObject();
        packet.addProperty("cmd", "LocationScouts");
        JsonArray locations = new JsonArray();
        locations.add((int) locationId);
        packet.add("locations", locations);
        packet.addProperty("create_as_hint", createAsHint);

        JsonArray wrapper = new JsonArray();
        wrapper.add(packet);

        webSocket.sendText(gson.toJson(wrapper), true);
        LOGGER.info("Requested AP scout for location {}{}", locationId, createAsHint ? " as hint" : "");
    }

    private ServerPlayer playerFromScoutTarget(long locationId) {
        if (server == null) {
            return null;
        }
        UUID playerId = scoutTargetsByLocation.get(locationId);
        return playerId != null ? server.getPlayerList().getPlayer(playerId) : getCurrentPlayer();
    }

    public String getScoutedRewardName(long locationId) {
        return scoutedRewardsByLocation.get(locationId);
    }

    public int getScoutedRewardFlags(long locationId) {
        return scoutedRewardFlagsByLocation.getOrDefault(locationId, 0);
    }

    private boolean isProgressionOrUseful(int flags) {
        return (flags & 3) != 0;
    }

    public String getScoutedRewardReceiverName(long locationId) {
        return scoutedRewardReceiversByLocation.get(locationId);
    }

    public boolean isScoutedRewardLocal(long locationId) {
        return scoutedRewardIsLocalByLocation.getOrDefault(locationId, false);
    }

    public boolean isCheckRewardHinted(long locationId) {
        if (server == null || !server.isSameThread()) {
            return false;
        }
        return server != null && APProgressData.get(server).hasCheckTableHint(locationId);
    }

    public String getShopCheckRewardDisplay() {
        return shopCheckRewardDisplay;
    }

    public String getBountyCheckRewardDisplay() {
        return bountyCheckRewardDisplay;
    }

    private String readRewardDisplayMode(JsonObject slotData, String key, String fallback) {
        if (!slotData.has(key)) {
            return fallback;
        }
        try {
            String value = slotData.get(key).getAsString().trim().toLowerCase(Locale.ROOT);
            if (value.startsWith("option_")) {
                value = value.substring("option_".length());
            }
            return switch (value) {
                case "0" -> "all";
                case "1" -> "classification";
                case "2" -> "nothing";
                case "all", "classification", "nothing" -> value;
                default -> fallback;
            };
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void updateCheckItemRewardInfo(long locationId, String itemName, int flags, String receiverName, boolean localReward) {
        if (server == null || itemName == null || itemName.isBlank()) {
            return;
        }
        if (!server.isSameThread()) {
            runOnServerThread(() -> updateCheckItemRewardInfo(locationId, itemName, flags, receiverName, localReward));
            return;
        }

        UUID targetId = scoutTargetsByLocation.get(locationId);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (targetId != null && !player.getUUID().equals(targetId)) {
                continue;
            }

            for (ItemStack stack : player.getInventory().items) {
                updateStackRewardInfo(stack, locationId, itemName, flags, receiverName, localReward);
            }
            for (ItemStack stack : player.getInventory().offhand) {
                updateStackRewardInfo(stack, locationId, itemName, flags, receiverName, localReward);
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        APBountyCheckManager.updateRewardInfo(server, locationId, itemName, flags, receiverName, localReward);
    }

    private void markCheckRewardHinted(long locationId) {
        if (!isChestCheckLocation(locationId) && !isBountyCheckLocation(locationId)) {
            return;
        }
        runOnServerThread(() -> APProgressData.get(server).markCheckTableHinted(locationId));
    }

    private void updateStackRewardInfo(ItemStack stack, long locationId, String itemName, int flags, String receiverName, boolean localReward) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        if (stack.getTag().getLong("LocationID") != locationId) {
            return;
        }
        stack.getOrCreateTag().putString("RewardName", itemName);
        stack.getOrCreateTag().putInt("ItemFlags", flags);
        stack.getOrCreateTag().putBoolean("RewardForLocalSlot", localReward);
        if (receiverName != null && !receiverName.isBlank()) {
            stack.getOrCreateTag().putString("ReceiverSlotName", receiverName);
        }
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
        String[] skills = {
                "heal", "dash", "nova", "execute", "ghost_walk",
                "mega_jump", "rampage", "smite", "empower", "vein_miner",
                "hunter", "farmer", "mana_shield", "retribution", "implode",
                "taunt", "stonefall", "totem", "javelin", "shell",
                "fireball", "storm_arrow", "battle_cry", "ice_bolt", "arcane",
                "earthquake", "chain_lightning", "unknown", "grenade"
        };

        int index = (int)(itemId - 43000);
        return index >= 0 && index < skills.length ? skills[index] : "unknown";
    }

    private String getTalentName(long itemId) {
        String[] talents = {
                "intelligence", "haste", "strength", "speed", "reach", "purist",
                "stone_skin", "critical_strike", "fatal_strike_chance",
                "fatal_strike_damage", "thorns_chance", "thorns_damage",
                "horde_mastery", "unknown", "unknown",
                "unknown", "last_stand", "berserking", "sorcery",
                "prime_amplification", "bountiful_harvest", "lightning_damage",
                "lightning_stun", "treasure_seeker", "stoneskin",
                "lingering_fumes", "clean_escape", "witchery", "methodical",
                "depleted", "fatal_strike", "fatal_strike_ii", "mana_harvest",
                "heart_fragments", "mana_steal", "mana_steal_ii",
                "life_steal", "life_steal_ii", "cleave", "explosive",
                "prudent", "alchemist", "blizzard", "frostbite", "daze",
                "nucleus", "hunters_instinct", "arcana", "blazing",
                "lucky_momentum", "frenzy", "voltaic_impact",
                "axe_specialisation", "sword_specialisation", "blood_rush",
                "blight", "toxic_reaction", "unknown",
                "unknown", "unknown", "farmer_twerker", "might_ii",
                "battle_trance", "intelligence_ii", "haste_ii", "speed_ii",
                "prudent_ii", "ethereal", "quickening", "trap_disarm",
                "treasure_sense", "executioner", "assassin_mastery",
                "dungeon_mastery", "champion_mastery"
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
                "map_markers", "vault_map", "vault_decks",
                "augment_station"
        };

        int index = (int)(itemId - 43200);
        return index >= 0 && index < mods.length ? mods[index] : "unknown";
    }

    private String getExpertiseName(long itemId) {
        String[] expertises = {
                "lucky_altar", "fortuitous_finesse", "fortunate",
                "experienced", "infuser", "mystic",
                "trinketer", "divine", "unbreakable",
                "marketer", "bounty_hunter", "angel",
                "jeweler", "artisan", "bartering",
                "companion_loyalty"
        };

        int index = (int)(itemId - 43300);
        return index >= 0 && index < expertises.length ? expertises[index] : "unknown";
    }

    private void giveFillerItem(ServerPlayer player, long itemId, String sourcePlayerName, long sourceLocationId, int sendingPlayer) {
        int index = (int)(itemId - 33700);

        // Map to actual VH items/commands [itemId, baseCount]
        String[][] fillers = {
                {"the_vault:cooked_vault_steak", "8"},
                {"the_vault:chromatic_iron_ingot", "24"},
                {"minecraft:shulker_box", "1"},
                {"minecraft:experience_bottle", "20"},
                {"minecraft:emerald", "50"},
                {"minecraft:ender_pearl", "2"},
                {"sophisticatedbackpacks:backpack", "1"},
                {"the_vault:bounty_pearl", "10"},
                {"the_vault:gemstone", "2"},
                {"the_vault:chromatic_steel_ingot", "4"},
                {"the_vault:vault_gold", "1"},
                {"the_vault:vault_silver", "1"},
                {"the_vault:vault_bronze", "32"},
                {"minecraft:diamond", "5"},
                {"the_vault:vault_alloy", "1"},
                {"the_vault:wild_focus", "1"},
                {"the_vault:vault_plating", "9"},
                {"the_vault:vault_scrap", "1"},
                {"the_vault:vault_gold", "4"},
                {"the_vault:vault_diamond", "1"},
                {"the_vault:plain_burger", "1"},
                {"the_vault:crystal_seal_empty", "1"},
                {"the_vault:silver_scrap", "1"},
                {"the_vault:soul_shard", "1"},
                {"the_vault:helmet", "1"},
                {"the_vault:chestplate", "1"},
                {"the_vault:leggings", "1"},
                {"the_vault:boots", "1"},
                {"the_vault:mod_box", "1"},
                {"minecraft:diamond", "20"},
                {"the_vault:phoenix_feather", "1"},
                {"the_vault:vault_catalyst_fragment", "64"},
                {"the_vault:inscription_piece", "32"},
                {"the_vault:unidentified_artifact", "1"},
                {"the_vault:trinket", "1"},
                {"the_vault:vault_diamond", "16"},
                {"the_vault:vault_gold", "20"},
                {"the_vault:vault_silver", "4"},
                {"the_vault:vault_silver", "20"},
                {"the_vault:gem_pog", "1"},
                {"minecraft:netherite_ingot", "2"},
                {"the_vault:wardrobe", "1"},
                {"the_vault:vault_bronze", "64"},
                {"the_vault:gem_echo", "1"},
                {"the_vault:sour_orange", "1"},
                {"minecraft:bamboo", "1"},
                {"the_vault:vault_gold", "64"},
                {"the_vault:vault_silver", "64"},
                {"the_vault:vault_bronze", "81"},
                {"the_vault:neuralizer", "1"},
                {"the_vault:perfect_echo_gem", "1"},
                {"the_vault:lost_bounty", "1"},
                {"the_vault:ember", "1"},
                {"the_vault:omega_pog", "1"},
                {"minecraft:air", "1"}
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
                giveVaultGear(player, gearType, sourcePlayerName, sourceLocationId, sendingPlayer);
                return;
            }

            float[] multipliers = {1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
            float multiplier = multipliers[new Random().nextInt(multipliers.length)];
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

            String rewardName = getFillerRewardName(itemId);
            sendReceivedItemMessage(player, sourceLocationId, sendingPlayer,
                    rewardName != null ? rewardName : displayStackName(itemId_str, finalCount),
                    getItemColor(0, itemId));
        } else {
            LOGGER.warn("Invalid filler item index: {}", index);
        }
    }


    private void giveVaultGear(ServerPlayer player, String gearType, String sourcePlayerName, long sourceLocationId, int sendingPlayer) {
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

            sendReceivedItemMessage(player, sourceLocationId, sendingPlayer, String.format("Vault %s (Level %d)", gearType, playerLevel), legacyTextColor(ChatFormatting.AQUA));

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

    private void giveProgressiveArtifact(ServerPlayer player, long sourceLocationId, int sendingPlayer, int itemFlags) {
        int artifactProgress = APProgressData.get(player.getServer()).claimNextArtifactProgress();
        int artifactOrder = APArtifactLayout.orderForProgress(artifactProgress, artifactRequirement);
        if (artifactOrder <= 0 || artifactOrder > 25) {
            LOGGER.warn("Cannot give AP artifact for invalid artifact progress {} with requirement {}", artifactProgress, artifactRequirement);
            return;
        }

        DiscoveredArtifactsData.get(player.getServer()).discoverArtifactAndBroadcast(artifactOrder, player);

        sendReceivedItemMessage(
                player,
                sourceLocationId,
                sendingPlayer,
                "Artifact " + artifactProgress + "/" + artifactRequirement,
                getItemColor(itemFlags, VaultHuntersData.ITEM_PROGRESSIVE_ARTIFACT)
        );
        LOGGER.info("Recorded AP artifact progress {}/{} as artifact order {} for {}", artifactProgress, artifactRequirement, artifactOrder, player.getName().getString());
    }
    // ========== UTILITY ==========

    private String getString(JsonObject object, String key) {
        if (object == null) {
            return "";
        }
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
        String localName = resolveLocalItemName(itemId);
        if (localName != null) {
            return localName;
        }

        if (gameName != null) {
            Map<Long, String> dataPackageItems = itemNamesByGame.get(gameName);
            if (dataPackageItems != null && dataPackageItems.containsKey(itemId)) {
                return dataPackageItems.get(itemId);
            }
        }
        if (itemId >= 44000L && itemId < 44500L) {
            return getCheckTableLocationName(itemId);
        }
        return "Unknown Item [" + itemId + "]";
    }

    private String resolveLocalItemName(long itemId) {
        String progressionName = getProgressionItemName(itemId);
        if (progressionName != null) {
            return progressionName;
        }

        String unlockName = VaultHuntersData.getLocationNameById(itemId);
        if (unlockName != null) {
            return displayUnlockName(unlockName);
        }

        if (itemId >= 33700L && itemId < 33800L) {
            String fillerName = getFillerRewardName(itemId);
            return fillerName != null ? fillerName : "Useful Vault Supplies";
        }
        String trapId = APTrapManager.trapIdForItem(itemId);
        if (trapId != null) {
            return APTrapManager.displayName(trapId);
        }
        return null;
    }

    private String getFillerRewardName(long itemId) {
        int index = (int)(itemId - 33700L);
        return index >= 0 && index < FILLER_REWARD_NAMES.length ? FILLER_REWARD_NAMES[index] : null;
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

        if (senderSlot == slot || game.equals(gameName)) {
            String localName = VaultHuntersData.getLocationNameById(locationId);
            if (localName != null) {
                return displayLocationName(localName);
            }
            if (locationId >= 44000L && locationId < 44500L) {
                return getCheckTableLocationName(locationId);
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
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_ARTIFACT) return "Artifact";
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_VAULT_TIME) return "Progressive Vault Time";
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_BOUNTY) return "Progressive Bounty";
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_CRYSTAL_COST_REDUCTION) return "Progressive Crystal Cost Reduction";
        if (itemId == VaultHuntersData.ITEM_PROGRESSIVE_BACKPACK_MOD) return "Progressive Backpack Mod";
        if (itemId == VaultHuntersData.ITEM_SKILL_POINT) return "Skill Point";
        if (itemId == VaultHuntersData.ITEM_EXPERTISE_POINT) return "Expertise Point";
        if (itemId == VaultHuntersData.ITEM_KNOWLEDGE_STAR) return "Knowledge Point";
        return null;
    }

    private boolean isLocalVhapItem(long itemId) {
        if (itemId == Long.MIN_VALUE) {
            return false;
        }
        if (itemId >= 33700L && itemId < 33800L) {
            return true;
        }
        if (APTrapManager.trapIdForItem(itemId) != null) {
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
        String displayName = UNLOCK_DISPLAY_NAMES.get(unlockName);
        if (displayName != null) {
            return displayName;
        }
        int separator = unlockName.indexOf(':');
        String name = separator >= 0 ? unlockName.substring(separator + 1) : unlockName;
        return displayItemName("vhap:" + name);
    }

    private String displayLocationName(String locationName) {
        if (locationName.startsWith("vhskill:")) {
            return "Skill: " + displayUnlockName(locationName);
        }
        if (locationName.startsWith("vhtalent:")) {
            return "Talent: " + displayUnlockName(locationName);
        }
        if (locationName.startsWith("vhexpertise:")) {
            return "Expertise: " + displayUnlockName(locationName);
        }
        if (locationName.startsWith("vhmod:")) {
            return "Mod: " + displayUnlockName(locationName);
        }
        return locationName;
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

    private String displayStackName(String itemId, int count) {
        if ("minecraft:experience_bottle".equals(itemId)) {
            return count == 1 ? "XP Bottle" : "XP Bottles";
        }

        String itemName = displayItemName(itemId);
        if (count == 1) {
            return itemName;
        }
        return itemName.endsWith("s") ? itemName : itemName + "s";
    }

    private MutableComponent quoted(String text, ChatFormatting color, boolean bold) {
        MutableComponent component = new TextComponent(text).withStyle(color);
        if (bold) {
            component.withStyle(ChatFormatting.BOLD);
        }
        return component;
    }

    private MutableComponent quoted(String text, TextColor color, boolean bold) {
        MutableComponent component = new TextComponent(text)
                .withStyle(style -> style.withColor(color));
        if (bold) {
            component.withStyle(ChatFormatting.BOLD);
        }
        return component;
    }

    private MutableComponent colored(String text, ChatFormatting color) {
        return new TextComponent(text).withStyle(color);
    }

    private MutableComponent colored(String text, TextColor color) {
        return new TextComponent(text).withStyle(style -> style.withColor(color));
    }

    private void appendSourceLocation(MutableComponent message, long locationId, int senderSlot) {
        if (locationId <= 0L) {
            return;
        }

        String locationName = resolveLocationName(Long.toString(locationId), senderSlot);
        if (locationName == null || locationName.isBlank()) {
            return;
        }

        message.append(colored(" (", ChatFormatting.WHITE))
                .append(colored(locationName, ChatFormatting.GREEN))
                .append(colored(")", ChatFormatting.WHITE));
    }

    private String localSlotDisplayName() {
        if (slotName != null && !slotName.isBlank()) {
            return slotName;
        }
        ServerPlayer player = getCurrentPlayer();
        return player != null ? player.getName().getString() : "Player";
    }

    private void sendFoundItemMessage(ServerPlayer player, long locationId, String itemName) {
        MutableComponent message = colored(localSlotDisplayName(), ChatFormatting.YELLOW)
                .append(colored(" found their ", ChatFormatting.WHITE))
                .append(quoted(itemName, getItemColor(0, Long.MIN_VALUE), false));
        appendSourceLocation(message, locationId, slot);
        broadcastComponentToPlayers(message);
    }

    private void sendFoundUnlockMessage(long locationId, String type, String unlockName) {
        MutableComponent message = colored(localSlotDisplayName(), ChatFormatting.YELLOW)
                .append(colored(" found their ", ChatFormatting.WHITE))
                .append(colored(capitalize(type), ChatFormatting.DARK_PURPLE))
                .append(colored(": ", ChatFormatting.WHITE))
                .append(colored(displayUnlockName(unlockName), ChatFormatting.DARK_PURPLE));
        appendSourceLocation(message, locationId, slot);
        broadcastComponentToPlayers(message);
    }

    private void sendReceivedUnlockMessage(long locationId, int sendingPlayer, String type, String unlockName) {
        String senderName = getPlayerName(sendingPlayer);
        MutableComponent message;
        if (sendingPlayer == slot || senderName == null || senderName.isBlank() || senderName.equals(slotName)) {
            message = colored(localSlotDisplayName(), ChatFormatting.YELLOW)
                    .append(colored(" found their ", ChatFormatting.WHITE))
                    .append(colored(capitalize(type), ChatFormatting.DARK_PURPLE))
                    .append(colored(": ", ChatFormatting.WHITE))
                    .append(colored(displayUnlockName(unlockName), ChatFormatting.DARK_PURPLE));
        } else {
            message = colored(senderName, ChatFormatting.YELLOW)
                    .append(colored(" sent ", ChatFormatting.WHITE))
                    .append(colored(capitalize(type), ChatFormatting.DARK_PURPLE))
                    .append(colored(": ", ChatFormatting.WHITE))
                    .append(colored(displayUnlockName(unlockName), ChatFormatting.DARK_PURPLE))
                    .append(colored(" to you", ChatFormatting.WHITE));
        }

        appendSourceLocation(message, locationId, sendingPlayer);
        broadcastComponentToPlayers(message);
    }

    private boolean isChestCheckLocation(long locationId) {
        return locationId >= 44000L && locationId < 44500L;
    }

    private boolean isBountyCheckLocation(long locationId) {
        return (locationId >= 43650L && locationId < 43700L)
                || (locationId >= 44500L && locationId < 44650L);
    }

    private String getCheckTableLocationName(long locationId) {
        long index = locationId - 44000L;
        long sphere = index / 25L;
        long slot = (index % 25L) + 1L;
        long minLevel = sphere * 5L + 1L;
        long maxLevel = minLevel + 4L;
        return "Check Table Lv " + minLevel + "-" + maxLevel + " - Slot " + slot;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void broadcastComponentToPlayers(MutableComponent message) {
        if (server == null) return;

        if (!server.isSameThread()) {
            MutableComponent copy = message.copy();
            server.execute(() -> broadcastComponentToPlayers(copy));
            return;
        }

        LOGGER.info("[AP Chat] {}", message.getString());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendMessage(message.copy(), player.getUUID());
        }
    }

    private ServerPlayer getCurrentPlayer() {
        if (server == null) return null;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return null;
        }

        if (targetPlayerId != null) {
            for (ServerPlayer player : players) {
                if (player.getUUID().equals(targetPlayerId)) {
                    return player;
                }
            }

            LOGGER.warn("Preferred AP reward target {} is not online; falling back to slot-name lookup", targetPlayerId);
        }

        if (slotName != null && !slotName.isBlank()) {
            for (ServerPlayer player : players) {
                if (player.getGameProfile().getName().equalsIgnoreCase(slotName)) {
                    return player;
                }
                if (player.getName().getString().equalsIgnoreCase(slotName)) {
                    return player;
                }
            }

            LOGGER.warn("No online Minecraft player matched AP slot '{}'; using {} as fallback",
                    slotName, players.get(0).getGameProfile().getName());
        }

        return players.get(0);
    }

    private void sendConnectedMessageToPlayers() {
        broadcastStatusToPlayers("[AP] Connected as " + slotName, ChatFormatting.GREEN);
    }

    private void broadcastStatusToPlayers(String message, ChatFormatting color) {
        if (server == null) return;

        if (!server.isSameThread()) {
            server.execute(() -> broadcastStatusToPlayers(message, color));
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendMessage(
                    new TextComponent(message).withStyle(color),
                    player.getUUID()
            );
        }
    }

}
