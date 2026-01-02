package com.example.vhapmod.mixin;

import com.example.vhapmod.APSkillLockManager;
import iskallia.vault.research.type.Research;
import iskallia.vault.research.ResearchTree;
import iskallia.vault.world.data.PlayerResearchesData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "iskallia.vault.world.data.PlayerResearchesData", remap = false)
public class ModResearchPreventionMixin {

    private static final Logger LOGGER = LogManager.getLogger("ModResearchMixin");

    @Inject(
            method = "research(Lnet/minecraft/server/level/ServerPlayer;Liskallia/vault/research/type/Research;Z)Liskallia/vault/world/data/PlayerResearchesData;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void preventModResearch(ServerPlayer player, Research research, boolean sendMessage, CallbackInfoReturnable<PlayerResearchesData> cir) {
        LOGGER.info("=== MOD RESEARCH MIXIN FIRED ===");
        LOGGER.info("Player: {}", player.getName().getString());
        LOGGER.info("Mod: {}", research.getName());

        String normalizedName = "vhmod:" + research.getName().toLowerCase().replace(" ", "_");
        LOGGER.info("Normalized: {}", normalizedName);

        boolean isUnlocked = APSkillLockManager.isModUnlockedSilent(player, normalizedName);
        LOGGER.info("Is unlocked in AP: {}", isUnlocked);

        if (!isUnlocked) {
            LOGGER.warn("BLOCKING mod research!");

            // Get the actual research cost to refund
            PlayerResearchesData researchesData = PlayerResearchesData.get(player.getLevel());
            ResearchTree researchTree = researchesData.getResearches(player);
            int researchCost = researchTree.getResearchCost(research);

            LOGGER.info("Refunding {} knowledge points", researchCost);

            // Refund the actual knowledge point cost
            net.minecraft.server.level.ServerLevel level = player.getLevel();
            iskallia.vault.world.data.PlayerVaultStatsData statsData =
                    iskallia.vault.world.data.PlayerVaultStatsData.get(level);
            iskallia.vault.skill.PlayerVaultStats stats = statsData.getVaultStats(player);
            stats.addKnowledgePoints(researchCost);
            statsData.setDirty();

            MutableComponent message = new TextComponent("[AP] ")
                    .withStyle(ChatFormatting.RED)
                    .append(new TextComponent(research.getName() + " is locked! You need to receive it from Archipelago first."));

            player.sendMessage(message, player.getUUID());
            cir.cancel();
        }
    }
}