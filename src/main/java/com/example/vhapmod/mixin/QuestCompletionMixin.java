package com.example.vhapmod.mixin;

import com.example.vhapmod.VaultHuntersAPMod;
import iskallia.vault.quest.base.Quest;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "iskallia.vault.quest.QuestState", remap = false)
public class QuestCompletionMixin {

    private static final Logger LOGGER = LogManager.getLogger("QuestMixin");

    @Shadow
    protected UUID playerId;

    @Inject(
            method = "setComplete",
            at = @At("TAIL"),
            remap = false
    )
    private void onQuestCompleted(Quest quest, CallbackInfo ci) {
        LOGGER.info("=== QUEST MIXIN FIRED ===");
        LOGGER.info("Quest: {}", quest.getName());
        LOGGER.info("Player UUID: {}", playerId);

        // Get ServerPlayer from UUID
        ServerPlayer player = getServerPlayer();

        if (player != null && VaultHuntersAPMod.getProgressionTracker() != null) {
            LOGGER.info("Calling progression tracker...");
            VaultHuntersAPMod.getProgressionTracker().onQuestCompleted(player);
        } else {
            LOGGER.warn("Player or progression tracker is null!");
        }
    }

    @Shadow
    private ServerPlayer getServerPlayer() {
        return null; // Mixin will replace this
    }
}