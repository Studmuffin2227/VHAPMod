package com.example.vhapmod.mixin;

import com.example.vhapmod.VaultHuntersAPMod;
import com.example.vhapmod.VaultHuntersManager;
import com.example.vhapmod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

/**
 * Intercepts Vault Hunters chest loot generation to add Archipelago checks
 *
 * STRATEGY: Counter-based progressive checks with CHANCE
 * - Only SOME chests give checks (configurable %)
 * - But the checks are still numbered: "Vault Chest 1", "Vault Chest 2", etc.
 * - Player might open 10 chests before finding check #1
 * - Then open 5 more before finding check #2
 */
@Mixin(targets = "iskallia.vault.block.entity.VaultChestTileEntity", remap = false)
public abstract class VaultChestLootMixin extends BlockEntity {

    private static final Logger LOGGER = LogManager.getLogger();


    VaultHuntersManager manager = VaultHuntersAPMod.getManager();

    // Get the Chance of a check appearing from the manager
    float checkChance = manager.getNormalChestWeight();


    // Use Java's Random for the chance roll
    private static final Random RANDOM = new Random();

    // Required constructor for extending BlockEntity
    public VaultChestLootMixin() {
        super(null, null, null);
    }

    // Shadow the getBlockPos method from the chest
    @Shadow
    public abstract BlockPos getBlockPos();

    /**
     * Inject at the end of generateLootTable to add/replace items with AP checks
     */
    @Inject(
            method = "generateLootTable",
            at = @At("TAIL"),
            remap = false
    )
    private void onGenerateLootTable(
            Object version,
            Player player,
            List<ItemStack> loot,
            Object random,
            CallbackInfo ci
    ) {
        // Only process on server side and for real players
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Get manager
        VaultHuntersManager manager = VaultHuntersAPMod.getManager();
        if (manager == null) {
            return;
        }

        // FIRST: Roll the dice - does this chest have a check?
        if (!shouldContainCheck()) {
            // No check in this chest - just return normal loot
            return;
        }

        // SECOND: This chest won the lottery! Get the next check number
        long locationId = manager.getNextChestCheckId(serverPlayer);

        if (locationId == 0) {
            // No more chest checks available for this player
            LOGGER.debug("No more chest checks available for player {}",
                    serverPlayer.getName().getString());
            return;
        }

        int checkNumber = (int)(locationId - 45000);
        LOGGER.info("Player {} found chest check #{} ({}% chance)",
                serverPlayer.getName().getString(), checkNumber, (checkChance * 100));

        // Create the AP check item
        ItemStack checkItem = ModItems.getCheckItemStack(locationId);

        // Replace the first item with the check
        if (!loot.isEmpty()) {
            loot.set(0, checkItem);
        } else {
            loot.add(checkItem);
        }
    }

    /**
     * Determine if this chest should contain an AP check
     * This is the RNG roll!
     */
    private boolean shouldContainCheck() {
        return RANDOM.nextFloat() < checkChance;
    }

}