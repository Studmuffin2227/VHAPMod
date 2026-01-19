package com.example.vhapmod.mixin;

import com.example.vhapmod.VaultHuntersAPMod;
import com.example.vhapmod.VaultHuntersManager;
import com.example.vhapmod.item.ModItems;
import iskallia.vault.block.entity.VaultChestTileEntity;
import iskallia.vault.core.Version;
import iskallia.vault.core.random.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Intercepts Vault Hunters chest loot generation to add Archipelago checks
 */
@Mixin(value = VaultChestTileEntity.class, remap = false)
public class VaultChestLootMixin {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Inject at the end of generateLootTable to add/replace items with AP checks
     */
    @Inject(
            method = "generateLootTable",
            at = @At("TAIL"),
            remap = false
    )
    private void onGenerateLootTable(
            Version version,
            Player player,
            List<ItemStack> loot,
            RandomSource random,
            CallbackInfo ci
    ) {
        // Only process on server side and for real players
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Get the chest's position
        VaultChestTileEntity chest = (VaultChestTileEntity)(Object)this;
        BlockPos pos = chest.getBlockPos();

        // Check with VaultHuntersManager if this chest should contain an AP check
        VaultHuntersManager manager = VaultHuntersAPMod.getManager();
        if (manager == null) {
            return;
        }

        // Generate a location ID based on chest position
        // This ensures the same chest always has the same check
        long locationId = generateLocationIdFromPosition(pos);

        // Check if this location has already been collected
        if (manager.isLocationChecked(locationId)) {
            LOGGER.debug("Chest at {} already checked, skipping", pos);
            return;
        }

        // Check if this chest should contain an AP check
        // You can customize this logic - for now, let's say 20% of chests
        if (shouldContainCheck(pos, random)) {
            LOGGER.info("Adding AP check to chest at {} for player {}",
                    pos, serverPlayer.getName().getString());

            // Create the AP check item
            ItemStack checkItem = ModItems.getCheckItemStack(locationId);

            // Strategy: Replace the first item with the check
            // You can change this to clear all items, add as bonus, etc.
            if (!loot.isEmpty()) {
                loot.set(0, checkItem);
            } else {
                loot.add(checkItem);
            }
        }
    }

    /**
     * Generate a consistent location ID from chest position
     * Uses a hash of the position to ensure same chest = same ID
     */
    private long generateLocationIdFromPosition(BlockPos pos) {
        // Base location ID for chests (you can adjust this range)
        long BASE_CHEST_ID = 45000L; // After your other location types

        // Create a hash from position
        // This ensures the same position always generates the same ID
        long hash = (long)pos.getX() * 73856093L
                ^ (long)pos.getY() * 19349663L
                ^ (long)pos.getZ() * 83492791L;

        // Keep it positive and in a reasonable range
        hash = Math.abs(hash) % 10000; // Max 10000 chest locations

        return BASE_CHEST_ID + hash;
    }

    /**
     * Determine if this chest should contain an AP check
     * You can customize this logic based on your needs
     */
    private boolean shouldContainCheck(BlockPos pos, RandomSource random) {
        // Option 1: Random percentage (20% of chests)
        // return random.nextFloat() < 0.2f;

        // Option 2: Use position hash for deterministic checks
        // Same chest will always be/not be a check
        int hash = pos.hashCode();
        return (hash & 0xFF) < 51; // ~20% of chests (51/256)

        // Option 3: Every Nth chest
        // return (Math.abs(pos.getX() + pos.getY() + pos.getZ()) % 5) == 0;
    }
}