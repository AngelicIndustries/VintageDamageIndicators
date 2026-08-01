package org.fentanylsolutions.vintagedamageindicators.gui;

import java.util.Arrays;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;

import org.fentanylsolutions.vintagedamageindicators.Config;
import org.fentanylsolutions.vintagedamageindicators.VintageDamageIndicators;
import org.fentanylsolutions.vintagedamageindicators.client.HudOverlayLayout;
import org.fentanylsolutions.vintagedamageindicators.client.HudPreviewMath;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class HudPositionScreen extends GuiScreen {

    private static final int DONE_BUTTON_ID = 1;
    private static final int CANCEL_BUTTON_ID = 2;
    private static final int TOGGLE_BOSS_BAR_BUTTON_ID = 3;
    private static final int BOTTOM_INFO_ROW_Y_OFFSET = 74;
    private static final int SIZE_FIELD_X = 38;
    private static final int SIZE_FIELD_WIDTH = 70;
    private static final int SIZE_FIELD_HEIGHT = 14;
    private static final int DRAG_HINT_X = 118;
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
    private static final float MIN_SCALE = 0.1F;
    private static final float MAX_SCALE = 10.0F;
    private static final int OUTLINE_COLOR = 0xB0FFFFFF;
    private static final String SAMPLE_BOSS_NAME = "Wither";
    private static final float SAMPLE_BOSS_HEALTH = 0.68F;
    private static final String[] SAMPLE_PLAYER_NAMES = { "Jack", "Alice", "BuilderBob", "Mossy", "SnowFox",
        "EnderPear", "CopperCat", "BrickTop", "GlowInk", "Silverfish" };
    private static final int[] SAMPLE_PLAYER_PINGS = { 42, 78, 123, 165, 245, 312, 480, 610, 920, -1 };
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
    private GuiTextField sizeField;
    private GuiButton toggleBossBarButton;
    private float previewScale;
    private boolean previewDisableBossBar;
    private boolean sizeInputValid = true;
    private int hudX;
    private int hudY;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudPositionScreen(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.previewDisableBossBar = Config.disableBossBar;
        this.toggleBossBarButton = new GuiButton(
            TOGGLE_BOSS_BAR_BUTTON_ID,
            this.width / 2 - 102,
            this.height - 52,
            204,
            20,
            "");
        this.buttonList.add(new GuiButton(DONE_BUTTON_ID, this.width / 2 - 102, this.height - 28, 100, 20, "Done"));
        this.buttonList.add(new GuiButton(CANCEL_BUTTON_ID, this.width / 2 + 2, this.height - 28, 100, 20, "Cancel"));
        this.buttonList.add(this.toggleBossBarButton);
        updateBossBarButtonText();
        this.previewScale = clampScale(Config.hudIndicatorSize);
        this.sizeField = new GuiTextField(
            this.fontRendererObj,
            SIZE_FIELD_X,
            getBottomInfoRowY(),
            SIZE_FIELD_WIDTH,
            SIZE_FIELD_HEIGHT);
        this.sizeField.setMaxStringLength(10);
        this.sizeField.setText(formatScale(this.previewScale));
        this.sizeField.setFocused(false);
        applyStoredHudPosition();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.sizeField != null) {
            this.sizeField.updateCursorCounter();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == TOGGLE_BOSS_BAR_BUTTON_ID) {
            this.previewDisableBossBar = !this.previewDisableBossBar;
            updateBossBarButtonText();
            return;
        }
        if (button.id == DONE_BUTTON_ID) {
            saveLayoutToConfig();
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }
        if (button.id == CANCEL_BUTTON_ID) {
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawOverlayPreview();
        drawHudPreview();
        drawDragOutline();
        int bottomInfoRowY = getBottomInfoRowY();
        drawString(this.fontRendererObj, "Size", 8, bottomInfoRowY + 3, 0xFFFFFF);
        if (this.sizeField != null) {
            this.sizeField.drawTextBox();
        }
        drawString(this.fontRendererObj, "Drag preview to position", DRAG_HINT_X, bottomInfoRowY + 3, 0xA0A0A0);
        if (!this.sizeInputValid) {
            drawString(this.fontRendererObj, "Invalid size", 8, bottomInfoRowY - 12, 0xFF5555);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (this.sizeField != null && this.sizeField.textboxKeyTyped(typedChar, keyCode)) {
            parseScaleFromInput();
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (this.sizeField != null) {
            this.sizeField.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0 && !isMouseOverButton(mouseX, mouseY)
            && !isOverSizeField(mouseX, mouseY)
            && isInsideHud(mouseX, mouseY)) {
            this.dragging = true;
            this.dragOffsetX = mouseX - this.hudX;
            this.dragOffsetY = mouseY - this.hudY;
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        if (state == 0) {
            this.dragging = false;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, button, timeSinceLastClick);
        if (!this.dragging || button != 0) {
            return;
        }
        this.hudX = mouseX - this.dragOffsetX;
        this.hudY = mouseY - this.dragOffsetY;
        clampHudToScreen();
    }

    private void drawHudPreview() {
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer fontRenderer = this.fontRendererObj;
        if (fontRenderer == null) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(this.hudX, this.hudY, 0.0F);
        GL11.glScalef(this.previewScale, this.previewScale, 1.0F);
        drawBackground(minecraft);
        drawFrame(minecraft);
        drawHealthBar(minecraft);
        drawName(fontRenderer);
        drawHealthText(fontRenderer);
        drawPotionStrip(minecraft, fontRenderer);
        GL11.glPopMatrix();
    }

    private void drawOverlayPreview() {
        if (this.fontRendererObj == null) {
            return;
        }

        HudOverlayLayout.OverlayRect hudRect = new HudOverlayLayout.OverlayRect(
            this.hudX,
            this.hudY,
            getScaledHudWidth(),
            getScaledHudHeight());
        HudOverlayLayout.BossBarLayout bossBarLayout = HudOverlayLayout.resolveBossBarLayout(
            this.width,
            this.height,
            hudRect,
            !this.previewDisableBossBar,
            this.fontRendererObj.getStringWidth(SAMPLE_BOSS_NAME));
        HudOverlayLayout.PlayerListLayout playerListLayout = HudOverlayLayout.resolvePlayerListLayout(
            this.width,
            this.height,
            hudRect,
            bossBarLayout,
            this.fontRendererObj,
            Arrays.asList(SAMPLE_PLAYER_NAMES),
            SAMPLE_PLAYER_NAMES.length,
            SAMPLE_PLAYER_NAMES.length);

        if (bossBarLayout.visible) {
            drawBossBarPreview(bossBarLayout);
        }
        drawPlayerListPreview(playerListLayout);
    }

    private void drawBossBarPreview(HudOverlayLayout.BossBarLayout layout) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager()
            .bindTexture(Gui.icons);
        int fillWidth = Math.round(SAMPLE_BOSS_HEALTH * (HudOverlayLayout.BOSS_BAR_WIDTH + 1));
        GL11.glEnable(GL11.GL_BLEND);
        drawTexturedModalRect(layout.left, layout.getBarY(), 0, 74, HudOverlayLayout.BOSS_BAR_WIDTH, 5);
        drawTexturedModalRect(layout.left, layout.getBarY(), 0, 74, HudOverlayLayout.BOSS_BAR_WIDTH, 5);
        if (fillWidth > 0) {
            drawTexturedModalRect(layout.left, layout.getBarY(), 0, 79, fillWidth, 5);
        }
        this.fontRendererObj.drawStringWithShadow(
            SAMPLE_BOSS_NAME,
            layout.getNameX(this.fontRendererObj.getStringWidth(SAMPLE_BOSS_NAME)),
            layout.getNameY(),
            0xFFFFFF);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawPlayerListPreview(HudOverlayLayout.PlayerListLayout layout) {
        int panelWidth = layout.columnWidth * layout.columns;
        drawRect(
            layout.left - 1,
            layout.top - 1,
            layout.left + panelWidth,
            layout.top + 9 * layout.rows,
            Integer.MIN_VALUE);

        for (int i = 0; i < layout.playerCount && i < SAMPLE_PLAYER_NAMES.length; i++) {
            int xPos = layout.left + i % layout.columns * layout.columnWidth;
            int yPos = layout.top + i / layout.columns * 9;
            drawRect(xPos, yPos, xPos + layout.columnWidth - 1, yPos + 8, 553648127);
            this.fontRendererObj.drawStringWithShadow(SAMPLE_PLAYER_NAMES[i], xPos, yPos, 0xFFFFFF);

            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(Gui.icons);
            int pingIndex = getPingIconIndex(SAMPLE_PLAYER_PINGS[i % SAMPLE_PLAYER_PINGS.length]);
            this.zLevel += 100.0F;
            drawTexturedModalRect(xPos + layout.columnWidth - 12, yPos, 0, 176 + pingIndex * 8, 10, 8);
            this.zLevel -= 100.0F;
        }
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

    private void drawFrame(Minecraft minecraft) {
        minecraft.getTextureManager()
            .bindTexture(HUD_FRAME_TEXTURE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawTexturedModalRect(0, 0, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawHealthBar(Minecraft minecraft) {
        int healthBarV = Config.colorblindHealthBar ? 36 : 0;
        int currentWidth = Math.round(HEALTH_BAR_WIDTH * 0.9F);
        minecraft.getTextureManager()
            .bindTexture(HEALTH_TEXTURE);
        drawTexturedModalRect(
            HEALTH_BAR_X,
            HEALTH_BAR_Y,
            0,
            healthBarV + HEALTH_BAR_HEIGHT,
            HEALTH_BAR_WIDTH,
            HEALTH_BAR_HEIGHT);
        drawTexturedModalRect(HEALTH_BAR_X, HEALTH_BAR_Y, 0, healthBarV, currentWidth, HEALTH_BAR_HEIGHT);
    }

    private void drawName(FontRenderer fontRenderer) {
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

    private void drawHealthText(FontRenderer fontRenderer) {
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

    private void drawPotionStrip(Minecraft minecraft, FontRenderer fontRenderer) {
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
            drawTexturedModalRect(
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

    private void drawStandaloneTexture(int x, int y, int width, int height) {
        Gui.func_146110_a(x, y, 0.0F, 0.0F, width, height, (float) width, (float) height);
    }

    private Potion getSamplePotion() {
        if (SAMPLE_POTION_ID < 0 || SAMPLE_POTION_ID >= Potion.potionTypes.length) {
            return null;
        }
        return Potion.potionTypes[SAMPLE_POTION_ID];
    }

    private int getPotionContentWidth(FontRenderer fontRenderer) {
        String timeText = StringUtils.ticksToElapsedTime(SAMPLE_POTION_DURATION);
        Potion potion = getSamplePotion();
        int width = fontRenderer.getStringWidth(timeText);
        if (potion != null && potion.hasStatusIcon()) {
            width += POTION_ICON_SIZE + POTION_TIME_GAP;
        }
        return Math.max(MIN_POTION_CONTENT_WIDTH, width + POTION_CONTENT_PADDING_LEFT + POTION_CONTENT_PADDING_RIGHT);
    }

    private int getHudWidth() {
        int stripWidth = POTION_LEFT_WIDTH + getPotionContentWidth(this.fontRendererObj) + POTION_RIGHT_WIDTH;
        return Math.max(PANEL_WIDTH, POTION_PANEL_X + stripWidth);
    }

    private int getHudHeight() {
        return Math.max(PANEL_HEIGHT, POTION_PANEL_Y + POTION_PANEL_HEIGHT);
    }

    private int getScaledHudWidth() {
        return Math.max(1, Math.round(getHudWidth() * this.previewScale));
    }

    private int getScaledHudHeight() {
        return Math.max(1, Math.round(getHudHeight() * this.previewScale));
    }

    private void drawDragOutline() {
        int left = this.hudX;
        int top = this.hudY;
        int right = left + getScaledHudWidth();
        int bottom = top + getScaledHudHeight();
        drawRect(left, top, right, top + 1, OUTLINE_COLOR);
        drawRect(left, bottom - 1, right, bottom, OUTLINE_COLOR);
        drawRect(left, top, left + 1, bottom, OUTLINE_COLOR);
        drawRect(right - 1, top, right, bottom, OUTLINE_COLOR);
    }

    private boolean isInsideHud(int mouseX, int mouseY) {
        return mouseX >= this.hudX && mouseX <= this.hudX + getScaledHudWidth()
            && mouseY >= this.hudY
            && mouseY <= this.hudY + getScaledHudHeight();
    }

    private boolean isMouseOverButton(int mouseX, int mouseY) {
        for (Object object : this.buttonList) {
            if (!(object instanceof GuiButton)) {
                continue;
            }
            GuiButton button = (GuiButton) object;
            if (!button.visible) {
                continue;
            }
            if (mouseX >= button.xPosition && mouseX < button.xPosition + button.width
                && mouseY >= button.yPosition
                && mouseY < button.yPosition + button.height) {
                return true;
            }
        }
        return false;
    }

    private boolean isOverSizeField(int mouseX, int mouseY) {
        return this.sizeField != null && mouseX >= this.sizeField.xPosition
            && mouseX < this.sizeField.xPosition + this.sizeField.width
            && mouseY >= this.sizeField.yPosition
            && mouseY < this.sizeField.yPosition + this.sizeField.height;
    }

    private void applyStoredHudPosition() {
        int width = getScaledHudWidth();
        int height = getScaledHudHeight();
        this.hudX = Config.hudIndicatorAlignLeft ? Config.hudIndicatorPositionX
            : this.width - width - Config.hudIndicatorPositionX;
        this.hudY = Config.hudIndicatorAlignTop ? Config.hudIndicatorPositionY
            : this.height - height - Config.hudIndicatorPositionY;
        clampHudToScreen();
    }

    private void clampHudToScreen() {
        int maxX = Math.max(0, this.width - getScaledHudWidth());
        int maxY = Math.max(0, this.height - getScaledHudHeight());
        this.hudX = Math.max(0, Math.min(this.hudX, maxX));
        this.hudY = Math.max(0, Math.min(this.hudY, maxY));
    }

    private void parseScaleFromInput() {
        String text = this.sizeField.getText()
            .trim();
        if (text.isEmpty() || "-".equals(text) || ".".equals(text) || "-.".equals(text)) {
            this.sizeInputValid = false;
            return;
        }
        try {
            float parsed = Float.parseFloat(text);
            if (Float.isNaN(parsed) || Float.isInfinite(parsed)) {
                this.sizeInputValid = false;
                return;
            }
            this.previewScale = clampScale(parsed);
            this.sizeInputValid = true;
            clampHudToScreen();
        } catch (NumberFormatException ignored) {
            this.sizeInputValid = false;
        }
    }

    private void saveLayoutToConfig() {
        int hudWidth = getScaledHudWidth();
        int hudHeight = getScaledHudHeight();
        boolean alignLeft = this.hudX <= (this.width - hudWidth) / 2;
        boolean alignTop = this.hudY <= (this.height - hudHeight) / 2;
        int offsetX = alignLeft ? this.hudX : this.width - hudWidth - this.hudX;
        int offsetY = alignTop ? this.hudY : this.height - hudHeight - this.hudY;
        Config.setHudLayout(this.previewScale, alignLeft, alignTop, Math.max(0, offsetX), Math.max(0, offsetY));
        Config.setDisableBossBar(this.previewDisableBossBar);
        Config.save();
    }

    private void updateBossBarButtonText() {
        if (this.toggleBossBarButton != null) {
            this.toggleBossBarButton.displayString = this.previewDisableBossBar ? "Vanilla Boss Bar: Disabled"
                : "Vanilla Boss Bar: Enabled";
        }
    }

    private int getBottomInfoRowY() {
        return this.height - BOTTOM_INFO_ROW_Y_OFFSET;
    }

    private int getPingIconIndex(int ping) {
        if (ping < 0) {
            return 5;
        }
        if (ping < 150) {
            return 0;
        }
        if (ping < 300) {
            return 1;
        }
        if (ping < 600) {
            return 2;
        }
        if (ping < 1000) {
            return 3;
        }
        return 4;
    }

    private float clampScale(float scale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    private String formatScale(float scale) {
        String text = String.format(Locale.ROOT, "%.2f", scale);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
