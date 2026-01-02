package com.example.vhapmod.mixin;

import com.example.vhapmod.APSkillLockManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "iskallia.vault.network.message.AbilityLevelMessage", remap = false)
public class AbilityLevelMessageMixin {

    @Shadow @Final private String abilityName;
    @Shadow @Final private boolean isUpgrade;

    private static final Logger LOGGER = LogManager.getLogger("AbilityMixin");

    @Inject(
            method = "upgradeAbility",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void preventAbilityUpgrade(iskallia.vault.network.message.AbilityLevelMessage message, ServerPlayer player, CallbackInfo ci) {
        LOGGER.info("=== ABILITY UPGRADE ATTEMPT ===");

        try {
            java.lang.reflect.Field field = message.getClass().getDeclaredField("abilityName");
            field.setAccessible(true);
            String abilityName = (String) field.get(message);

            LOGGER.info("Ability: {}", abilityName);
            LOGGER.info("Player: {}", player.getName().getString());

            String normalizedName = "vhskill:" + abilityName.toLowerCase().replace(" ", "_");

            if (!APSkillLockManager.isSkillUnlockedSilent(player, normalizedName)) {
                LOGGER.warn("BLOCKING ability upgrade!");

                MutableComponent msg = new TextComponent("[AP] ")
                        .withStyle(ChatFormatting.RED)
                        .append(new TextComponent(abilityName + " is locked! You need to receive it from Archipelago first."));

                player.sendMessage(msg, player.getUUID());
                ci.cancel();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get ability name from message", e);
        }
    }
}