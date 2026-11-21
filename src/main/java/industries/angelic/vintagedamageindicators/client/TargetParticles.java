package industries.angelic.vintagedamageindicators.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import industries.angelic.vintagedamageindicators.mixins.early.minecraft.AccessorEffectRenderer;

/**
 * Mobs with no model, only particles (the Aether's whirlwind, Biomes O' Plenty's phantom, Uralte Zauber's wraiths),
 * are drawn in the preview by their particles in the world: the ones around the mob, drawn the way EffectRenderer
 * draws them, facing the viewer, around the mob's feet in the world's axes.
 */
public final class TargetParticles {

    private static final ResourceLocation PARTICLES = new ResourceLocation("textures/particle/particles.png");
    /** How far past its hitbox a particle still belongs to the mob, in blocks. */
    private static final double MARGIN = 0.5D;
    /** How long a mob counts as made of particles after the last one was seen around it, in milliseconds. */
    private static final long SEEN_FOR_MILLIS = 2000L;

    /** The last mob particles were seen around, and when: the HUD has one target at a time. */
    private static int lastSeenEntity = -1;
    private static long lastSeenMillis;

    private TargetParticles() {}

    /** Whether particles were around the mob just now. A mob with no model and none is invisible on purpose. */
    public static boolean around(EntityLivingBase entity) {
        long now = Minecraft.getSystemTime();
        if (anyAround(entity)) {
            lastSeenEntity = entity.getEntityId();
            lastSeenMillis = now;
            return true;
        }
        return lastSeenEntity == entity.getEntityId() && now - lastSeenMillis < SEEN_FOR_MILLIS;
    }

    /** Draws the particles around the mob. Called inside HudEntityRenderer.drawEntity, in the mob's own space. */
    static void draw(EntityLivingBase entity, float partialTicks) {
        List<?>[] layers = layers();
        if (layers == null) {
            return;
        }
        AxisAlignedBB box = entity.boundingBox.expand(MARGIN, MARGIN, MARGIN);
        double oldX = EntityFX.interpPosX;
        double oldY = EntityFX.interpPosY;
        double oldZ = EntityFX.interpPosZ;
        EntityFX.interpPosX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        EntityFX.interpPosY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - entity.yOffset;
        EntityFX.interpPosZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        Minecraft minecraft = Minecraft.getMinecraft();
        Tessellator tessellator = Tessellator.instance;
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            for (int layer = 0; layer < layers.length; layer++) {
                boolean drawing = false;
                for (Object object : layers[layer]) {
                    EntityFX particle = (EntityFX) object;
                    if (!inside(box, particle)) {
                        continue;
                    }
                    // The lit layer draws itself, like EffectRenderer.renderLitParticles
                    if (layer < 3 && !drawing) {
                        minecraft.getTextureManager()
                            .bindTexture(
                                layer == 0 ? PARTICLES
                                    : layer == 1 ? TextureMap.locationBlocksTexture : TextureMap.locationItemsTexture);
                        tessellator.startDrawingQuads();
                        drawing = true;
                    }
                    tessellator.setBrightness(particle.getBrightnessForRender(partialTicks));
                    particle.renderParticle(
                        tessellator,
                        partialTicks,
                        ActiveRenderInfo.rotationX,
                        ActiveRenderInfo.rotationXZ,
                        ActiveRenderInfo.rotationZ,
                        ActiveRenderInfo.rotationYZ,
                        ActiveRenderInfo.rotationXY);
                }
                if (drawing) {
                    tessellator.draw();
                }
            }
        } finally {
            EntityFX.interpPosX = oldX;
            EntityFX.interpPosY = oldY;
            EntityFX.interpPosZ = oldZ;
            GL11.glDepthMask(true);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        }
    }

    private static boolean anyAround(EntityLivingBase entity) {
        List<?>[] layers = layers();
        if (layers == null) {
            return false;
        }
        AxisAlignedBB box = entity.boundingBox.expand(MARGIN, MARGIN, MARGIN);
        for (List<?> layer : layers) {
            for (Object object : layer) {
                if (inside(box, (EntityFX) object)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean inside(AxisAlignedBB box, EntityFX particle) {
        return particle.posX >= box.minX && particle.posX <= box.maxX
            && particle.posY >= box.minY
            && particle.posY <= box.maxY
            && particle.posZ >= box.minZ
            && particle.posZ <= box.maxZ;
    }

    private static List<?>[] layers() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.effectRenderer == null ? null
            : ((AccessorEffectRenderer) minecraft.effectRenderer).vdi$getFxLayers();
    }
}
