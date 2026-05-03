package com.example.vhapmod.mixin;

import com.example.vhapmod.APSkillLockManager;
import iskallia.vault.research.type.Research;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

@Mixin(targets = "iskallia.vault.research.ResearchTree", remap = false)
public abstract class ModResearchPreventionMixin {

    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(
            method = "research(Liskallia/vault/research/type/Research;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void preventLockedModResearch(Research research, CallbackInfo ci) {
        if (!Thread.currentThread().getName().contains("Server thread")) {
            return;
        }

        try {
            String modName = research.getName();

            // Try to get the player UUID from this ResearchTree instance
            UUID playerId = null;
            try {
                // Search for UUID field in class hierarchy.
                Class<?> current = this.getClass();
                while (current != null && current != Object.class && playerId == null) {
                    for (Field field : current.getDeclaredFields()) {
                        if (field.getType() == UUID.class) {
                            field.setAccessible(true);
                            playerId = (UUID) field.get(this);
                            break;
                        }
                    }
                    current = current.getSuperclass();
                }
            } catch (Exception ignored) {
            }

            // Get the player from the server
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }

            ServerPlayer player = playerId != null ? server.getPlayerList().getPlayer(playerId) : null;

            // Fallback for single-player / single-user sessions where UUID is not attached to tree.
            if (player == null) {
                List<ServerPlayer> players = server.getPlayerList().getPlayers();
                if (players.size() == 1) {
                    player = players.get(0);
                }
            }

            if (player != null) {
                boolean unlocked = APSkillLockManager.isModUnlocked(player, modName);

                if (!unlocked) {
                    ci.cancel();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error in mod research prevention", e);
        }
    }
}