package com.example.vhapmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "iskallia.vault.gear.data.VaultGearData", remap = false)
public class VaultGearLevelMixin {

    @Shadow
    private int itemLevel;

    @Inject(method = "setItemLevel", at = @At("TAIL"), remap = false)
    private void removeLevelCap(int itemLevel, CallbackInfo ci) {
        // The method just set this.itemLevel = Math.min(itemLevel, 100)
        // We override it to be the full value
        this.itemLevel = itemLevel;
    }
}