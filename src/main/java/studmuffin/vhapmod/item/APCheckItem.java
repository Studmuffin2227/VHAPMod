package studmuffin.vhapmod.item;

import studmuffin.vhapmod.VaultHuntersAPMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        return new TextComponent("AP Check");
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

        tooltip.add(new TextComponent("Archipelago Check")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(new TextComponent(" "));
        if (stack.hasTag() && stack.getTag().contains("ShopBits")) {
            int bits = stack.getTag().getInt("ShopBits");
            boolean trapReward = stack.getTag().getBoolean("ShopTrapReward");
            tooltip.add(new TextComponent((trapReward ? "Pays: " : "Costs: ") + bits + " Archipela-bits")
                    .withStyle(trapReward ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        if (stack.hasTag() && stack.getTag().contains("RewardName")) {
            tooltip.add(getContainsText(stack));
        } else {
            tooltip.add(new TextComponent("Contains: unknown")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!isRewardForLocalSlot(stack)) {
            tooltip.add(new TextComponent(" "));
            tooltip.add(getMysteryText(stack));
        }
        tooltip.add(new TextComponent(" "));
        tooltip.add(new TextComponent(stack.hasTag() && stack.getTag().contains("ShopBits") ? "Click in Check Table" : "Right-click to claim")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private Component getContainsText(ItemStack stack) {
        int flags = stack.hasTag() ? stack.getTag().getInt("ItemFlags") : 0;
        ChatFormatting itemColor = getItemColor(stack, flags);
        MutableComponent message = new TextComponent("Contains: ")
                .withStyle(ChatFormatting.GRAY);
        message.append(new TextComponent(stack.getTag().getString("RewardName"))
                .withStyle(itemColor));

        if (stack.getTag().contains("ReceiverSlotName")) {
            message.append(new TextComponent(" for ")
                    .withStyle(ChatFormatting.GRAY));
            message.append(new TextComponent(stack.getTag().getString("ReceiverSlotName"))
                    .withStyle(ChatFormatting.YELLOW));
        }
        return message;
    }

    private ChatFormatting getItemColor(ItemStack stack, int flags) {
        if (stack.hasTag() && stack.getTag().getBoolean("ShopTrapReward")) {
            return ChatFormatting.RED;
        }
        if ((flags & 1) != 0) {
            return ChatFormatting.LIGHT_PURPLE;
        }
        return ChatFormatting.AQUA;
    }

    private boolean isRewardForLocalSlot(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("RewardForLocalSlot");
    }

    private Component getMysteryText(ItemStack stack) {
        if (!stack.hasTag()) {
            return new TextComponent("You don't know what this is, but it seems useless")
                    .withStyle(ChatFormatting.DARK_GRAY);
        }

        if (stack.getTag().getBoolean("ShopTrapReward")) {
            return new TextComponent("You don't know what this is, but you could get richer")
                    .withStyle(ChatFormatting.RED);
        }

        int flags = stack.getTag().getInt("ItemFlags");
        if ((flags & 1) != 0) {
            MutableComponent message = new TextComponent("You don't know what this is, but it seems ")
                    .withStyle(ChatFormatting.GRAY);
            message.append(new TextComponent("IMPORTANT")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            return message;
        }
        if ((flags & 2) != 0 || flags == 0) {
            return new TextComponent("You don't know what this is, but it seems to have a use")
                    .withStyle(ChatFormatting.GRAY);
        }
        return new TextComponent("You don't know what this is, but it seems to have a use")
                .withStyle(ChatFormatting.GRAY);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
