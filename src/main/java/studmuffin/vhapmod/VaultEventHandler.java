package studmuffin.vhapmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import iskallia.vault.event.event.VaultJoinEvent;
import iskallia.vault.event.event.BountyCompleteEvent;
import iskallia.vault.entity.entity.VaultFighterEntity;
import iskallia.vault.world.data.ServerVaults;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studmuffin.vhapmod.item.ArchipelaPouchItem;
import studmuffin.vhapmod.item.ModItems;

import java.util.Locale;
import java.util.List;
import java.util.Random;

import static studmuffin.vhapmod.APSkillLockManager.syncToClient;

/**
 * Listens for Forge events and VH-specific events to track progression
 */
@Mod.EventBusSubscriber(modid = "apvaulthuntersmod")
public class VaultEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static VaultHuntersManager manager;
    private static final Random RANDOM = new Random();

    public static void setManager(VaultHuntersManager vhManager) {
        manager = vhManager;
    }

    // ==================== FORGE EVENTS ====================

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        ItemEntity itemEntity = event.getItem();
        ItemStack pickedUp = itemEntity.getItem();
        if (!pickedUp.is(ModItems.ARCHIPELA_BITS.get()) || pickedUp.isEmpty()) {
            return;
        }

        int remaining = pickedUp.getCount();
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.ARCHIPELA_POUCH.get())) {
                remaining = ArchipelaPouchItem.addBits(stack, remaining);
            }
        }

        if (remaining != pickedUp.getCount()) {
            player.getInventory().setChanged();
            pickedUp.setCount(remaining);
            if (remaining <= 0) {
                itemEntity.discard();
                event.setCanceled(true);
            }
        }
    }

    /**
     * Track when players log in - connect to AP and check their VH data
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player {} logged in - checking VH progression", player.getName().getString());

            // Load player's unlocked skills/talents/expertises/mods from disk
            APSkillLockManager.loadPlayer(player);

            // Connect to AP server
            /*
            APIntegration apIntegration = VaultHuntersAPMod.getAPIntegration();
            if (apIntegration != null) {
                apIntegration.connectToAP(player, player.getName().getString());
            }
            */
            // Log their current progression (for testing)
            VHDataReader.logPlayerProgression(player);

            // Check player progression against AP
            checkPlayerProgression(player);
            if (manager != null) {
                manager.restoreWorldProgression(player.getServer());
                manager.grantStartingKitIfEnabled(player);
            }

            APWebSocketClient apClient = VaultHuntersAPMod.getAPClient();
            if (apClient != null) {
                apClient.registerKnownPlayer(player);
                apClient.claimTargetPlayerIfUnset(player);
                apClient.flushPendingFillerItems(player);
                apClient.flushPendingReceivedItems();
            }
            if (APChatModeManager.load(player)) {
                player.sendMessage(
                        new net.minecraft.network.chat.TextComponent(
                                "[AP] Restored Archipelago chat mode. Use /mcchat to return to Minecraft chat."
                        ).withStyle(net.minecraft.ChatFormatting.AQUA),
                        player.getUUID()
                );
            }
            DeathLinkCurseManager.applyPendingCurses(player);

            // Sync Client To Server
            syncToClient(player);
        }
    }

    /**
     * Clean up when players log out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player {} logged out", player.getName().getString());

            VHProgressionTracker.onPlayerLogout(player);

            if (player.getServer() != null) {
                player.getServer().execute(() ->
                        VaultHuntersAPMod.disconnectFromAPIfNoPlayersOnline(player.getServer()));
            }
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!APChatModeManager.isUsingAPChat(player.getUUID())) {
            return;
        }

        event.setCanceled(true);
        APWebSocketClient client = VaultHuntersAPMod.getAPClient();
        if (client == null || !client.sendChat(event.getMessage())) {
            player.sendMessage(
                    new net.minecraft.network.chat.TextComponent(
                            "[AP] Message not sent: disconnected. Use /mcchat to return to Minecraft chat."
                    ).withStyle(net.minecraft.ChatFormatting.RED),
                    player.getUUID()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof ServerPlayer player) {
            APWebSocketClient client = VaultHuntersAPMod.getAPClient();
            if (client != null) {
                client.onPlayerDeath(player);
            }
        }
    }

    @SubscribeEvent
    public static void onBountyComplete(BountyCompleteEvent event) {
        VaultHuntersManager activeManager = manager != null ? manager : VaultHuntersAPMod.getManager();
        if (!(event.getPlayer() instanceof ServerPlayer player) || activeManager == null) {
            return;
        }
        activeManager.onBountyCompleted(player);
    }

    @SubscribeEvent
    public static void onVaultJoin(VaultJoinEvent event) {
        ServerPlayer player = event.getPlayer();
        DeathLinkCurseManager.applyPendingCurses(player);
        APTrapManager.armVaultTrapTimer(player, event.getVault());
        APBoonManager.rollRandomVaultBoon(player);
        APBlessingManager.activatePendingBlessings(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 100 != 0) {
            return;
        }
        APTrapManager.tickVaultTraps(player);
        APTrapManager.applyPendingCrystalCostTrap(player);
        APBoonManager.clearIfOutsideVault(player);
        APBlessingManager.clearIfOutsideVault(player);
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof VaultFighterEntity fighter) || event.getWorld().isClientSide()) {
            return;
        }
        if (!ServerVaults.isInVault(event.getWorld())
                && !"the_vault".equals(event.getWorld().dimension().location().getNamespace())
                && !event.getWorld().dimension().location().getPath().contains("vault")) {
            return;
        }

        APWebSocketClient client = VaultHuntersAPMod.getAPClient();
        if (client == null || fighter.hasCustomName()) {
            return;
        }
        List<String> names = client.getKnownPlayerNames();
        if (names.isEmpty()) {
            return;
        }

        fighter.setCustomName(new TextComponent(names.get(RANDOM.nextInt(names.size()))));
        fighter.setCustomNameVisible(true);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        VaultHuntersManager activeManager = manager != null ? manager : VaultHuntersAPMod.getManager();
        if (!(event.getPlayer() instanceof ServerPlayer player) || activeManager == null) {
            return;
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
        if (blockId == null || !"the_vault".equals(blockId.getNamespace())) {
            return;
        }

        String path = blockId.getPath().toLowerCase(Locale.ROOT);
        float chestChance = activeManager.getNormalChestWeight();
        if (isCoinPile(event.getState(), path)) {
            rollBits(player, chestChance / 3.0F, 2, 4, "coin pile");
        } else if (isVaultOre(event.getState(), path)) {
            rollBits(player, chestChance * 0.9F, 3, 5, "ore");
        }
    }

    private static boolean isVaultOre(BlockState state, String path) {
        if (path.contains("chromatic_iron")) {
            return false;
        }
        return state.getBlock().getClass().getName().contains("VaultOreBlock")
                || (path.contains("ore") && !path.contains("decor"));
    }

    private static boolean isCoinPile(BlockState state, String path) {
        String className = state.getBlock().getClass().getName();
        return className.contains("CoinPile")
                || path.contains("coin_pile")
                || path.contains("coins")
                || path.contains("vault_coin");
    }

    private static void rollBits(ServerPlayer player, float chance, int min, int max, String source) {
        float effectiveChance = Math.min(1.0F, chance * APBoonManager.bitChanceMultiplier(player));
        if (RANDOM.nextFloat() >= effectiveChance) {
            return;
        }

        int amount = min + RANDOM.nextInt(max - min + 1);
        ModItems.giveBits(player, amount);
        ModSounds.playArchipelaBitsGain(player);
        LOGGER.info("Gave {} Archipela-bit(s) from {}", amount, source);
    }

    /**
     * Track level changes - for milestone checks
     */
    @SubscribeEvent
    public static void onPlayerLevelChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // We'll check level milestones periodically
            checkLevelMilestones(player);
            DeathLinkCurseManager.applyPendingCurses(player);
        }
    }

    // ==================== VH PROGRESSION CHECKING ====================

    /**
     * Check a player's current VH progression and send any missing checks
     */
    private static void checkPlayerProgression(ServerPlayer player) {
        try {
            // Try to access VH's player data using reflection
            LOGGER.info("Attempting to read VH player data for {}", player.getName().getString());

            // The VHProgressionTracker handles this now

        } catch (Exception e) {
            LOGGER.error("Failed to read VH player data", e);
        }
    }

    /**
     * Check if player has reached any level milestones
     */
    private static void checkLevelMilestones(ServerPlayer player) {
        if (manager == null) return;

        try {
            // Get actual VH level
            int level = VHDataReader.getPlayerLevel(player);

            if (level >= 10) manager.onLevelMilestone(player, 10);
            if (level >= 20) manager.onLevelMilestone(player, 20);
            if (level >= 40) manager.onLevelMilestone(player, 40);
            if (level >= 50) manager.onLevelMilestone(player, 50);
            if (level >= 100) manager.onLevelMilestone(player, 100);

        } catch (Exception e) {
            LOGGER.error("Failed to check level milestones", e);
        }
    }
}
