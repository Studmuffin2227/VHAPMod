package studmuffin.vhapmod.mixin;

import studmuffin.vhapmod.VaultHuntersAPMod;
import studmuffin.vhapmod.VaultHuntersManager;
import studmuffin.vhapmod.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Backup method: Mixin that gives AP checks directly to player when opening Vault chests
 * This is disabled by default - use VaultChestEventListener instead
 */
@Mixin(targets = "iskallia.vault.block.entity.VaultChestTileEntity", remap = false)
public abstract class VaultChestLootMixin {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    @Unique
    private boolean vhapmod$hasGivenCheck = false;

    @Inject(
            method = "generateChestLoot",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void onGenerateChestLoot(Player source, boolean compress, CallbackInfo ci) {
        // Prevent multiple triggers
        if (this.vhapmod$hasGivenCheck) {
            return;
        }

        // Only process on server side and for real players
        if (!(source instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Get manager
        VaultHuntersManager manager = VaultHuntersAPMod.getManager();
        if (manager == null) {
            return;
        }

        // Roll the dice - does this chest have a check?
        float checkChance = manager.getNormalChestWeight();
        if (RANDOM.nextFloat() >= checkChance) {
            return;
        }

        // Get the next check number
        long locationId = manager.getNextChestCheckId(serverPlayer);
        if (locationId == 0) {
            return;
        }

        int checkNumber = (int)(locationId - 50000L + 1);
        LOGGER.info("Player {} found chest check #{} ({}% chance)",
                serverPlayer.getName().getString(), checkNumber, (checkChance * 100));

        // Create the AP check item and give it directly to player
        ItemStack checkItem = ModItems.getCheckItemStack(locationId);
        boolean added = serverPlayer.getInventory().add(checkItem);

        if (!added) {
            // If inventory is full, drop it at player's feet
            serverPlayer.drop(checkItem, false);
        }

        this.vhapmod$hasGivenCheck = true;
    }
}