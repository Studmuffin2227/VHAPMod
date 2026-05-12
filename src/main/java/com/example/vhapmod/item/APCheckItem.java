package com.example.vhapmod.item;

import com.example.vhapmod.VaultHuntersAPMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
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
    public Component getName(ItemStack stack) {
        return new TextComponent("AP Chest Item");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (player instanceof ServerPlayer serverPlayer && claimCheck(serverPlayer, hand)) {
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), false);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer && claimCheck(serverPlayer, context.getHand())) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private boolean claimCheck(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return false;
        }

        if (!sendCheck(player, stack)) {
            return false;
        }

        stack.shrink(1);
        if (stack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
        player.getInventory().setChanged();
        return true;
    }

    private boolean sendCheck(ServerPlayer player, ItemStack stack) {
        // Read location ID from NBT
        long locationId = getLocationId(stack);
        if (locationId == 0) {
            player.sendMessage(
                    new TextComponent("Invalid AP check (no location ID)")
                            .withStyle(ChatFormatting.RED),
                    player.getUUID()
            );
            return false;
        }

        if (locationId < 44000L || locationId >= 44500L) {
            player.sendMessage(
                    new TextComponent("Invalid AP chest check ID: " + locationId)
                            .withStyle(ChatFormatting.RED),
                    player.getUUID()
            );
            return false;
        }

        var client = VaultHuntersAPMod.getAPClient();
        int checkNumber = (int)(locationId - 44000 + 1);

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

            return true;
        } else {
            player.sendMessage(
                    new TextComponent("Check #" + checkNumber + " (not connected)")
                            .withStyle(ChatFormatting.YELLOW),
                    player.getUUID()
            );
            return true;
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
        if (stack.hasTag() && stack.getTag().contains("RewardName")) {
            tooltip.add(new TextComponent("Contains: " + stack.getTag().getString("RewardName"))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(new TextComponent("Contains: unknown")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(new TextComponent(""));
        tooltip.add(new TextComponent("Right-click to claim")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
