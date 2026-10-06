package studmuffin.vhapmod.item;

import studmuffin.vhapmod.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Registers just ONE AP check item
 * Uses NBT to store which location ID it represents
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "apvaulthuntersmod");

    // ONLY ONE ITEM REGISTERED!
    public static final RegistryObject<APCheckItem> AP_CHECK =
            ITEMS.register("ap_check", () -> new APCheckItem(0L)); // Dummy ID, uses NBT

    public static final RegistryObject<ArchipelaBitsItem> ARCHIPELA_BITS =
            ITEMS.register("archipela_bits", ArchipelaBitsItem::new);

    public static final RegistryObject<ArchipelaPouchItem> ARCHIPELA_POUCH =
            ITEMS.register("archipela_pouch", ArchipelaPouchItem::new);

    public static final RegistryObject<TrapCleanserItem> TRAP_CLEANSER =
            ITEMS.register("trap_cleanser", TrapCleanserItem::new);

    public static final RegistryObject<BlockItem> CHECK_TABLE =
            ITEMS.register("check_table", () -> new BlockItem(ModBlocks.CHECK_TABLE.get(), new Item.Properties().tab(ModCreativeModeTab.TAB)) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(new TextComponent("Use bits found from Chests/Ores/Coin Piles to buy checks")
                            .withStyle(ChatFormatting.GRAY));
                }
            });

    /**
     * Create an ItemStack for a specific location ID
     */
    public static ItemStack getCheckItemStack(long locationId) {
        ItemStack stack = new ItemStack(AP_CHECK.get());

        // Store the location ID in NBT
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong("LocationID", locationId);

        return stack;
    }

    public static ItemStack getCheckItemStack(long locationId, String rewardName) {
        ItemStack stack = getCheckItemStack(locationId);
        if (rewardName != null && !rewardName.isBlank()) {
            stack.getOrCreateTag().putString("RewardName", rewardName);
        }
        return stack;
    }

    public static ItemStack getShopCheckItemStack(long locationId, String rewardName, int bitAmount, boolean trapReward, int itemFlags,
                                                  String receiverSlotName, boolean rewardForLocalSlot) {
        ItemStack stack = getCheckItemStack(locationId, rewardName);
        stack.setCount(Math.max(1, bitAmount));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("ShopBits", bitAmount);
        tag.putBoolean("ShopTrapReward", trapReward);
        tag.putInt("ItemFlags", itemFlags);
        tag.putBoolean("RewardForLocalSlot", rewardForLocalSlot);
        if (receiverSlotName != null && !receiverSlotName.isBlank()) {
            tag.putString("ReceiverSlotName", receiverSlotName);
        }
        return stack;
    }

    public static ItemStack getBitsStack(int count) {
        return new ItemStack(ARCHIPELA_BITS.get(), Math.max(1, count));
    }

    public static void giveBits(ServerPlayer player, int amount) {
        int remaining = Math.max(0, amount);
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ARCHIPELA_POUCH.get())) {
                remaining = ArchipelaPouchItem.addBits(stack, remaining);
            }
        }
        if (remaining <= 0) {
            player.getInventory().setChanged();
            return;
        }
        ItemStack stack = getBitsStack(remaining);
        if (!player.getInventory().add(stack) || !stack.isEmpty()) {
            ItemEntity dropped = player.drop(stack.copy(), false);
            if (dropped != null) {
                dropped.setNoPickUpDelay();
                dropped.setOwner(player.getUUID());
            }
        }
        player.getInventory().setChanged();
    }

    public static ItemStack getBountyCheckItemStack(long locationId, String rewardName, int itemFlags,
                                                    String receiverSlotName, boolean rewardForLocalSlot) {
        ItemStack stack = getCheckItemStack(locationId, rewardName);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("BountyCheck", true);
        tag.putBoolean("AutoClaim", true);
        tag.putInt("ItemFlags", itemFlags);
        tag.putBoolean("RewardForLocalSlot", rewardForLocalSlot);
        if (receiverSlotName != null && !receiverSlotName.isBlank()) {
            tag.putString("ReceiverSlotName", receiverSlotName);
        }
        return stack;
    }
}
