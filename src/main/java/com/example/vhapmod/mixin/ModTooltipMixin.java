package com.example.vhapmod.mixin;

import com.example.vhapmod.APSkillLockManager;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.client.gui.screen.player.legacy.widget.ResearchWidget;
import iskallia.vault.client.util.TooltipUtil;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.research.ResearchTree;
import iskallia.vault.research.type.Research;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ResearchWidget.class, remap = false)
public abstract class ModTooltipMixin {

    @Shadow
    private String researchName;

    @Shadow
    private boolean locked;

    @Shadow
    private ResearchTree researchTree;

    @Shadow
    public abstract boolean m_5953_(double mouseX, double mouseY);

    @Inject(method = "renderHover", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void addAPTooltip(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!this.m_5953_(mouseX, mouseY)) {
            return;
        }

        List<FormattedCharSequence> tooltip = new ArrayList<>();
        tooltip.add(new TextComponent(this.researchName).getVisualOrderText());

        if (!this.researchTree.getResearchesDone().contains(this.researchName)
                && !APSkillLockManager.isModUnlockedClient(this.researchName)) {
            tooltip.add(new TextComponent("Locked by Archipelago")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .getVisualOrderText());
        }

        if (this.locked) {
            List<Research> dependencies = ModConfigs.SKILL_GATES.getGates().getDependencyResearches(this.researchName);
            if (!dependencies.isEmpty()) {
                String requiresText = ModConfigs.SKILL_GATES.getGates().hasEitherSkillGate(this.researchName)
                        ? "Requires any of:"
                        : "Requires:";
                tooltip.add(new TextComponent(requiresText)
                        .withStyle(ChatFormatting.RED)
                        .getVisualOrderText());
                dependencies.forEach(dependency ->
                        tooltip.add(new TextComponent("- " + dependency.getName())
                                .withStyle(ChatFormatting.RED)
                                .getVisualOrderText())
                );
            }
        }

        List<Research> lockedBy = ModConfigs.SKILL_GATES.getGates().getLockedByResearches(this.researchName);
        if (!lockedBy.isEmpty()) {
            tooltip.add(new TextComponent("Cannot be unlocked alongside:")
                    .withStyle(ChatFormatting.RED)
                    .getVisualOrderText());
            lockedBy.forEach(conflict ->
                    tooltip.add(new TextComponent("- " + conflict.getName())
                            .withStyle(ChatFormatting.RED)
                            .getVisualOrderText())
            );
        }

        TooltipUtil.renderTooltip(poseStack, tooltip, mouseX, mouseY, Integer.MAX_VALUE, Integer.MAX_VALUE);
        ci.cancel();
    }
}
