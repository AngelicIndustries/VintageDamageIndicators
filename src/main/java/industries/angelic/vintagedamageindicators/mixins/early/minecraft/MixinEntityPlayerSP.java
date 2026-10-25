package industries.angelic.vintagedamageindicators.mixins.early.minecraft;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.VintageDamageIndicators;
import industries.angelic.vintagedamageindicators.event.ParticleEventHandler;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Inject(method = "onCriticalHit", at = @At("HEAD"), cancellable = true)
    private void onCriticalHitInject(Entity target, CallbackInfo ci) {
        if (Config.disableVanillaCriticalParticles) {
            ci.cancel();
            return;
        }

        VintageDamageIndicators.debug(
            "Critical Hit on " + target.getClass()
                .getSimpleName());
    }

    @Inject(method = "onEnchantmentCritical", at = @At("HEAD"), cancellable = true)
    private void onEnchantmentCriticalInject(Entity target, CallbackInfo ci) {
        if (Config.disableVanillaCriticalParticles) {
            ci.cancel();
            return;
        }
        if (Config.criticalParticlesEnabled) {
            VintageDamageIndicators.debug("Spawning crit FX for: " + target);
            ParticleEventHandler.doCritical(target);
        }
    }
}
