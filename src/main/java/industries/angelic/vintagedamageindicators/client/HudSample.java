package industries.angelic.vintagedamageindicators.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;

import org.lwjgl.opengl.GL11;

import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.VintageDamageIndicators;

/**
 * A sample HUD indicator, with no mob: frame, health bar, name, health and one potion effect. Drawn by the HUD position
 * screen and as the preview in Dashboard's HUD editor (DashboardHud).
 */
public final class HudSample {

    private static final int PANEL_WIDTH = HudPreviewMath.PANEL_WIDTH;
    private static final int PANEL_HEIGHT = HudPreviewMath.PANEL_HEIGHT;
    private static final int HEALTH_BAR_X = HudPreviewMath.HEALTH_BAR_X;
    private static final int HEALTH_BAR_Y = HudPreviewMath.HEALTH_BAR_Y;
    private static final int HEALTH_BAR_WIDTH = HudPreviewMath.HEALTH_BAR_WIDTH;
    private static final int HEALTH_BAR_HEIGHT = HudPreviewMath.HEALTH_BAR_HEIGHT;
    private static final int POTION_PANEL_X = HEALTH_BAR_X - 4;
    private static final int POTION_PANEL_Y = HEALTH_BAR_Y + HEALTH_BAR_HEIGHT + 4;
    private static final int POTION_PANEL_HEIGHT = 26;
    private static final int POTION_LEFT_WIDTH = 7;
    private static final int POTION_RIGHT_WIDTH = 6;
    private static final int POTION_CONTENT_PADDING_LEFT = 3;
    private static final int POTION_CONTENT_PADDING_RIGHT = 4;
    private static final int POTION_ICON_SIZE = 18;
    private static final int POTION_ICON_Y = 4;
    private static final int POTION_TIME_GAP = 2;
    private static final int POTION_TIME_Y = 9;
    private static final int POTION_TIME_COLOR = 0xFFFFFF;
    private static final int SAMPLE_POTION_ID = 1;
    private static final int SAMPLE_POTION_DURATION = 2400;
    private static final int MIN_POTION_CONTENT_WIDTH = 86;
    private static final ResourceLocation HUD_BACKGROUND_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/damage_indicator_background.png");
    private static final ResourceLocation HUD_FRAME_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/damage_indicator.png");
    private static final ResourceLocation HEALTH_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/damage_indicator_health.png");
    private static final ResourceLocation POTION_LEFT_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/potion_left.png");
    private static final ResourceLocation POTION_LEFT_OVERLAY_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/potion_left_overlay.png");
    private static final ResourceLocation POTION_MIDDLE_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/potion_middle.png");
    private static final ResourceLocation POTION_MIDDLE_OVERLAY_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/potion_middle_overlay.png");
    private static final ResourceLocation POTION_RIGHT_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/potion_right.png");
    private static final ResourceLocation POTION_RIGHT_OVERLAY_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/potion_right_overlay.png");
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation(
        "textures/gui/container/inventory.png");

    private HudSample() {}

    /** Draws the sample at 0, 0 at its own size; gui draws the textures. */
    public static void draw(Minecraft minecraft, FontRenderer fontRenderer, Gui gui) {
        drawBackground(minecraft, gui);
        drawFrame(minecraft, gui);
        drawHealthBar(minecraft, gui);
        drawName(fontRenderer);
        drawHealthText(fontRenderer);
        drawPotionStrip(minecraft, fontRenderer, gui);
    }

    /** The sample's width, the potion strip included. */
    public static int width(FontRenderer fontRenderer) {
        int stripWidth = POTION_LEFT_WIDTH + getPotionContentWidth(fontRenderer) + POTION_RIGHT_WIDTH;
        return Math.max(PANEL_WIDTH, POTION_PANEL_X + stripWidth);
    }

    public static int height() {
        return Math.max(PANEL_HEIGHT, POTION_PANEL_Y + POTION_PANEL_HEIGHT);
    }

    private static void drawBackground(Minecraft minecraft, Gui gui) {
        minecraft.getTextureManager()
            .bindTexture(HUD_BACKGROUND_TEXTURE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, Config.hudIndicatorBackgroundOpacity);
        gui.drawTexturedModalRect(0, 0, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static void drawFrame(Minecraft minecraft, Gui gui) {
        minecraft.getTextureManager()
            .bindTexture(HUD_FRAME_TEXTURE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        gui.drawTexturedModalRect(0, 0, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static void drawHealthBar(Minecraft minecraft, Gui gui) {
        int healthBarV = Config.colorblindHealthBar ? 36 : 0;
        int currentWidth = Math.round(HEALTH_BAR_WIDTH * 0.9F);
        minecraft.getTextureManager()
            .bindTexture(HEALTH_TEXTURE);
        gui.drawTexturedModalRect(
            HEALTH_BAR_X,
            HEALTH_BAR_Y,
            0,
            healthBarV + HEALTH_BAR_HEIGHT,
            HEALTH_BAR_WIDTH,
            HEALTH_BAR_HEIGHT);
        gui.drawTexturedModalRect(HEALTH_BAR_X, HEALTH_BAR_Y, 0, healthBarV, currentWidth, HEALTH_BAR_HEIGHT);
    }

    private static void drawName(FontRenderer fontRenderer) {
        String text = "Target";
        int width = fontRenderer.getStringWidth(text);
        float textScale = Math.min(1.0F, HudPreviewMath.NAME_MAX_WIDTH / (float) Math.max(width, 1));
        GL11.glPushMatrix();
        GL11.glTranslatef(HudPreviewMath.NAME_CENTER_X, HudPreviewMath.NAME_Y, 0.0F);
        GL11.glScalef(textScale, textScale, 1.0F);
        int drawX = -Math.round(width / 2.0F);
        if (Config.hudNameTextOutline) {
            fontRenderer.drawStringWithShadow(text, drawX, 0, 0xFFFFFF);
        } else {
            fontRenderer.drawString(text, drawX, 0, 0xFFFFFF);
        }
        GL11.glPopMatrix();
    }

    private static void drawHealthText(FontRenderer fontRenderer) {
        String separator = Config.healthSeparator ? " | " : "/";
        String text = Config.healthDecimals ? "18.0" + separator + "20.0" : "18" + separator + "20";
        int width = fontRenderer.getStringWidth(text);
        float textScale = Math.min(1.0F, HudPreviewMath.HEALTH_TEXT_MAX_WIDTH / (float) Math.max(width, 1));
        GL11.glPushMatrix();
        GL11.glTranslatef(HudPreviewMath.HEALTH_TEXT_CENTER_X, HudPreviewMath.HEALTH_TEXT_Y, 0.0F);
        GL11.glScalef(textScale, textScale, 1.0F);
        int drawX = -Math.round(width / 2.0F);
        if (Config.hudHealthTextOutline) {
            fontRenderer.drawStringWithShadow(text, drawX, 0, 0xFFFFFF);
        } else {
            fontRenderer.drawString(text, drawX, 0, 0xFFFFFF);
        }
        GL11.glPopMatrix();
    }

    private static void drawPotionStrip(Minecraft minecraft, FontRenderer fontRenderer, Gui gui) {
        int middleWidth = getPotionContentWidth(fontRenderer);
        int rightX = POTION_PANEL_X + POTION_LEFT_WIDTH + middleWidth;
        minecraft.getTextureManager()
            .bindTexture(POTION_LEFT_TEXTURE);
        drawStandaloneTexture(POTION_PANEL_X, POTION_PANEL_Y, POTION_LEFT_WIDTH, POTION_PANEL_HEIGHT);
        minecraft.getTextureManager()
            .bindTexture(POTION_MIDDLE_TEXTURE);
        for (int offset = 0; offset < middleWidth; offset++) {
            drawStandaloneTexture(POTION_PANEL_X + POTION_LEFT_WIDTH + offset, POTION_PANEL_Y, 1, POTION_PANEL_HEIGHT);
        }
        minecraft.getTextureManager()
            .bindTexture(POTION_RIGHT_TEXTURE);
        drawStandaloneTexture(rightX, POTION_PANEL_Y, POTION_RIGHT_WIDTH, POTION_PANEL_HEIGHT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, Config.hudIndicatorBackgroundOpacity);
        minecraft.getTextureManager()
            .bindTexture(POTION_LEFT_OVERLAY_TEXTURE);
        drawStandaloneTexture(POTION_PANEL_X, POTION_PANEL_Y, POTION_LEFT_WIDTH, POTION_PANEL_HEIGHT);
        minecraft.getTextureManager()
            .bindTexture(POTION_MIDDLE_OVERLAY_TEXTURE);
        for (int offset = 0; offset < middleWidth; offset++) {
            drawStandaloneTexture(POTION_PANEL_X + POTION_LEFT_WIDTH + offset, POTION_PANEL_Y, 1, POTION_PANEL_HEIGHT);
        }
        minecraft.getTextureManager()
            .bindTexture(POTION_RIGHT_OVERLAY_TEXTURE);
        drawStandaloneTexture(rightX, POTION_PANEL_Y, POTION_RIGHT_WIDTH, POTION_PANEL_HEIGHT);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        int drawX = POTION_PANEL_X + POTION_LEFT_WIDTH + POTION_CONTENT_PADDING_LEFT;
        String timeText = StringUtils.ticksToElapsedTime(SAMPLE_POTION_DURATION);
        Potion potion = getSamplePotion();
        if (potion != null && potion.hasStatusIcon()) {
            minecraft.getTextureManager()
                .bindTexture(INVENTORY_TEXTURE);
            int iconIndex = potion.getStatusIconIndex();
            gui.drawTexturedModalRect(
                drawX,
                POTION_PANEL_Y + POTION_ICON_Y,
                iconIndex % 8 * 18,
                198 + iconIndex / 8 * 18,
                POTION_ICON_SIZE,
                POTION_ICON_SIZE);
            drawX += POTION_ICON_SIZE + POTION_TIME_GAP;
        }
        fontRenderer.drawString(timeText, drawX, POTION_PANEL_Y + POTION_TIME_Y, POTION_TIME_COLOR);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawStandaloneTexture(int x, int y, int width, int height) {
        Gui.func_146110_a(x, y, 0.0F, 0.0F, width, height, (float) width, (float) height);
    }

    private static Potion getSamplePotion() {
        if (SAMPLE_POTION_ID < 0 || SAMPLE_POTION_ID >= Potion.potionTypes.length) {
            return null;
        }
        return Potion.potionTypes[SAMPLE_POTION_ID];
    }

    private static int getPotionContentWidth(FontRenderer fontRenderer) {
        String timeText = StringUtils.ticksToElapsedTime(SAMPLE_POTION_DURATION);
        Potion potion = getSamplePotion();
        int width = fontRenderer.getStringWidth(timeText);
        if (potion != null && potion.hasStatusIcon()) {
            width += POTION_ICON_SIZE + POTION_TIME_GAP;
        }
        return Math.max(MIN_POTION_CONTENT_WIDTH, width + POTION_CONTENT_PADDING_LEFT + POTION_CONTENT_PADDING_RIGHT);
    }
}
