package industries.angelic.vintagedamageindicators.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import org.lwjgl.opengl.GL11;

import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.util.Util;

public final class HudPreviewParticles {

    private static final double BASE_X_SPEED = 0.001D;
    private static final double BASE_Z_SPEED = 0.001D;
    private static final float SCREEN_HORIZONTAL_MOTION_SCALE = 60.0F;
    private static final float SCREEN_VERTICAL_MOTION_SCALE = 22.0F;
    private static final float BASE_START_X = HudPreviewMath.ENTITY_X;
    private static final float BASE_START_Y = Math
        .max(HudPreviewMath.PREVIEW_CLIP_Y + 8.0F, HudPreviewMath.ENTITY_Y - 30.0F);
    private static final float START_X_JITTER = 2.5F;

    private static final Map<Integer, List<Particle>> PARTICLES_BY_ENTITY = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static long lastUpdateMillis = -1L;
    private static float tickAccumulator = 0.0F;

    private HudPreviewParticles() {}

    public static void tick() {
        if (PARTICLES_BY_ENTITY.isEmpty()) {
            lastUpdateMillis = Minecraft.getSystemTime();
            tickAccumulator = 0.0F;
            return;
        }

        long now = Minecraft.getSystemTime();
        if (lastUpdateMillis < 0L) {
            lastUpdateMillis = now;
            return;
        }

        float deltaTicks = Math.min((now - lastUpdateMillis) / 50.0F, 5.0F);
        lastUpdateMillis = now;
        if (deltaTicks <= 0.0F) {
            return;
        }

        tickAccumulator += deltaTicks;
        int wholeTicks = MathHelper.floor_float(tickAccumulator);
        if (wholeTicks <= 0) {
            return;
        }
        tickAccumulator -= wholeTicks;

        Iterator<Map.Entry<Integer, List<Particle>>> mapIterator = PARTICLES_BY_ENTITY.entrySet()
            .iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<Integer, List<Particle>> entry = mapIterator.next();
            List<Particle> particles = entry.getValue();
            Iterator<Particle> particleIterator = particles.iterator();
            while (particleIterator.hasNext()) {
                Particle particle = particleIterator.next();
                boolean alive = true;
                for (int tick = 0; tick < wholeTicks && alive; tick++) {
                    alive = particle.tick();
                }
                if (!alive) {
                    particleIterator.remove();
                }
            }
            if (particles.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    public static void spawnDamage(EntityLivingBase target, int damage) {
        if (!Config.hudPreviewParticlesEnabled || target == null || damage == 0) {
            return;
        }

        boolean heal = damage < 0;
        int baseColor = heal ? Config.healColor : Config.hurtColor;
        int shadowColor = getShadowColor(baseColor);
        addParticle(
            target.getEntityId(),
            createParticle(
                Integer.toString(Math.abs(damage)),
                false,
                Config.damageParticleGravity,
                Config.damageParticleScale,
                Config.damageParticleLifespan,
                Util.applyAlpha(baseColor, Config.damageParticleTransparency),
                Util.applyAlpha(shadowColor, Config.damageParticleTransparency)));
    }

    public static void spawnCritical(Entity target) {
        if (!Config.hudPreviewParticlesEnabled || !Config.criticalParticlesEnabled || target == null) {
            return;
        }

        int entityId = target.getEntityId();
        addParticle(
            entityId,
            createParticle(
                I18n.format("vintagedamageindicators.critical"),
                true,
                Config.criticalParticleGravity,
                Config.criticalParticleScale,
                Config.criticalParticleLifespan,
                Util.applyAlpha(Config.criticalParticleColor, Config.criticalParticleTransparency),
                Util.applyAlpha(0, Config.criticalParticleTransparency)));
    }

    public static void clear(int entityId) {
        PARTICLES_BY_ENTITY.remove(entityId);
    }

    public static void render(EntityLivingBase target, FontRenderer fontRenderer, int screenX, int screenY,
        float hudScale, int scaledHeight, int guiScaleFactor) {
        if (!Config.hudPreviewParticlesEnabled || target == null) {
            return;
        }

        List<Particle> particles = PARTICLES_BY_ENTITY.get(target.getEntityId());
        if (particles == null || particles.isEmpty()) {
            return;
        }

        int scissorX = Math.round((screenX + HudPreviewMath.PREVIEW_CLIP_X * hudScale) * guiScaleFactor);
        int scissorWidth = Math.round(HudPreviewMath.PREVIEW_CLIP_WIDTH * hudScale * guiScaleFactor);
        int scissorHeight = Math.round(HudPreviewMath.PREVIEW_CLIP_HEIGHT * hudScale * guiScaleFactor);
        int scissorY = Math.round(
            (scaledHeight - (screenY + (HudPreviewMath.PREVIEW_CLIP_Y + HudPreviewMath.PREVIEW_CLIP_HEIGHT) * hudScale))
                * guiScaleFactor);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, Math.max(0, scissorWidth), Math.max(0, scissorHeight));
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        for (Particle particle : particles) {
            particle.render(fontRenderer);
        }

        GL11.glPopAttrib();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void addParticle(int entityId, Particle particle) {
        PARTICLES_BY_ENTITY.computeIfAbsent(entityId, ignored -> new ArrayList<>())
            .add(particle);
    }

    private static Particle createParticle(String text, boolean critical, float gravity, float configuredScale,
        int maxAgeTicks, int color, int shadowColor) {
        double xSpeed = BASE_X_SPEED;
        double ySpeed = 0.05D * Config.bounceStrength;
        double zSpeed = BASE_Z_SPEED;
        float motionMagnitude = MathHelper.sqrt_double((xSpeed * xSpeed) + (ySpeed * ySpeed) + (zSpeed * zSpeed));
        xSpeed = (xSpeed / motionMagnitude) * 0.12D;
        ySpeed = (ySpeed / motionMagnitude) * 0.12D;
        zSpeed = (zSpeed / motionMagnitude) * 0.12D;
        float horizontalDirection = RANDOM.nextBoolean() ? 1.0F : -1.0F;
        return new Particle(
            text,
            BASE_START_X + ((RANDOM.nextFloat() - 0.5F) * START_X_JITTER * 2.0F),
            BASE_START_Y + (critical ? -2.0F : 0.0F),
            (float) xSpeed,
            (float) ySpeed,
            (float) zSpeed,
            gravity,
            Math.max(0.35F, configuredScale * 0.2F),
            Math.max(1, maxAgeTicks),
            color,
            shadowColor,
            critical,
            horizontalDirection);
    }

    private static int getShadowColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (r / 5 << 16) | (g / 5 << 8) | (b / 5);
    }

    private static final class Particle {

        private final String text;
        private final float baseScale;
        private final int maxAgeTicks;
        private final int color;
        private final int shadowColor;
        private final boolean critical;
        private final float horizontalDirection;

        private float x;
        private float y;
        private float motionX;
        private float motionY;
        private float motionZ;
        private float gravity;
        private float scale;
        private int ageTicks;
        private boolean grow = true;

        private Particle(String text, float x, float y, float motionX, float motionY, float motionZ, float gravity,
            float baseScale, int maxAgeTicks, int color, int shadowColor, boolean critical, float horizontalDirection) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.gravity = gravity;
            this.baseScale = baseScale;
            this.maxAgeTicks = maxAgeTicks;
            this.scale = baseScale;
            this.color = color;
            this.shadowColor = shadowColor;
            this.critical = critical;
            this.horizontalDirection = horizontalDirection;
        }

        private boolean tick() {
            if (this.ageTicks++ >= this.maxAgeTicks) {
                return false;
            }

            this.x += (this.motionX + this.motionZ) * SCREEN_HORIZONTAL_MOTION_SCALE * this.horizontalDirection;
            this.y -= this.motionY * SCREEN_VERTICAL_MOTION_SCALE;
            this.motionY -= 0.04F * this.gravity;
            this.motionX *= 0.98F;
            this.motionY *= 0.98F;
            this.motionZ *= 0.98F;
            return true;
        }

        private void render(FontRenderer fontRenderer) {
            int width = fontRenderer.getStringWidth(this.text);
            int drawX = -MathHelper.floor_float(width / 2.0F);
            int drawY = -MathHelper.floor_float(fontRenderer.FONT_HEIGHT / 2.0F);
            float renderScale = this.critical ? this.scale * 0.5F : this.scale;

            GL11.glPushMatrix();
            GL11.glTranslatef(this.x, this.y, 0.0F);
            GL11.glScalef(renderScale, renderScale, 1.0F);
            if (Config.dropShadow) {
                fontRenderer.drawString(this.text, drawX + 1, drawY + 1, this.shadowColor);
            }
            fontRenderer.drawString(this.text, drawX, drawY, this.color);
            GL11.glPopMatrix();

            if (this.grow) {
                this.scale *= 1.08F;
                if (this.scale > this.baseScale * 3.0F) {
                    this.grow = false;
                }
            } else {
                this.scale *= 0.96F;
            }
        }
    }
}
