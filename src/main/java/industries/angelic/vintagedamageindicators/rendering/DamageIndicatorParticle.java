package industries.angelic.vintagedamageindicators.rendering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.util.Util;

// TODO: cache the critical text localization and hook into I18n to refresh it whenever the language is changed

@SideOnly(Side.CLIENT)
public class DamageIndicatorParticle extends EntityFX {

    private static final List<DamageIndicatorParticle> ACTIVE_PARTICLES = new ArrayList<>();

    private boolean isCritical;
    private String particleText;
    private boolean isHeal;
    private int damage;
    private boolean doGrow;
    public boolean renderOnTop;
    private float scale;
    private int color;
    private int shadowColor;

    /* Critical particle constructor */
    public DamageIndicatorParticle(World world, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn,
        double ySpeedIn, double zSpeedIn) {
        this(world, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, 0);
        this.isCritical = true;
        this.particleGravity = Config.criticalParticleGravity;
        this.particleText = I18n.format("vintagedamageindicators.critical");
        this.particleScale = Config.criticalParticleScale;
        this.particleMaxAge = Config.criticalParticleLifespan;
        this.scale = Config.criticalParticleScale;
        this.color = Util.applyAlpha(Config.criticalParticleColor, Config.criticalParticleTransparency);
        this.shadowColor = Util.applyAlpha(0, Config.criticalParticleTransparency);
    }

    /* Normal constructor */
    public DamageIndicatorParticle(World world, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn,
        double ySpeedIn, double zSpeedIn, int damage) {
        super(world, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        this.isCritical = false;
        this.damage = Math.abs(damage);
        this.isHeal = damage < 0;
        this.doGrow = true;
        this.renderOnTop = false;
        setSize(0.2f, 0.2f);
        this.posY += this.height * 1.1f;
        setPosition(xCoordIn, yCoordIn, zCoordIn);
        this.motionX = xSpeedIn;
        this.motionY = ySpeedIn;
        this.motionZ = zSpeedIn;
        this.scale = Config.damageParticleScale;

        float motionMagnitude = MathHelper
            .sqrt_double((this.motionX * this.motionX) + (this.motionY * this.motionY) + (this.motionZ * this.motionZ));
        this.motionX = (this.motionX / motionMagnitude) * 0.12d;
        this.motionY = (this.motionY / motionMagnitude) * 0.12d;
        this.motionZ = (this.motionZ / motionMagnitude) * 0.12d;

        this.particleTextureJitterX = 1.5f;
        this.particleTextureJitterY = 1.5f;
        this.particleGravity = Config.damageParticleGravity;
        this.particleScale = Config.damageParticleScale;
        this.particleMaxAge = Config.damageParticleLifespan;
        this.particleAge = 0;

        this.color = Util
            .applyAlpha(this.isHeal ? Config.healColor : Config.hurtColor, Config.damageParticleTransparency);
        int color_ = this.isHeal ? Config.healColor : Config.hurtColor;
        int r = (color_ >> 16) & 0xFF;
        int g = (color_ >> 8) & 0xFF;
        int b = color_ & 0xFF;
        r = (int) (r / 5.0f);
        g = (int) (g / 5.0f);
        b = (int) (b / 5.0f);
        int shadowColor_ = (0xFF << 24) | (r << 16) | (g << 8) | b;
        this.shadowColor = Util.applyAlpha(shadowColor_, Config.damageParticleTransparency);
        this.particleText = String.valueOf(this.damage);
        ACTIVE_PARTICLES.add(this);
    }

    @Override
    public void renderParticle(Tessellator p_renderParticle_1_, float par2, float par3, float par4, float par5,
        float par6, float par7) {
        // No-op: rendering moved to renderAllParticles() called from RenderWorldLastEvent
        // to bypass Iris/Angelica deferred shader pipeline dimming
    }

    private void doRender(float partialTicks) {
        this.rotationYaw = -Minecraft.getMinecraft().thePlayer.rotationYaw;
        this.rotationPitch = Minecraft.getMinecraft().thePlayer.rotationPitch;

        float locX = (float) ((this.prevPosX + ((this.posX - this.prevPosX) * partialTicks)) - interpPosX);
        float locY = (float) ((this.prevPosY + ((this.posY - this.prevPosY) * partialTicks)) - interpPosY);
        float locZ = (float) ((this.prevPosZ + ((this.posZ - this.prevPosZ) * partialTicks)) - interpPosZ);

        GL11.glPushMatrix();
        if (this.renderOnTop) {
            GL11.glDepthFunc(GL11.GL_ALWAYS);
        } else {
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
        GL11.glTranslatef(locX, locY, locZ);
        GL11.glRotatef(this.rotationYaw, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(this.rotationPitch, 1.0f, 0.0f, 0.0f);
        GL11.glScalef(-1.0f, -1.0f, 1.0f);
        GL11.glScaled(this.particleScale * 0.008d, this.particleScale * 0.008d, this.particleScale * 0.008d);
        if (this.isCritical) {
            GL11.glScaled(0.5d, 0.5d, 0.5d);
        }
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 0.003662109f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        if (Config.dropShadow) {
            fontRenderer.drawString(
                this.particleText,
                (-MathHelper.floor_float(fontRenderer.getStringWidth(this.particleText) / 2.0f)) + 1,
                (-MathHelper.floor_float(fontRenderer.FONT_HEIGHT / 2.0f)) + 1,
                this.shadowColor);
        }
        fontRenderer.drawString(
            this.particleText,
            -MathHelper.floor_float(fontRenderer.getStringWidth(this.particleText) / 2.0f),
            -MathHelper.floor_float(fontRenderer.FONT_HEIGHT / 2.0f),
            this.color);

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glPopMatrix();

        if (this.doGrow) {
            this.particleScale *= 1.08f;
            if (this.particleScale > this.scale * 3.0d) {
                this.doGrow = false;
            }
        } else {
            this.particleScale *= 0.96f;
        }
    }

    public static void renderAllParticles(float partialTicks) {
        if (ACTIVE_PARTICLES.isEmpty()) {
            return;
        }

        Iterator<DamageIndicatorParticle> it = ACTIVE_PARTICLES.iterator();
        while (it.hasNext()) {
            if (it.next().isDead) {
                it.remove();
            }
        }

        if (ACTIVE_PARTICLES.isEmpty()) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        for (DamageIndicatorParticle particle : ACTIVE_PARTICLES) {
            particle.doRender(partialTicks);
        }

        GL11.glPopAttrib();
    }

    public static void clearAll() {
        ACTIVE_PARTICLES.clear();
    }

    public int getFXLayer() {
        return 3;
    }
}
