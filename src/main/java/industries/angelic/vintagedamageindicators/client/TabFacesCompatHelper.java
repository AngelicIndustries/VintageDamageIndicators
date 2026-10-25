package industries.angelic.vintagedamageindicators.client;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import cpw.mods.fml.common.Loader;
import industries.angelic.vintagedamageindicators.VintageDamageIndicators;

public final class TabFacesCompatHelper {

    private static final boolean TAB_FACES_LOADED = Loader.isModLoaded("tabfaces");
    private static final TabFacesBridge BRIDGE = TAB_FACES_LOADED ? TabFacesBridge.load() : null;

    private TabFacesCompatHelper() {}

    public static boolean isLoaded() {
        return TAB_FACES_LOADED;
    }

    public static boolean shouldTrimTabMenu() {
        return BRIDGE != null && BRIDGE.shouldTrimTabMenu();
    }

    public static int adjustColumnWidth(int baseWidth, FontRenderer fontRenderer, List<String> displayNames) {
        if (BRIDGE == null) {
            return baseWidth;
        }
        return BRIDGE.adjustColumnWidth(baseWidth, fontRenderer, displayNames);
    }

    private static final class TabFacesBridge {

        private final Field trimTabMenu;
        private final Field enableFacesInTabMenu;
        private final Field trimTabMenuExtraWidth;
        private final Field trimTabMenuMaxColumnWidth;
        private final Field faceWidth;

        private TabFacesBridge(Class<?> config, Class<?> clientUtil) throws NoSuchFieldException {
            trimTabMenu = config.getField("trimTabMenu");
            enableFacesInTabMenu = config.getField("enableFacesInTabMenu");
            trimTabMenuExtraWidth = config.getField("trimTabMenuExtraWidth");
            trimTabMenuMaxColumnWidth = config.getField("trimTabMenuMaxColumnWidth");
            faceWidth = clientUtil.getField("faceWidth");
        }

        private static TabFacesBridge load() {
            // Released versions use the old namespace; rebranded versions use the new one.
            for (String packageName : new String[] { "industries.angelic.tabfaces",
                "org.fentanylsolutions.tabfaces" }) {
                try {
                    ClassLoader classLoader = TabFacesCompatHelper.class.getClassLoader();
                    Class<?> config = Class.forName(packageName + ".Config", false, classLoader);
                    Class<?> clientUtil = Class.forName(packageName + ".util.ClientUtil", false, classLoader);
                    return new TabFacesBridge(config, clientUtil);
                } catch (ClassNotFoundException ignored) {
                    // Try the other package namespace.
                } catch (ReflectiveOperationException | LinkageError e) {
                    VintageDamageIndicators.debug("TabFaces compatibility unavailable: " + e);
                }
            }
            return null;
        }

        private boolean shouldTrimTabMenu() {
            try {
                return trimTabMenu.getBoolean(null);
            } catch (IllegalAccessException e) {
                VintageDamageIndicators.debug("Could not read TabFaces trim setting: " + e);
                return false;
            }
        }

        private int adjustColumnWidth(int baseWidth, FontRenderer fontRenderer, List<String> displayNames) {
            if (!shouldTrimTabMenu() || fontRenderer == null || displayNames == null) {
                return baseWidth;
            }

            int maxNameWidth = 0;
            for (String displayName : displayNames) {
                if (displayName != null) {
                    maxNameWidth = Math.max(maxNameWidth, fontRenderer.getStringWidth(displayName));
                }
            }

            try {
                int configuredFaceWidth = enableFacesInTabMenu.getBoolean(null) ? faceWidth.getInt(null) : 0;
                int measuredWidth = maxNameWidth + configuredFaceWidth + 12 + 5 + trimTabMenuExtraWidth.getInt(null);
                return Math.max(baseWidth, Math.min(measuredWidth, trimTabMenuMaxColumnWidth.getInt(null)));
            } catch (IllegalAccessException e) {
                VintageDamageIndicators.debug("Could not read TabFaces column settings: " + e);
                return baseWidth;
            }
        }
    }
}
