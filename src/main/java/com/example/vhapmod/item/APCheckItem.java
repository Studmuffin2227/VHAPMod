package com.example.vhapmod.item;

import com.example.vhapmod.VaultHuntersAPMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Single Archipelago check item - uses NBT to store location ID
 */
public class APCheckItem extends Item {

    public APCheckItem(long dummyId) {
        super(new Properties()
                .tab(null)
                .stacksTo(1)
                .rarity(Rarity.EPIC));
        // dummyId is ignored - real ID comes from NBT
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            // Check if already processed via NBT tag
            if (stack.hasTag() && stack.getTag().getBoolean("APProcessed")) {
                return;
            }

            // Mark as processed
            stack.getOrCreateTag().putBoolean("APProcessed", true);

            // Send check and consume
            sendCheckAndConsume(player, stack);
        }
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        Level level = itemEntity.level;

        if (!level.isClientSide) {
            Player player = level.getNearestPlayer(
                    itemEntity.getX(),
                    itemEntity.getY(),
                    itemEntity.getZ(),
                    5.0,
                    false
            );

            if (player instanceof ServerPlayer serverPlayer) {
                ItemStack stack = itemEntity.getItem();
                sendCheck(serverPlayer, stack);
            }
        }

        super.onDestroyed(itemEntity);
    }

    private void sendCheckAndConsume(ServerPlayer player, ItemStack stack) {
        sendCheck(player, stack);
        stack.shrink(1); // Delete item
    }

    private void sendCheck(ServerPlayer player, ItemStack stack) {
        // Read location ID from NBT
        long locationId = getLocationId(stack);
        if (locationId == 0) {
            player.sendMessage(
                    new TextComponent("⚠ Invalid AP check (no location ID)")
                            .withStyle(ChatFormatting.RED),
                    player.getUUID()
            );
            return;
        }

        var client = VaultHuntersAPMod.getAPClient();

        if (client != null && client.isConnected()) {
            client.sendLocationCheck(locationId);

            player.level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS,
                    0.7F,
                    1.5F
            );

            int checkNumber = (int)(locationId - 44000 + 1);
            player.sendMessage(
                    new TextComponent("✓ Archipelago Check #" + checkNumber)
                            .withStyle(ChatFormatting.GOLD),
                    player.getUUID()
            );
        } else {
            int checkNumber = (int)(locationId - 44000 + 1);
            player.sendMessage(
                    new TextComponent("✓ Check #" + checkNumber + " (not connected)")
                            .withStyle(ChatFormatting.YELLOW),
                    player.getUUID()
            );
        }
    }

    /**
     * Get the location ID from NBT
     */
    private long getLocationId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("LocationID")) {
            return stack.getTag().getLong("LocationID");
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        long locationId = getLocationId(stack);

        if (locationId == 0) {
            tooltip.add(new TextComponent("Invalid Check")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        int checkNumber = (int)(locationId - 44000 + 1);

        tooltip.add(new TextComponent("Archipelago Check")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("Check #" + checkNumber)
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("Consumed on pickup")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}