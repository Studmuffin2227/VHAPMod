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

@Mixin(targets = "iskallia.vault.network.message.TalentLevelMessage", remap = false)
public class TalentLevelMessageMixin {
    
    private static final Logger LOGGER = LogManager.getLogger("TalentMixin");
    
    @Inject(
        method = "upgradeTalent",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void preventTalentUpgrade(iskallia.vault.network.message.TalentLevelMessage message, ServerPlayer player, CallbackInfo ci) {
        LOGGER.info("=== TALENT UPGRADE ATTEMPT ===");
        
        // Access the talentName field via reflection or get it from the message parameter
        // We'll use a try-catch to safely get the field
        try {
            java.lang.reflect.Field field = message.getClass().getDeclaredField("talentName");
            field.setAccessible(true);
            String talentName = (String) field.get(message);
            
            LOGGER.info("Talent: {}", talentName);
            LOGGER.info("Player: {}", player.getName().getString());
            
            String normalizedName = "vhtalent:" + talentName.toLowerCase().replace(" ", "_");
            
            if (!APSkillLockManager.isTalentUnlockedSilent(player, normalizedName)) {
                LOGGER.warn("BLOCKING talent upgrade!");
                
                MutableComponent msg = new TextComponent("[AP] ")
                    .withStyle(ChatFormatting.RED)
                    .append(new TextComponent(talentName + " is locked! You need to receive it from Archipelago first."));
                
                player.sendMessage(msg, player.getUUID());
                ci.cancel();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get talent name from message", e);
        }
    }
}
