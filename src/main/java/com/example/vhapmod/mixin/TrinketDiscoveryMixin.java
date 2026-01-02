package com.example.vhapmod.mixin;

import com.example.vhapmod.VaultHuntersAPMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "iskallia.vault.world.data.DiscoveredTrinketsData", remap = false)
public class TrinketDiscoveryMixin {

    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(
            method = "discoverTrinketAndBroadcast",
            at = @At("TAIL"),
            remap = false
    )
    private void onTrinketDiscovered(ItemStack stack, Player player, CallbackInfo ci) {
        LOGGER.info("=== TRINKET MIXIN FIRED ===");
        LOGGER.info("Player: {}", player.getName().getString());

        if (!(player instanceof ServerPlayer)) {
            LOGGER.warn("Player is not ServerPlayer!");
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        String trinketName = getTrinketName(stack);

        LOGGER.info("Trinket name: {}", trinketName);

        if (trinketName != null && VaultHuntersAPMod.getManager() != null) {
            String locationName = "Trinket: " + trinketName;
            LOGGER.info("Sending trinket check: {}", locationName);
            VaultHuntersAPMod.getManager().onTrinketFound(serverPlayer, locationName);
        } else {
            LOGGER.warn("Manager is null or trinket name is null!");
        }
    }

    private String getTrinketName(ItemStack stack) {
        try {
            return stack.getHoverName().getString();
        } catch (Exception e) {
            LOGGER.error("Failed to get trinket name", e);
            return null;
        }
    }
}