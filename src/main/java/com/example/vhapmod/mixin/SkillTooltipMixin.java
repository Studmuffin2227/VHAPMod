package com.example.vhapmod.mixin;

import com.example.vhapmod.APSkillLockManager;
import com.mojang.blaze3d.vertex.PoseStack;
import iskallia.vault.client.gui.overlay.VaultBarOverlay;
import iskallia.vault.client.gui.screen.player.legacy.widget.SkillWidget;
import iskallia.vault.client.util.TooltipUtil;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.skill.base.TieredSkill;
import iskallia.vault.skill.tree.ExpertiseTree;
import iskallia.vault.skill.tree.SkillTree;
import iskallia.vault.skill.tree.TalentTree;
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

@Mixin(value = SkillWidget.class, remap = false)
public abstract class SkillTooltipMixin {

    @Shadow
    protected SkillTree skillTree;

    @Shadow
    TieredSkill skill;

    @Shadow
    protected abstract boolean isLocked();

    @Shadow
    public abstract boolean m_5953_(double mouseX, double mouseY);

    @Inject(method = "renderHover", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void addAPTooltip(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!this.m_5953_(mouseX, mouseY)) {
            return;
        }

        List<FormattedCharSequence> tooltip = new ArrayList<>();
        tooltip.add(new TextComponent(this.skill.getName()).getVisualOrderText());

        if (isLockedByArchipelago()) {
            tooltip.add(new TextComponent("Locked by Archipelago")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .getVisualOrderText());
        }

        if (this.isLocked()) {
            List<String> dependencies = ModConfigs.SKILL_GATES.getGates().getDependencySkills(this.skill.getId());
            if (!dependencies.isEmpty()) {
                tooltip.add(new TextComponent("Requires:")
                        .withStyle(ChatFormatting.RED)
                        .getVisualOrderText());
                dependencies.forEach(dependency ->
                        this.skillTree.getForId(dependency).ifPresent(skill ->
                                tooltip.add(new TextComponent("- " + skill.getName())
                                        .withStyle(ChatFormatting.RED)
                                        .getVisualOrderText()))
                );
            }
        }

        List<String> lockedBy = ModConfigs.SKILL_GATES.getGates().getLockedBySkills(this.skill.getId());
        if (!lockedBy.isEmpty()) {
            tooltip.add(new TextComponent("Cannot be unlocked alongside:")
                    .withStyle(ChatFormatting.RED)
                    .getVisualOrderText());
            lockedBy.forEach(conflict ->
                    this.skillTree.getForId(conflict).ifPresent(skill ->
                            tooltip.add(new TextComponent("- " + skill.getName())
                                    .withStyle(ChatFormatting.RED)
                                    .getVisualOrderText()))
            );
        }

        if (this.skill.getUnmodifiedTier() < this.skill.getMaxLearnableTier()) {
            int unlockLevel = this.skill.getUnlockLevel();
            if (VaultBarOverlay.vaultLevel < unlockLevel) {
                tooltip.add(new TextComponent("Requires Vault Level " + unlockLevel)
                        .withStyle(ChatFormatting.RED)
                        .getVisualOrderText());
            }
        }

        TooltipUtil.renderTooltip(poseStack, tooltip, mouseX, mouseY, Integer.MAX_VALUE, Integer.MAX_VALUE);
        ci.cancel();
    }

    private boolean isLockedByArchipelago() {
        // Use skill ID instead of display name for more reliable matching
        String skillId = this.skill.getId();
        String skillName = this.skill.getName();

        boolean isLocked;
        if (this.skillTree instanceof TalentTree) {
            isLocked = !APSkillLockManager.isTalentUnlockedClient(skillId);
        } else if (this.skillTree instanceof ExpertiseTree) {
            isLocked = !APSkillLockManager.isExpertiseUnlockedClient(skillId);
        } else {
            isLocked = !APSkillLockManager.isSkillUnlockedClient(skillId);
        }

        org.apache.logging.log4j.LogManager.getLogger().info(
            "[SkillTooltip] Checking lock for '{}' (ID: '{}') - Locked: {}",
            skillName, skillId, isLocked
        );

        return isLocked;
    }
}
