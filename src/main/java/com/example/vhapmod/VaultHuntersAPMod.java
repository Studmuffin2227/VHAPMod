package com.example.vhapmod;

import com.example.vhapmod.event.VaultChestEventListener;
import com.example.vhapmod.item.ModItems;
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
        ModItems.ITEMS.register(modEventBus);
        LOGGER.info("Items registered!");

        //Register the Chest Generation Event Listener
        iskallia.vault.core.event.CommonEvents.CHEST_LOOT_GENERATION.register(
                this,
                (data) -> VaultChestEventListener.onChestGeneration(data)
        );

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

        VHProgressionTracker.setManager(vhManager);
        vhManager.setAPClient(apClient);



        LOGGER.info("VH AP Bridge setup complete");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Give the server to AP client so it can get players
        apClient.setServer(event.getServer());
        LOGGER.info("Server started - AP client ready");
        LOGGER.info("Use /apconnect <host> <port> <slotName> to connect to AP");
        LOGGER.info("Example: /apconnect archipelago.gg 45624 Muffin");

        APConnectionConfig config = APConnectionConfig.load();
        if (config.autoConnect && config.hasConnectionInfo() && !apClient.isConnected()) {
            LOGGER.info("Auto-connecting to AP server {}:{} as {}",
                    config.host, config.port, config.slotName);
            apClient.connect(config.host, config.port, config.slotName, config.password)
                    .whenComplete((unused, throwable) -> {
                        if (throwable != null) {
                            String reason = throwable.getMessage() != null
                                    ? throwable.getMessage()
                                    : throwable.getClass().getSimpleName();
                            LOGGER.warn("Auto-connect failed: {}", reason);
                        }
                    });
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