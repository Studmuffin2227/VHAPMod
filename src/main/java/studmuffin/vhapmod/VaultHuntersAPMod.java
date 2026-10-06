package studmuffin.vhapmod;

import studmuffin.vhapmod.block.ModBlocks;
import studmuffin.vhapmod.event.VaultChestEventListener;
import studmuffin.vhapmod.item.ModItems;
import studmuffin.vhapmod.menu.ModMenus;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("apvaulthuntersmod")
public class VaultHuntersAPMod {

    private static final Logger LOGGER = LogManager.getLogger();
    private static VaultHuntersManager vhManager;
    private static APWebSocketClient apClient;
    private static VHProgressionTracker progressionTracker;

    public VaultHuntersAPMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("=== VHAP MOD CONSTRUCTOR STARTING ===");

        // Register items
        LOGGER.info("Registering items...");
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        LOGGER.info("Items registered!");

        //Register the Chest Generation Event Listener
        iskallia.vault.core.event.CommonEvents.CHEST_LOOT_GENERATION.register(
                this,
                (data) -> VaultChestEventListener.onChestGeneration(data)
        );
        iskallia.vault.core.event.CommonEvents.ARTIFACT_CHANCE.register(
                this,
                (data) -> {
                    VaultHuntersManager manager = getManager();
                    if (manager != null && manager.suppressesVaultArtifactRewards()) {
                        data.setProbability(0.0F);
                    }
                }
        );
        iskallia.vault.core.event.CommonEvents.PLAYER_STAT.register(
                this,
                APBlessingManager::onPlayerStat
        );
        iskallia.vault.core.event.CommonEvents.MANA_MODIFY.register(
                this,
                APRingLinkManager::onManaModify
        );
        APBoonManager.registerModifierAndPatchPools();

        LOGGER.info("Registered Vault Chest event listener for AP checks");

        ModNetwork.init();

        LOGGER.info("=== VHAP MOD CONSTRUCTOR COMPLETE ===");

        // Setup events
        modEventBus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        vhManager = new VaultHuntersManager();
        apClient = new APWebSocketClient(vhManager);
        progressionTracker = new VHProgressionTracker();

        VaultEventHandler.setManager(vhManager);
        VHProgressionTracker.setManager(vhManager);
        vhManager.setAPClient(apClient);



        LOGGER.info("VH AP Bridge setup complete");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Give the server to AP client so it can get players
        apClient.setServer(event.getServer());
        APBoonManager.registerModifierAndPatchPools();
        LOGGER.info("Server started - AP client ready");
        LOGGER.info("Use /apconnect <host> <port> <slotName> to connect to AP");
        LOGGER.info("Example: /apconnect archipelago.gg 45624 Muffin");
        LOGGER.info("AP auto-connect is disabled for server hosting; connect manually with /apconnect");
    }

    public static void tryAutoConnectIfPlayersOnline(MinecraftServer server) {
        if (apClient == null || server == null) {
            return;
        }

        apClient.setServer(server);
    }

    public static void disconnectFromAPIfNoPlayersOnline(MinecraftServer server) {
        if (apClient == null || server == null || !server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }

        if (apClient.isConnectionActive()) {
            LOGGER.info("No players online - disconnecting from AP server");
            apClient.disconnect("No Minecraft players online");
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering AP commands...");
        APCommands.register(event.getDispatcher());
        LOGGER.info("AP commands registered!");
    }

    public static VaultHuntersManager getManager() {
        return vhManager;
    }

    public static APWebSocketClient getAPClient() {
        return apClient;
    }

    public static VHProgressionTracker getProgressionTracker() {
        return progressionTracker;
    }

}
