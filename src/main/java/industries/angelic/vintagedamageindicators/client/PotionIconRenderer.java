package industries.angelic.vintagedamageindicators.client;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import org.lwjgl.opengl.GL11;

/**
 * Adapts Forge's inventory potion icon hook to VDI's compact potion strip.
 */
public final class PotionIconRenderer {

    private static final int INVENTORY_ICON_OFFSET_X = 6;
    private static final int INVENTORY_ICON_OFFSET_Y = 7;
    private static final Map<Class<?>, Boolean> CUSTOM_ICON_CLASSES = Collections
        .synchronizedMap(new WeakHashMap<Class<?>, Boolean>());

    private PotionIconRenderer() {}

    public static boolean hasIcon(Potion potion) {
        return potion != null && (potion.hasStatusIcon() || hasCustomIcon(potion));
    }

    public static boolean hasCustomIcon(Potion potion) {
        if (potion == null) {
            return false;
        }

        Class<?> potionClass = potion.getClass();
        Boolean cached = CUSTOM_ICON_CLASSES.get(potionClass);
        if (cached != null) {
            return cached.booleanValue();
        }

        boolean custom = declaresCustomIcon(potionClass);
        CUSTOM_ICON_CLASSES.put(potionClass, Boolean.valueOf(custom));
        return custom;
    }

    /**
     * Forge passes the potion panel origin to custom renderers; its standard icon
     * is drawn at +6,+7 inside that panel. Translate VDI's desired icon position
     * back to that origin so compliant custom renderers land in the strip.
     */
    public static void drawCustomIcon(Potion potion, PotionEffect effect, Minecraft minecraft, int iconX, int iconY) {
        if (!hasCustomIcon(potion) || effect == null) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            potion.renderInventoryEffect(
                iconX - INVENTORY_ICON_OFFSET_X,
                iconY - INVENTORY_ICON_OFFSET_Y,
                effect,
                minecraft);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static boolean declaresCustomIcon(Class<?> potionClass) {
        try {
            Method method = potionClass
                .getMethod("renderInventoryEffect", Integer.TYPE, Integer.TYPE, PotionEffect.class, Minecraft.class);
            return method.getDeclaringClass() != Potion.class;
        } catch (NoSuchMethodException | SecurityException ignored) {
            return false;
        }
    }
}
