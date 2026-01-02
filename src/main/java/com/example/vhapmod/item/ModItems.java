package com.example.vhapmod.item;

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
}