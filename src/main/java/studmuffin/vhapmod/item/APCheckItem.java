package studmuffin.vhapmod.item;

import studmuffin.vhapmod.VaultHuntersAPMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
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

        if (!isValidCheckLocation(locationId)) {
            player.sendMessage(
                    new TextComponent("Invalid AP check ID: " + locationId)
                            .withStyle(ChatFormatting.RED),
                    player.getUUID()
            );
            return false;
        }

        var client = VaultHuntersAPMod.getAPClient();
        int checkNumber = getCheckNumber(locationId);

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
            if (stack.hasTag() && stack.getTag().getBoolean("AutoClaim")) {
                return false;
            }
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

    private boolean isValidCheckLocation(long locationId) {
        return (locationId >= 44000L && locationId < 44500L)
                || (locationId >= 43650L && locationId < 43700L)
                || (locationId >= 44500L && locationId < 44650L);
    }

    private int getCheckNumber(long locationId) {
        if (locationId >= 43650L && locationId < 43700L) {
            return (int)(locationId - 43650L + 1L);
        }
        if (locationId >= 44500L && locationId < 44650L) {
            return (int)(locationId - 44500L + 51L);
        }
        return (int)(locationId - 44000L + 1L);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !stack.hasTag()
                || !stack.getTag().getBoolean("AutoClaim")) {
            return;
        }
        if (!sendCheck(player, stack)) {
            return;
        }
        stack.shrink(1);
        player.getInventory().setChanged();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        long locationId = getLocationId(stack);

        if (locationId == 0) {
            tooltip.add(new TextComponent("Invalid Check")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        tooltip.add(new TextComponent(stack.hasTag() && stack.getTag().getBoolean("BountyCheck") ? "Bounty Check #" + getCheckNumber(locationId) : "Archipelago Check")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(new TextComponent(" "));

        String displayMode = getRewardDisplayMode(stack);
        if ("nothing".equals(displayMode)) {
            tooltip.add(new TextComponent("A Mysterious Item From Another World")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else if ("classification".equals(displayMode)) {
            tooltip.add(getClassificationText(stack, locationId));
        } else {
            String rewardName = getRewardName(stack, locationId);
            if (rewardName != null && !rewardName.isBlank()) {
                tooltip.add(getContainsText(stack, locationId, rewardName));
            } else {
                tooltip.add(new TextComponent("Contains: unknown")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            if (!isRewardForLocalSlot(stack, locationId)) {
                tooltip.add(new TextComponent(" "));
                tooltip.add(getMysteryText(stack, locationId));
            }
        }
        tooltip.add(new TextComponent(" "));
        tooltip.add(new TextComponent(stack.hasTag() && stack.getTag().contains("ShopBits") ? "Click in Check Table" : "Right-click to claim")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private String getRewardDisplayMode(ItemStack stack) {
        var client = VaultHuntersAPMod.getAPClient();
        if (client == null) {
            return "all";
        }
        if (client.isCheckRewardHinted(getLocationId(stack))) {
            return "all";
        }
        if (stack.hasTag() && stack.getTag().getBoolean("BountyCheck")) {
            return client.getBountyCheckRewardDisplay();
        }
        if (stack.hasTag() && stack.getTag().contains("ShopBits")) {
            return client.getShopCheckRewardDisplay();
        }
        return "all";
    }

    private Component getContainsText(ItemStack stack, long locationId, String rewardName) {
        int flags = getRewardFlags(stack, locationId);
        ChatFormatting itemColor = getItemColor(stack, flags);
        MutableComponent message = new TextComponent("Contains: ")
                .withStyle(ChatFormatting.GRAY);
        message.append(new TextComponent(rewardName)
                .withStyle(itemColor));

        String receiverName = getRewardReceiverName(stack, locationId);
        if (receiverName != null && !receiverName.isBlank()) {
            message.append(new TextComponent(" for ")
                    .withStyle(ChatFormatting.GRAY));
            message.append(new TextComponent(receiverName)
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

    private boolean isRewardForLocalSlot(ItemStack stack, long locationId) {
        if (stack.hasTag() && stack.getTag().contains("RewardForLocalSlot")) {
            return stack.getTag().getBoolean("RewardForLocalSlot");
        }
        var client = VaultHuntersAPMod.getAPClient();
        return client == null || client.isScoutedRewardLocal(locationId);
    }

    private Component getMysteryText(ItemStack stack, long locationId) {
        if (!stack.hasTag()) {
            return new TextComponent("You don't know what this is, but it seems useless")
                    .withStyle(ChatFormatting.DARK_GRAY);
        }

        if (stack.getTag().getBoolean("ShopTrapReward")) {
            return new TextComponent("You don't know what this is, but you could get richer")
                    .withStyle(ChatFormatting.RED);
        }

        int flags = getRewardFlags(stack, locationId);
        if ((flags & 1) != 0) {
            MutableComponent message = new TextComponent("You don't know what this is, but it seems ")
                    .withStyle(ChatFormatting.GRAY);
            message.append(new TextComponent("IMPORTANT")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            return message;
        }
        if ((flags & 2) != 0) {
            return new TextComponent("You don't know what this is, but it seems to have a use")
                    .withStyle(ChatFormatting.GRAY);
        }
        return new TextComponent("You don't know what this is, but it seems probably not needed")
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private Component getClassificationText(ItemStack stack, long locationId) {
        int flags = getRewardFlags(stack, locationId);
        String classification = getClassificationName(flags);
        ChatFormatting color = getClassificationColor(flags);
        boolean localReward = isRewardForLocalSlot(stack, locationId);

        MutableComponent message = new TextComponent(localReward
                ? "You know what this is, and it is "
                : "You don't know what this is, and it seems like it is ")
                .withStyle(ChatFormatting.GRAY);
        TextComponent classificationText = new TextComponent(classification);
        message.append((flags & 1) != 0
                ? classificationText.withStyle(color, ChatFormatting.BOLD)
                : classificationText.withStyle(color));
        return message;
    }

    private String getClassificationName(int flags) {
        if ((flags & 4) != 0) {
            return "Harmful";
        }
        if ((flags & 1) != 0) {
            return "IMPORTANT";
        }
        if ((flags & 2) != 0) {
            return "Useful";
        }
        return "Probably not needed";
    }

    private ChatFormatting getClassificationColor(int flags) {
        if ((flags & 4) != 0) {
            return ChatFormatting.RED;
        }
        if ((flags & 1) != 0) {
            return ChatFormatting.LIGHT_PURPLE;
        }
        if ((flags & 2) != 0) {
            return ChatFormatting.AQUA;
        }
        return ChatFormatting.DARK_GRAY;
    }

    private String getRewardName(ItemStack stack, long locationId) {
        if (stack.hasTag() && stack.getTag().contains("RewardName")) {
            return stack.getTag().getString("RewardName");
        }
        var client = VaultHuntersAPMod.getAPClient();
        return client != null ? client.getScoutedRewardName(locationId) : null;
    }

    private int getRewardFlags(ItemStack stack, long locationId) {
        if (stack.hasTag() && stack.getTag().contains("ItemFlags")) {
            return stack.getTag().getInt("ItemFlags");
        }
        var client = VaultHuntersAPMod.getAPClient();
        return client != null ? client.getScoutedRewardFlags(locationId) : 0;
    }

    private String getRewardReceiverName(ItemStack stack, long locationId) {
        if (stack.hasTag() && stack.getTag().contains("ReceiverSlotName")) {
            return stack.getTag().getString("ReceiverSlotName");
        }
        var client = VaultHuntersAPMod.getAPClient();
        return client != null ? client.getScoutedRewardReceiverName(locationId) : null;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
