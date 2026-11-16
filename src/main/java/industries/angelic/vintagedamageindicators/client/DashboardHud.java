package industries.angelic.vintagedamageindicators.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.vintagedamageindicators.Config;

/**
 * The HUD indicator as an element of Dashboard's HUD editor, to move, resize or hide there. The only class using
 * Dashboard's API, so it is only loaded with Dashboard installed (HudEventHandler.DASHBOARD).
 */
@SideOnly(Side.CLIENT)
public final class DashboardHud {

    /** The panel's width the last time it was drawn; the potion strip can widen it. */
    private static int contentWidth = HudPreviewMath.PANEL_WIDTH;

    private static final HudElement PANEL = new HudElement(
        "vintagedamageindicators:hud_indicator",
        (screenWidth,
            screenHeight) -> bounds(HudOverlayLayout.getDrawnHudRect(screenWidth, screenHeight, contentWidth)));

    private DashboardHud() {}

    public static void register() {
        PANEL.setActiveCheck(() -> Config.hudIndicatorEnabled)
            .setPreview(
                (screenWidth, screenHeight) -> HudRegistry.render(PANEL, () -> drawSample(screenWidth, screenHeight)));
        HudRegistry.register(PANEL);
    }

    /** False when the player hid the panel, so it isn't drawn. Measures it either way. */
    public static boolean begin(ScaledResolution resolution, int width) {
        contentWidth = width;
        return HudRegistry.beginRender(PANEL, resolution);
    }

    public static void end() {
        HudRegistry.endRender();
    }

    /** The sample the HUD position screen shows, where the panel would be. */
    private static void drawSample(int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getMinecraft();
        contentWidth = HudSample.width(minecraft.fontRenderer);
        HudOverlayLayout.OverlayRect rect = HudOverlayLayout.getDrawnHudRect(screenWidth, screenHeight, contentWidth);
        GL11.glPushMatrix();
        GL11.glTranslatef(rect.left, rect.top, 0.0F);
        GL11.glScalef(Config.hudIndicatorSize, Config.hudIndicatorSize, 1.0F);
        HudSample.draw(minecraft, minecraft.fontRenderer, new Gui());
        GL11.glPopMatrix();
    }

    private static HudBounds bounds(HudOverlayLayout.OverlayRect rect) {
        return new HudBounds(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }
}
