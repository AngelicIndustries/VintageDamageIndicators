package industries.angelic.vintagedamageindicators.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S0BPacketAnimation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.VintageDamageIndicators;
import industries.angelic.vintagedamageindicators.event.ParticleEventHandler;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(
        method = "handleAnimation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/EffectRenderer;addEffect(Lnet/minecraft/client/particle/EntityFX;)V",
            shift = At.Shift.BEFORE),
        cancellable = true)
    private void onCritEffect(S0BPacketAnimation packetIn, CallbackInfo ci) {
        int animationType = packetIn.func_148977_d();

        if (animationType == 4 || animationType == 5) {
            if (Config.disableVanillaCriticalParticles) {
                ci.cancel();
            }

            if (Config.criticalParticlesEnabled) {
                Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(packetIn.func_148978_c());
                VintageDamageIndicators.debug("Spawning crit FX for: " + entity);
                ParticleEventHandler.doCritical(entity);
            }
        }
    }
}
