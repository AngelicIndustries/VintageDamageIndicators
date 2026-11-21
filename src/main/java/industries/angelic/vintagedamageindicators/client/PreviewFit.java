package industries.angelic.vintagedamageindicators.client;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import industries.angelic.vintagedamageindicators.VintageDamageIndicators;

/**
 * How big a mob really draws in the HUD preview. Renderers scale and shift their models in ways the hitbox doesn't
 * tell, so each mob is drawn once offscreen, the way the HUD draws it, and the pixels it covered are read back. The
 * result is kept per mob class, size, age and angle.
 */
public final class PreviewFit {

    private static final int SIZE = 256;
    private static final int ANCHOR_X = SIZE / 2;
    private static final int ANCHOR_Y = SIZE * 3 / 4;
    /** About how many pixels a mob is drawn tall or wide for measuring, before checking it fit. */
    private static final float MEASURE_PIXELS = 72.0F;
    private static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;

    /** How long a mob that drew nothing waits before it is measured again: it may have been fading in or out. */
    private static final long NOTHING_RETRY_MILLIS = 1000L;
    /** What get gives for a mob that drew nothing at all, like one made only of particles. */
    public static final Bounds NOTHING = new Bounds(0.0F, 0.0F, 0.0F, 0.0F);

    private static final Map<String, Bounds> CACHE = new HashMap<>();
    private static final Map<String, Long> NOTHING_SINCE = new HashMap<>();
    private static final FloatBuffer DEPTH = BufferUtils.createFloatBuffer(SIZE * SIZE);
    private static final ByteBuffer ALPHA = BufferUtils.createByteBuffer(SIZE * SIZE);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static Framebuffer framebuffer;

    private PreviewFit() {}

    /**
     * The mob's drawn bounds, in drawEntity scale units around its feet; NOTHING when it drew nothing, null when they
     * can't be measured.
     */
    public static Bounds get(EntityLivingBase entity, boolean child, float yaw, float pitch, float roll) {
        if (!OpenGlHelper.isFramebufferEnabled()) {
            return null;
        }
        String key = entity.getClass()
            .getName() + '|'
            + child
            + '|'
            + Math.round(entity.width * 100.0F)
            + '|'
            + Math.round(entity.height * 100.0F)
            + '|'
            + Math.round(yaw)
            + '|'
            + Math.round(pitch)
            + '|'
            + Math.round(roll);
        Long nothingSince = NOTHING_SINCE.get(key);
        if (CACHE.containsKey(key)
            && (nothingSince == null || Minecraft.getSystemTime() - nothingSince < NOTHING_RETRY_MILLIS)) {
            return CACHE.get(key);
        }
        Bounds bounds = null;
        try {
            bounds = measure(entity, yaw, pitch, roll);
        } catch (Throwable t) {
            VintageDamageIndicators.LOG.warn(
                "Couldn't measure the preview of {}, it uses its hitbox instead",
                entity.getClass()
                    .getName(),
                t);
        }
        CACHE.put(key, bounds);
        if (bounds == NOTHING) {
            NOTHING_SINCE.put(key, Minecraft.getSystemTime());
        } else {
            NOTHING_SINCE.remove(key);
        }
        return bounds;
    }

    public static void clear() {
        CACHE.clear();
        NOTHING_SINCE.clear();
    }

    private static Bounds measure(EntityLivingBase entity, float yaw, float pitch, float roll) {
        float scale = MEASURE_PIXELS / Math.max(0.5F, Math.max(entity.width, entity.height));
        // A model much bigger than its hitbox runs off the edge, so it's drawn smaller until it fits
        for (int attempt = 0; attempt < 3; attempt++) {
            int[] box = draw(entity, scale, yaw, pitch, roll);
            if (box == null) {
                return NOTHING;
            }
            if (box[0] > 0 && box[1] > 0 && box[2] < SIZE - 1 && box[3] < SIZE - 1) {
                return new Bounds(
                    (box[0] - ANCHOR_X) / scale,
                    (box[1] - ANCHOR_Y) / scale,
                    (box[2] + 1 - ANCHOR_X) / scale,
                    (box[3] + 1 - ANCHOR_Y) / scale);
            }
            scale /= 4.0F;
        }
        return null;
    }

    /** Draws the mob offscreen and returns the box it covered (left, top, right, bottom), or null if nothing. */
    private static int[] draw(EntityLivingBase entity, float scale, float yaw, float pitch, float roll) {
        int previousFramebuffer = GL11.glGetInteger(GL_FRAMEBUFFER_BINDING);
        VIEWPORT.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
        if (framebuffer == null) {
            framebuffer = new Framebuffer(SIZE, SIZE, true);
        }
        framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(true);
        // The same projection the HUD is drawn with, so the mob draws exactly as it would there
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, SIZE, SIZE, 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
        try {
            HudEntityRenderer.drawEntity(ANCHOR_X, ANCHOR_Y, scale, yaw, pitch, roll, entity);
            DEPTH.clear();
            GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, DEPTH);
            ALPHA.clear();
            GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, ALPHA);
        } finally {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, previousFramebuffer);
            GL11.glViewport(VIEWPORT.get(0), VIEWPORT.get(1), VIEWPORT.get(2), VIEWPORT.get(3));
        }

        int[] box = coveredBox(true);
        // Translucent and glowing mobs write no depth, but they do leave color behind
        return box != null ? box : coveredBox(false);
    }

    /** The box of pixels the mob covered, by depth (solid parts) or by color (anything drawn); null if none. */
    private static int[] coveredBox(boolean byDepth) {
        int left = SIZE, top = SIZE, right = -1, bottom = -1;
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                int i = row * SIZE + column;
                boolean covered = byDepth ? DEPTH.get(i) < 0.9999F : (ALPHA.get(i) & 0xFF) > 8;
                if (covered) {
                    // Read from the bottom row up; the HUD counts from the top down
                    int y = SIZE - 1 - row;
                    left = Math.min(left, column);
                    right = Math.max(right, column);
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        return right < 0 ? null : new int[] { left, top, right, bottom };
    }

    /** Where the drawing reaches, in drawEntity scale units from the mob's feet; y grows downward. */
    public static final class Bounds {

        public final float left;
        public final float top;
        public final float right;
        public final float bottom;

        Bounds(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public float width() {
            return this.right - this.left;
        }

        public float height() {
            return this.bottom - this.top;
        }

        public float centerX() {
            return (this.left + this.right) / 2.0F;
        }
    }
}
