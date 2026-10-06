package studmuffin.vhapmod;

import studmuffin.vhapmod.network.CheckTableSelectSphereMessage;
import studmuffin.vhapmod.network.CheckTableSelectTabMessage;
import studmuffin.vhapmod.network.CheckTableHintUsefulMessage;
import studmuffin.vhapmod.network.ArchipelaPouchActionMessage;
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
        CHANNEL.registerMessage(
                id++,
                CheckTableSelectSphereMessage.class,
                CheckTableSelectSphereMessage::encode,
                CheckTableSelectSphereMessage::decode,
                CheckTableSelectSphereMessage::handle
        );
        CHANNEL.registerMessage(
                id++,
                CheckTableSelectTabMessage.class,
                CheckTableSelectTabMessage::encode,
                CheckTableSelectTabMessage::decode,
                CheckTableSelectTabMessage::handle
        );
        CHANNEL.registerMessage(
                id++,
                CheckTableHintUsefulMessage.class,
                CheckTableHintUsefulMessage::encode,
                CheckTableHintUsefulMessage::decode,
                CheckTableHintUsefulMessage::handle
        );
        CHANNEL.registerMessage(
                id++,
                ArchipelaPouchActionMessage.class,
                ArchipelaPouchActionMessage::encode,
                ArchipelaPouchActionMessage::decode,
                ArchipelaPouchActionMessage::handle
        );
    }
}
