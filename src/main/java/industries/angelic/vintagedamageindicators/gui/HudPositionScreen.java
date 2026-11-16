package industries.angelic.vintagedamageindicators.gui;

import java.util.Arrays;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.client.HudOverlayLayout;
import industries.angelic.vintagedamageindicators.client.HudSample;

public class HudPositionScreen extends GuiScreen {

    private static final int DONE_BUTTON_ID = 1;
    private static final int CANCEL_BUTTON_ID = 2;
    private static final int TOGGLE_BOSS_BAR_BUTTON_ID = 3;
    private static final int BOTTOM_INFO_ROW_Y_OFFSET = 74;
    private static final int SIZE_FIELD_X = 38;
    private static final int SIZE_FIELD_WIDTH = 70;
    private static final int SIZE_FIELD_HEIGHT = 14;
    private static final int DRAG_HINT_X = 118;
    private static final float MIN_SCALE = 0.1F;
    private static final float MAX_SCALE = 10.0F;
    private static final int OUTLINE_COLOR = 0xB0FFFFFF;
    private static final String SAMPLE_BOSS_NAME = "Wither";
    private static final float SAMPLE_BOSS_HEALTH = 0.68F;
    private static final String[] SAMPLE_PLAYER_NAMES = { "Jack", "Alice", "BuilderBob", "Mossy", "SnowFox",
        "EnderPear", "CopperCat", "BrickTop", "GlowInk", "Silverfish" };
    private static final int[] SAMPLE_PLAYER_PINGS = { 42, 78, 123, 165, 245, 312, 480, 610, 920, -1 };

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
        HudSample.draw(minecraft, fontRenderer, this);
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

    private int getHudWidth() {
        return HudSample.width(this.fontRendererObj);
    }

    private int getHudHeight() {
        return HudSample.height();
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
