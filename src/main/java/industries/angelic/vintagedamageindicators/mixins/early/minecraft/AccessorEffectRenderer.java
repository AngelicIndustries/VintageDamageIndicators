package industries.angelic.vintagedamageindicators.mixins.early.minecraft;

import java.util.List;

import net.minecraft.client.particle.EffectRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The world's particles by layer, for drawing the ones around a mob made of particles (TargetParticles). */
@Mixin(EffectRenderer.class)
public interface AccessorEffectRenderer {

    @Accessor("fxLayers")
    List<?>[] vdi$getFxLayers();
}
