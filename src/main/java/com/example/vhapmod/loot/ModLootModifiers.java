package com.example.vhapmod.loot;

import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModLootModifiers {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<GlobalLootModifierSerializer<?>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.LOOT_MODIFIER_SERIALIZERS, "apvaulthuntersmod");

    static {
        LOGGER.info("=== ModLootModifiers STATIC BLOCK ===");
        LOGGER.info("DeferredRegister created for: apvaulthuntersmod");
    }

    public static final RegistryObject<APAwareLootModifier.Serializer> AP_CHECK_MODIFIER =
            LOOT_MODIFIERS.register("ap_check", () -> {
                LOGGER.info("Creating APAwareLootModifier.Serializer instance");
                return new APAwareLootModifier.Serializer();
            });

    static {
        LOGGER.info("Registered ap_check loot modifier");
    }
}