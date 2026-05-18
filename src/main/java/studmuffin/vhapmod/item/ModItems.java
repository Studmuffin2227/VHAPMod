package studmuffin.vhapmod.item;

import studmuffin.vhapmod.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

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

    public static final RegistryObject<BlockItem> CHECK_TABLE =
            ITEMS.register("check_table", () -> new BlockItem(ModBlocks.CHECK_TABLE.get(), new Item.Properties().tab(null)));

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
        stack.setCount(Math.max(1, Math.min(64, bitAmount)));
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
}
