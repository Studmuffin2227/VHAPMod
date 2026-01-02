package com.example.vhapmod;

import com.example.vhapmod.network.APUnlockSyncMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    /**
     * MUST be called during mod construction phase!
     * Cannot be called at runtime or you get "Registration of impl channels is locked"
     */
    public static void init() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation("apvaulthuntersmod", "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        // Register messages
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                APUnlockSyncMessage.class,
                APUnlockSyncMessage::encode,
                APUnlockSyncMessage::decode,
                APUnlockSyncMessage::handle
        );
    }
}