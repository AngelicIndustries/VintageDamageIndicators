package org.fentanylsolutions.vintagedamageindicators.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;

import org.fentanylsolutions.vintagedamageindicators.Config;
import org.fentanylsolutions.vintagedamageindicators.MobTypes;
import org.fentanylsolutions.vintagedamageindicators.VintageDamageIndicators;
import org.fentanylsolutions.vintagedamageindicators.client.HudEntityRenderer;
import org.fentanylsolutions.vintagedamageindicators.client.HudPreviewMath;
import org.fentanylsolutions.vintagedamageindicators.client.HudPreviewWorld;
import org.fentanylsolutions.vintagedamageindicators.client.PotionIconRenderer;
import org.fentanylsolutions.vintagedamageindicators.client.PreviewEntityFactory;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class LogoPreviewScreen extends GuiScreen {

    private static final int PREVIEW_BACKGROUND_COLOR = 0xFF252A34;
    private static final float PREVIEW_SCALE = 1.8F;
    private static final String PREVIEW_ENTITY_ID = "Creeper";
    private static final String PREVIEW_NAME_OVERRIDE = "Vintage Damage Indicators";
    private static final float PREVIEW_HEALTH_CURRENT = 18.0F;
    private static final float PREVIEW_HEALTH_MAX = 20.0F;
    private static final String PREVIEW_HEALTH_TEXT_OVERRIDE = "Wait, didn't we already have these?";
    private static final boolean PREVIEW_SHOW_POTION_TIME = true;
    private static final String[] PREVIEW_POTION_LIST = { "28,2400", "29,1200" };

    private static final int PANEL_WIDTH = HudPreviewMath.PANEL_WIDTH;
    private static final int PANEL_HEIGHT = HudPreviewMath.PANEL_HEIGHT;
    private static final int HEALTH_BAR_X = HudPreviewMath.HEALTH_BAR_X;
    private static final int HEALTH_BAR_Y = HudPreviewMath.HEALTH_BAR_Y;
    private static final int HEALTH_BAR_WIDTH = HudPreviewMath.HEALTH_BAR_WIDTH;
    private static final int HEALTH_BAR_HEIGHT = HudPreviewMath.HEALTH_BAR_HEIGHT;
    private static final int POTION_PANEL_X = HEALTH_BAR_X - 4;
    private static final int POTION_PANEL_Y = HEALTH_BAR_Y + HEALTH_BAR_HEIGHT + 3;
    private static final int POTION_PANEL_HEIGHT = 26;
    private static final int POTION_LEFT_WIDTH = 7;
    private static final int POTION_RIGHT_WIDTH = 6;
    private static final int POTION_CONTENT_PADDING_LEFT = 3;
    private static final int POTION_CONTENT_PADDING_RIGHT = 4;
    private static final int POTION_ICON_SIZE = 18;
    private static final int POTION_ICON_Y = 4;
    private static final int POTION_ENTRY_GAP = 4;
    private static final int POTION_TIME_GAP = 2;
    private static final int POTION_TIME_Y = 9;
    private static final int POTION_TIME_COLOR = 0xFFFFFF;
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

    private final GuiScreen parentScreen;
    private EntityLivingBase previewEntity;
    private boolean previewEntityCreateAttempted;
    private List<PreviewPotionEntry> previewPotions = Collections.emptyList();

    public LogoPreviewScreen(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.previewEntity = null;
        this.previewEntityCreateAttempted = false;
        this.previewPotions = buildPotionEntries(this.fontRendererObj);
    }

    @Override
    public void updateScreen() {
        if (this.previewEntity != null) {
            this.previewEntity.ticksExisted++;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, PREVIEW_BACKGROUND_COLOR);

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityLivingBase entity = getOrCreatePreviewEntity(minecraft);
        if (entity == null) {
            drawCenteredString(
                this.fontRendererObj,
                "Failed to create entity: " + PREVIEW_ENTITY_ID,
                this.width / 2,
                this.height / 2,
                0xFFFFFF);
            return;
        }

        int potionStripWidth = getPotionStripWidth();
        int hudWidth = Math.max(PANEL_WIDTH, POTION_PANEL_X + potionStripWidth);
        int x = (this.width - Math.round(hudWidth * PREVIEW_SCALE)) / 2;
        int y = (this.height - Math.round(PANEL_HEIGHT * PREVIEW_SCALE)) / 2;
        MobTypes mobType = MobTypes.getTypeFor(entity);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(PREVIEW_SCALE, PREVIEW_SCALE, 1.0F);
        drawHudPreview(minecraft, this.fontRendererObj, entity, mobType);
        GL11.glPopMatrix();

        drawCenteredString(this.fontRendererObj, "ESC to return", this.width / 2, this.height - 14, 0xE0E0E0);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private EntityLivingBase getOrCreatePreviewEntity(Minecraft minecraft) {
        if (this.previewEntity != null) {
            syncEntityPosition(this.previewEntity, minecraft);
            return this.previewEntity;
        }
        if (this.previewEntityCreateAttempted) {
            return null;
        }
        this.previewEntityCreateAttempted = true;

        World world = minecraft.theWorld;
        if (world == null && minecraft.thePlayer != null) {
            world = minecraft.thePlayer.worldObj;
        }
        if (world == null) {
            world = HudPreviewWorld.INSTANCE;
        }

        EntityLivingBase created = PreviewEntityFactory.create(PREVIEW_ENTITY_ID, world, "logo preview screen");
        if (created == null) {
            return null;
        }

        this.previewEntity = created;
        syncEntityPosition(this.previewEntity, minecraft);
        return this.previewEntity;
    }

    private void syncEntityPosition(EntityLivingBase entity, Minecraft minecraft) {
        if (minecraft.renderViewEntity != null && minecraft.renderViewEntity.worldObj == entity.worldObj) {
            entity.setPosition(
                minecraft.renderViewEntity.posX,
                minecraft.renderViewEntity.posY,
                minecraft.renderViewEntity.posZ);
            return;
        }
        if (minecraft.thePlayer != null && minecraft.thePlayer.worldObj == entity.worldObj) {
            entity.setPosition(minecraft.thePlayer.posX, minecraft.thePlayer.posY, minecraft.thePlayer.posZ);
            return;
        }
        entity.setPosition(0.0D, 0.0D, 0.0D);
    }

    private void drawHudPreview(Minecraft minecraft, FontRenderer fontRenderer, EntityLivingBase entity,
        MobTypes mobType) {
        drawBackground(minecraft);
        drawEntityPreview(entity);
        drawFrame(minecraft);
        drawMobTypeIcon(minecraft, mobType);
        drawHealthBar(minecraft);
        drawName(fontRenderer);
        drawHealthText(fontRenderer);
        drawPotionStrip(minecraft, fontRenderer);
    }

    private void drawBackground(Minecraft minecraft) {
        minecraft.getTextureManager()
            .bindTexture(HUD_BACKGROUND_TEXTURE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, Config.hudIndicatorBackgroundOpacity);
        drawTexturedModalRect(0, 0, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawEntityPreview(EntityLivingBase entity) {
        HudPreviewMath.PreviewSettings preview = HudPreviewMath.resolvePreviewSettings(entity, null);
        HudEntityRenderer
            .drawEntity(preview.x, preview.y, preview.scale, preview.yaw, preview.pitch, preview.roll, entity);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private void drawFrame(Minecraft minecraft) {
        minecraft.getTextureManager()
            .bindTexture(HUD_FRAME_TEXTURE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawTexturedModalRect(0, 0, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawMobTypeIcon(Minecraft minecraft, MobTypes mobType) {
        minecraft.getTextureManager()
            .bindTexture(mobType.getTexture());
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Gui.func_146110_a(
            HudPreviewMath.ICON_X,
            HudPreviewMath.ICON_Y,
            0.0F,
            0.0F,
            HudPreviewMath.ICON_SIZE,
            HudPreviewMath.ICON_SIZE,
            (float) HudPreviewMath.ICON_SIZE,
            (float) HudPreviewMath.ICON_SIZE);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawHealthBar(Minecraft minecraft) {
        float maxHealth = Math.max(PREVIEW_HEALTH_MAX, 0.01F);
        float health = Math.max(0.0F, Math.min(PREVIEW_HEALTH_CURRENT, maxHealth));
        float ratio = health / maxHealth;
        int currentWidth = Math.round(HEALTH_BAR_WIDTH * ratio);
        int healthBarV = Config.colorblindHealthBar ? 36 : 0;

        minecraft.getTextureManager()
            .bindTexture(HEALTH_TEXTURE);
        drawTexturedModalRect(
            HEALTH_BAR_X,
            HEALTH_BAR_Y,
            0,
            healthBarV + HEALTH_BAR_HEIGHT,
            HEALTH_BAR_WIDTH,
            HEALTH_BAR_HEIGHT);
        if (currentWidth > 0) {
            drawTexturedModalRect(HEALTH_BAR_X, HEALTH_BAR_Y, 0, healthBarV, currentWidth, HEALTH_BAR_HEIGHT);
        }
    }

    private void drawName(FontRenderer fontRenderer) {
        int width = fontRenderer.getStringWidth(PREVIEW_NAME_OVERRIDE);
        float textScale = Math.min(1.0F, HudPreviewMath.NAME_MAX_WIDTH / (float) Math.max(width, 1));
        GL11.glPushMatrix();
        GL11.glTranslatef(HudPreviewMath.NAME_CENTER_X, HudPreviewMath.NAME_Y, 0.0F);
        GL11.glScalef(textScale, textScale, 1.0F);
        int drawX = -Math.round(width / 2.0F);
        fontRenderer.drawString(PREVIEW_NAME_OVERRIDE, drawX, 0, 0xFFFFFF);
        GL11.glPopMatrix();
    }

    private void drawHealthText(FontRenderer fontRenderer) {
        int width = fontRenderer.getStringWidth(PREVIEW_HEALTH_TEXT_OVERRIDE);
        float textScale = Math.min(1.0F, HudPreviewMath.HEALTH_TEXT_MAX_WIDTH / (float) Math.max(width, 1));
        GL11.glPushMatrix();
        GL11.glTranslatef(HudPreviewMath.HEALTH_TEXT_CENTER_X, HudPreviewMath.HEALTH_TEXT_Y, 0.0F);
        GL11.glScalef(textScale, textScale, 1.0F);
        int drawX = -Math.round(width / 2.0F);
        fontRenderer.drawString(PREVIEW_HEALTH_TEXT_OVERRIDE, drawX, 0, 0xFFFFFF);
        GL11.glPopMatrix();
    }

    private void drawPotionStrip(Minecraft minecraft, FontRenderer fontRenderer) {
        if (this.previewPotions.isEmpty()) {
            return;
        }

        int middleWidth = getPotionContentWidth();
        int rightX = POTION_PANEL_X + POTION_LEFT_WIDTH + middleWidth;

        minecraft.getTextureManager()
            .bindTexture(POTION_LEFT_TEXTURE);
        drawStandaloneTexture(POTION_PANEL_X, POTION_PANEL_Y, POTION_LEFT_WIDTH, POTION_PANEL_HEIGHT);

        if (middleWidth > 0) {
            minecraft.getTextureManager()
                .bindTexture(POTION_MIDDLE_TEXTURE);
            for (int offset = 0; offset < middleWidth; offset++) {
                drawStandaloneTexture(
                    POTION_PANEL_X + POTION_LEFT_WIDTH + offset,
                    POTION_PANEL_Y,
                    1,
                    POTION_PANEL_HEIGHT);
            }
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

        if (middleWidth > 0) {
            minecraft.getTextureManager()
                .bindTexture(POTION_MIDDLE_OVERLAY_TEXTURE);
            for (int offset = 0; offset < middleWidth; offset++) {
                drawStandaloneTexture(
                    POTION_PANEL_X + POTION_LEFT_WIDTH + offset,
                    POTION_PANEL_Y,
                    1,
                    POTION_PANEL_HEIGHT);
            }
        }

        minecraft.getTextureManager()
            .bindTexture(POTION_RIGHT_OVERLAY_TEXTURE);
        drawStandaloneTexture(rightX, POTION_PANEL_Y, POTION_RIGHT_WIDTH, POTION_PANEL_HEIGHT);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);

        int drawX = POTION_PANEL_X + POTION_LEFT_WIDTH + POTION_CONTENT_PADDING_LEFT;
        for (int index = 0; index < this.previewPotions.size(); index++) {
            PreviewPotionEntry entry = this.previewPotions.get(index);
            int textX = drawX;

            if (entry.hasIcon && entry.potion != null) {
                int iconY = POTION_PANEL_Y + POTION_ICON_Y;
                if (entry.potion.hasStatusIcon()) {
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    minecraft.getTextureManager()
                        .bindTexture(INVENTORY_TEXTURE);
                    int iconIndex = entry.potion.getStatusIconIndex();
                    drawTexturedModalRect(
                        drawX,
                        iconY,
                        iconIndex % 8 * 18,
                        198 + iconIndex / 8 * 18,
                        POTION_ICON_SIZE,
                        POTION_ICON_SIZE);
                }
                PotionIconRenderer.drawCustomIcon(entry.potion, entry.effect, minecraft, drawX, iconY);
                textX += POTION_ICON_SIZE + POTION_TIME_GAP;
            }

            if (!entry.timeText.isEmpty()) {
                fontRenderer.drawString(entry.timeText, textX, POTION_PANEL_Y + POTION_TIME_Y, POTION_TIME_COLOR);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            drawX += entry.width;
            if (index + 1 < this.previewPotions.size()) {
                drawX += POTION_ENTRY_GAP;
            }
        }
    }

    private int getPotionContentWidth() {
        int contentWidth = POTION_CONTENT_PADDING_LEFT + POTION_CONTENT_PADDING_RIGHT;
        for (int index = 0; index < this.previewPotions.size(); index++) {
            contentWidth += this.previewPotions.get(index).width;
            if (index + 1 < this.previewPotions.size()) {
                contentWidth += POTION_ENTRY_GAP;
            }
        }
        return contentWidth;
    }

    private int getPotionStripWidth() {
        if (this.previewPotions.isEmpty()) {
            return 0;
        }
        return POTION_LEFT_WIDTH + getPotionContentWidth() + POTION_RIGHT_WIDTH;
    }

    private void drawStandaloneTexture(int x, int y, int width, int height) {
        Gui.func_146110_a(x, y, 0.0F, 0.0F, width, height, (float) width, (float) height);
    }

    private List<PreviewPotionEntry> buildPotionEntries(FontRenderer fontRenderer) {
        if (fontRenderer == null) {
            return Collections.emptyList();
        }

        List<PreviewPotionEntry> entries = new ArrayList<>();
        for (String spec : PREVIEW_POTION_LIST) {
            if (spec == null) {
                continue;
            }

            String[] parts = spec.trim()
                .split(",");
            if (parts.length < 2) {
                continue;
            }

            int potionId = parseInt(parts[0], -1);
            int durationTicks = parseInt(parts[1], 0);
            if (potionId < 0 || potionId >= Potion.potionTypes.length) {
                continue;
            }

            Potion potion = Potion.potionTypes[potionId];
            if (potion == null) {
                continue;
            }

            String timeText = PREVIEW_SHOW_POTION_TIME ? StringUtils.ticksToElapsedTime(Math.max(0, durationTicks))
                : "";
            boolean hasIcon = PotionIconRenderer.hasIcon(potion);
            int width = hasIcon ? POTION_ICON_SIZE : 0;
            if (PREVIEW_SHOW_POTION_TIME) {
                if (hasIcon) {
                    width += POTION_TIME_GAP;
                }
                width += fontRenderer.getStringWidth(timeText);
            }
            if (width <= 0) {
                width = POTION_ICON_SIZE;
            }
            entries.add(
                new PreviewPotionEntry(
                    potion,
                    new PotionEffect(potion.id, Math.max(0, durationTicks), 0),
                    hasIcon,
                    timeText,
                    width,
                    potion.id));
        }

        entries.sort(Comparator.comparingInt(entry -> entry.sortOrder));
        return entries;
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static final class PreviewPotionEntry {

        private final Potion potion;
        private final PotionEffect effect;
        private final boolean hasIcon;
        private final String timeText;
        private final int width;
        private final int sortOrder;

        private PreviewPotionEntry(Potion potion, PotionEffect effect, boolean hasIcon, String timeText, int width,
            int sortOrder) {
            this.potion = potion;
            this.effect = effect;
            this.hasIcon = hasIcon;
            this.timeText = timeText;
            this.width = width;
            this.sortOrder = sortOrder;
        }
    }
}
