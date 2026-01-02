package com.example.vhapmod.loot;

import com.example.vhapmod.VaultHuntersAPMod;
import com.example.vhapmod.item.ModItems;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * AP-aware loot modifier
 * - Reads chest count from APWorld YAML
 * - Only spawns checks that haven't been collected yet
 * - Stops spawning after all checks found
 */
public class APAwareLootModifier extends LootModifier {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    // Track which location IDs have been found
    private static final Set<Long> foundChecks = new HashSet<>();
    private static final List<Long> remainingChecks = new ArrayList<>();

    // These will be read from the AP client's data
    private static int totalChestChecks = 100; // Default, overridden by AP
    private static float woodenChestWeight = 0.05f;
    private static float normalChestWeight = 0.25f;

    protected APAwareLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    /**
     * Called when AP connects - tells us how many chest checks exist
     */
    public static void setChestCheckCount(int count) {
        totalChestChecks = count;
        initializeRemainingChecks();
    }

    /**
     * Set wooden chest spawn weight from YAML
     */
    public static void setWoodenChestWeight(float weight) {
        woodenChestWeight = weight;
    }

    /**
     * Set normal chest spawn weight from YAML
     */
    public static void setNormalChestWeight(float weight) {
        normalChestWeight = weight;
    }

    /**
     * Called when a check is found
     */
    public static void markCheckFound(long locationId) {
        foundChecks.add(locationId);
        remainingChecks.remove(locationId);
    }

    /**
     * Initialize the pool of chest checks from AP data
     */
    private static void initializeRemainingChecks() {
        // TODO: Get the actual chest check location IDs from APWebSocketClient
        // For now, we'll use a range (you'll replace this with actual AP data)
        remainingChecks.clear();

        // Example: chest checks might be IDs 44000-44099
        long CHEST_CHECK_BASE = 44000L;
        for (int i = 0; i < totalChestChecks; i++) {
            long locationId = CHEST_CHECK_BASE + i;
            if (!foundChecks.contains(locationId)) {
                remainingChecks.add(locationId);
            }
        }
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        // FIRST: Log that this method is even being called
        LOGGER.info("=== doApply CALLED ===");
        // DEBUG: Log every loot table that opens
        ResourceLocation lootTable = context.getQueriedLootTableId();
        LOGGER.info("=== LOOT TABLE OPENED ===");
        LOGGER.info("Namespace: {}", lootTable.getNamespace());
        LOGGER.info("Path: {}", lootTable.getPath());
        LOGGER.info("Full ID: {}", lootTable);
        LOGGER.info("========================");

        // If all checks found, stop adding items
        if (remainingChecks.isEmpty()) {
            LOGGER.info("  → No remaining checks to spawn");
            return generatedLoot;
        }

        // Check if this is a vault chest
        if (lootTable == null || !isVaultChest(lootTable)) {
            LOGGER.info("  → Not a vault chest, skipping");
            return generatedLoot;
        }

        LOGGER.info("  → ✓ This is a VAULT CHEST!");

        // Determine chest type and weight
        float weight = isWoodenChest(lootTable) ? woodenChestWeight : normalChestWeight;
        LOGGER.info("  → Chest type: {}, Weight: {}%",
                isWoodenChest(lootTable) ? "WOODEN" : "NORMAL",
                (int)(weight * 100));

        // Roll for AP check item
        float roll = RANDOM.nextFloat();
        LOGGER.info("  → Rolling... {} < {} ?", roll, weight);

        if (roll < weight) {
            // Pick a random uncollected check
            long locationId = remainingChecks.get(RANDOM.nextInt(remainingChecks.size()));

            ItemStack checkItem = ModItems.getCheckItemStack(locationId);
            if (!checkItem.isEmpty()) {
                generatedLoot.add(checkItem);
                LOGGER.info("  → ★ SPAWNED AP CHECK #{}", locationId - 44000 + 1);
            }
        } else {
            LOGGER.info("  → No AP check this time");
        }

        return generatedLoot;
    }

    private boolean isVaultChest(ResourceLocation lootTable) {
        return lootTable.getNamespace().equals("the_vault") &&
                lootTable.getPath().contains("chest");
    }

    private boolean isWoodenChest(ResourceLocation lootTable) {
        String path = lootTable.getPath().toLowerCase();
        return path.contains("wooden") || path.contains("wood") ||
                path.contains("common") || path.contains("starter");
    }

    public static class Serializer extends GlobalLootModifierSerializer<APAwareLootModifier> {
        @Override
        public APAwareLootModifier read(ResourceLocation location, JsonObject object, LootItemCondition[] conditions) {
            return new APAwareLootModifier(conditions);
        }

        @Override
        public JsonObject write(APAwareLootModifier instance) {
            return makeConditions(instance.conditions);
        }
    }
}