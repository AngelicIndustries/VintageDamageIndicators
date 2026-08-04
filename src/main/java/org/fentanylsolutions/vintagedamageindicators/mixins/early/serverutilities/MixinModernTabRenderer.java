package org.fentanylsolutions.vintagedamageindicators.mixins.early.serverutilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.boss.BossStatus;

import org.fentanylsolutions.vintagedamageindicators.Config;
import org.fentanylsolutions.vintagedamageindicators.client.HudOverlayLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import serverutils.client.tab.ModernTabRenderer;

@Mixin(value = ModernTabRenderer.class, remap = false)
public abstract class MixinModernTabRenderer {

    @Unique
    private int vdi$shiftX;

    @Unique
    private int vdi$shiftY;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void vdi$resetPlayerListShift(ScaledResolution resolution, CallbackInfo ci) {
        this.vdi$shiftX = 0;
        this.vdi$shiftY = 0;
    }

    @ModifyVariable(method = "render", at = @At("STORE"), index = 36, remap = false)
    private int vdi$capturePlayerListWidth(int width, ScaledResolution resolution) {
        Minecraft minecraft = Minecraft.getMinecraft();
        HudOverlayLayout.OverlayRect hudRect = HudOverlayLayout.isHudVisible(minecraft)
            ? HudOverlayLayout.getConfiguredHudRect(resolution.getScaledWidth(), resolution.getScaledHeight())
            : null;
        int bossNameWidth = BossStatus.bossName == null ? 0
            : minecraft.fontRenderer.getStringWidth(BossStatus.bossName);
        HudOverlayLayout.BossBarLayout bossLayout = HudOverlayLayout.resolveBossBarLayout(
            resolution.getScaledWidth(),
            resolution.getScaledHeight(),
            hudRect,
            !Config.disableBossBar && BossStatus.bossName != null && BossStatus.statusBarTime > 0,
            bossNameWidth);
        HudOverlayLayout.PlayerListShift shift = HudOverlayLayout.resolveCustomPlayerListShift(
            resolution.getScaledWidth(),
            resolution.getScaledHeight(),
            hudRect,
            bossLayout,
            width);
        this.vdi$shiftX = shift.x;
        this.vdi$shiftY = shift.y;
        return width;
    }

    @ModifyVariable(method = "render", at = @At("STORE"), index = 37, remap = false)
    private int vdi$shiftPlayerListCenter(int center) {
        return center + this.vdi$shiftX;
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 10, ordinal = 0), remap = false)
    private int vdi$shiftPlayerListTop(int top) {
        return top + this.vdi$shiftY;
    }
}
