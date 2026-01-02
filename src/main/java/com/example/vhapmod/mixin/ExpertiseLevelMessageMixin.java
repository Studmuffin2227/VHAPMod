package com.example.vhapmod.mixin;

import com.example.vhapmod.APSkillLockManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "iskallia.vault.network.message.ExpertiseLevelMessage", remap = false)
public class ExpertiseLevelMessageMixin {
    
    private static final Logger LOGGER = LogManager.getLogger("ExpertiseMixin");
    
    @Inject(
        method = "upgradeExpertise",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void preventExpertiseUpgrade(iskallia.vault.network.message.ExpertiseLevelMessage message, ServerPlayer player, CallbackInfo ci) {
        LOGGER.info("=== EXPERTISE UPGRADE ATTEMPT ===");
        
        // Access the expertiseName field via reflection
        try {
            java.lang.reflect.Field field = message.getClass().getDeclaredField("expertiseName");
            field.setAccessible(true);
            String expertiseName = (String) field.get(message);
            
            LOGGER.info("Expertise: {}", expertiseName);
            LOGGER.info("Player: {}", player.getName().getString());
            
            String normalizedName = "vhexpertise:" + expertiseName.toLowerCase().replace(" ", "_");
            
            if (!APSkillLockManager.isExpertiseUnlockedSilent(player, normalizedName)) {
                LOGGER.warn("BLOCKING expertise upgrade!");
                
                MutableComponent msg = new TextComponent("[AP] ")
                    .withStyle(ChatFormatting.RED)
                    .append(new TextComponent(expertiseName + " is locked! You need to receive it from Archipelago first."));
                
                player.sendMessage(msg, player.getUUID());
                ci.cancel();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get expertise name from message", e);
        }
    }
}
