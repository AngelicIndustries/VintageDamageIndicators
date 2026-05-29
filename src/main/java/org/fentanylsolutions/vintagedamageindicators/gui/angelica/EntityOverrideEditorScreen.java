package org.fentanylsolutions.vintagedamageindicators.gui.angelica;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.fentanylsolutions.fentlib.gui.sodiumgui.SodiumGui;
import org.fentanylsolutions.fentlib.gui.sodiumgui.SodiumGuiButton;
import org.fentanylsolutions.fentlib.gui.sodiumgui.SodiumGuiScrollState;
import org.fentanylsolutions.vintagedamageindicators.Config;
import org.fentanylsolutions.vintagedamageindicators.MobTypes;
import org.fentanylsolutions.vintagedamageindicators.VintageDamageIndicators;
import org.fentanylsolutions.vintagedamageindicators.client.HudEntityRenderer;
import org.fentanylsolutions.vintagedamageindicators.client.HudPreviewMath;
import org.fentanylsolutions.vintagedamageindicators.client.HudPreviewWorld;
import org.fentanylsolutions.vintagedamageindicators.client.PreviewEntityFactory;
import org.fentanylsolutions.vintagedamageindicators.gui.GuiFactory;
import org.fentanylsolutions.vintagedamageindicators.varinstances.VarInstanceCommon;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class EntityOverrideEditorScreen extends GuiScreen {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.###");
    private static final int PAGE_BUTTON_Y = 6;
    private static final int CONTENT_TOP = 30;
    private static final int BOTTOM_BAR_HEIGHT = 32;
    private static final int OUTER_PADDING = 6;
    private static final int COLUMN_GAP = 8;
    private static final int SECTION_GAP = 8;
    private static final int SEARCH_HEIGHT = 18;
    private static final int SEARCH_TEXT_Y_OFFSET = 6;
    private static final int MIN_LEFT_WIDTH = 126;
    private static final int MAX_LEFT_WIDTH = 170;
    private static final int MIN_RIGHT_WIDTH = 220;
    private static final int CONTROL_WIDTH = 110;
    private static final int CHECKBOX_SIZE = 10;
    private static final int TEXT_PADDING = 6;
    private static final int TOOLTIP_WIDTH = 214;
    private static final int PREVIEW_BACKGROUND_RGB = 0x8A929C;
    private static final int PREVIEW_BACKGROUND_ALPHA = 0xE0;
    private static final int PREVIEW_TITLE_COLOR = 0xFFFFFFFF;
    private static final int PREVIEW_DETAIL_COLOR = 0xFFB8B8B8;
    private static final int NAME_UNDERLAY_X = HudPreviewMath.HEALTH_BAR_X + 1;
    private static final int NAME_UNDERLAY_Y = 4;
    private static final int NAME_UNDERLAY_WIDTH = HudPreviewMath.HEALTH_BAR_WIDTH - 2;
    private static final int NAME_UNDERLAY_HEIGHT = 16;

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/damage_indicator_background.png");
    private static final ResourceLocation FRAME_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/damage_indicator.png");
    private static final ResourceLocation HEALTH_TEXTURE = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/damage_indicator_health.png");

    private final GuiScreen parentScreen;
    private final EditorState state;
    private final List<OptionRow> optionRows = new ArrayList<>();
    private final SodiumGuiButton pageButton;
    private final SodiumGuiButton undoButton;
    private final SodiumGuiButton cancelButton;
    private final SodiumGuiButton saveButton;
    private final SodiumGuiScrollState mobScroll = new SodiumGuiScrollState();
    private final SodiumGuiScrollState optionScroll = new SodiumGuiScrollState();

    private GuiTextField searchField;
    private SodiumGuiScrollState draggingScrollState;
    private int lastMouseX;
    private int lastMouseY;
    private int leftX;
    private int leftY;
    private int leftWidth;
    private int leftListY;
    private int leftListHeight;
    private int rightX;
    private int rightY;
    private int rightWidth;
    private int optionListY;
    private int optionListHeight;
    private String activeTooltip;

    public EntityOverrideEditorScreen(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.state = new EditorState(parentScreen instanceof GuiFactory.ConfigGui);
        this.pageButton = new SodiumGuiButton("Entity Overrides", null);
        this.undoButton = new SodiumGuiButton("Undo", this::undoChanges);
        this.cancelButton = new SodiumGuiButton("Cancel", this::discardAndClose);
        this.saveButton = new SodiumGuiButton("Save", this::saveAndClose);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.searchField = new GuiTextField(this.fontRendererObj, 0, 0, 0, SEARCH_HEIGHT);
        this.searchField.setMaxStringLength(64);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setTextColor(0xFFFFFFFF);
        this.searchField.setText(this.state.search);
        this.searchField.setCanLoseFocus(true);
        this.pageButton.setSelected(true);
        this.rebuildOptionRows();
        this.updateLayout();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        this.searchField.updateCursorCounter();
        for (OptionRow row : this.optionRows) {
            row.update();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.activeTooltip = null;
        this.updateLayout();
        this.drawDefaultBackground();
        this.drawTopBar(mouseX, mouseY, partialTicks);
        this.drawMobList(mouseX, mouseY);
        this.drawPreviewPane(mouseX, mouseY, partialTicks);
        this.drawOptionRows(mouseX, mouseY);
        this.drawBottomBar(mouseX, mouseY, partialTicks);
        this.drawTooltip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        this.clearTextFocus();
        boolean handled = false;

        if (this.pageButton.mouseClicked(mouseX, mouseY, mouseButton)
            || this.undoButton.mouseClicked(mouseX, mouseY, mouseButton)
            || this.cancelButton.mouseClicked(mouseX, mouseY, mouseButton)
            || this.saveButton.mouseClicked(mouseX, mouseY, mouseButton)) {
            handled = true;
        }

        if (!handled && SodiumGui.isInside(mouseX, mouseY, this.leftX, this.leftY, this.leftWidth, SEARCH_HEIGHT)) {
            int fieldMouseX = SodiumGui
                .clamp(mouseX, this.searchField.xPosition, this.searchField.xPosition + this.searchField.width - 1);
            int fieldMouseY = SodiumGui
                .clamp(mouseY, this.searchField.yPosition, this.searchField.yPosition + this.searchField.height - 1);
            this.searchField.mouseClicked(fieldMouseX, fieldMouseY, mouseButton);
            handled = true;
        }

        if (!handled
            && SodiumGui.isInside(mouseX, mouseY, this.leftX, this.leftListY, this.leftWidth, this.leftListHeight)) {
            handled = this.clickMobList(mouseX, mouseY, mouseButton);
        }

        if (!handled && SodiumGui
            .isInside(mouseX, mouseY, this.rightX, this.optionListY, this.rightWidth, this.optionListHeight)) {
            handled = this.clickOptionRows(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (this.isAnyTextFocused()) {
                this.clearTextFocus();
                return;
            }
            this.discardAndClose();
            return;
        }

        if (this.searchField.isFocused()) {
            if (this.searchField.textboxKeyTyped(typedChar, keyCode)) {
                this.state.search = this.searchField.getText();
                this.mobScroll.setScroll(
                    0.0F,
                    SodiumGui.getContentHeight(
                        this.state.getFilteredOptions()
                            .size()),
                    this.leftListHeight);
                return;
            }
        }

        for (OptionRow row : this.optionRows) {
            if (row.keyTyped(typedChar, keyCode)) {
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        if (SodiumGui.isInside(
            this.lastMouseX,
            this.lastMouseY,
            this.leftX,
            this.leftListY,
            this.leftWidth,
            this.leftListHeight)) {
            this.mobScroll.scrollWheel(
                wheel,
                SodiumGui.getContentHeight(
                    this.state.getFilteredOptions()
                        .size()),
                this.leftListHeight);
            return;
        }
        if (SodiumGui.isInside(
            this.lastMouseX,
            this.lastMouseY,
            this.rightX,
            this.optionListY,
            this.rightWidth,
            this.optionListHeight)) {
            this.optionScroll
                .scrollWheel(wheel, SodiumGui.getContentHeight(this.optionRows.size()), this.optionListHeight);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        if (mouseButton == 0 && this.draggingScrollState != null) {
            this.dragScrollbar(mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            if (this.draggingScrollState != null) {
                this.draggingScrollState.endDrag();
                this.draggingScrollState = null;
            }
        }
        super.mouseMovedOrUp(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private void updateLayout() {
        int contentWidth = Math.max(0, this.width - OUTER_PADDING * 2);
        this.leftWidth = Math.min(MAX_LEFT_WIDTH, Math.max(MIN_LEFT_WIDTH, contentWidth / 3));
        this.rightWidth = contentWidth - this.leftWidth - COLUMN_GAP;
        if (this.rightWidth < MIN_RIGHT_WIDTH) {
            this.leftWidth = Math.max(MIN_LEFT_WIDTH, contentWidth - MIN_RIGHT_WIDTH - COLUMN_GAP);
            this.rightWidth = contentWidth - this.leftWidth - COLUMN_GAP;
        }
        this.leftX = OUTER_PADDING;
        this.leftY = CONTENT_TOP;
        this.rightX = this.leftX + this.leftWidth + COLUMN_GAP;
        this.rightY = CONTENT_TOP;
        this.leftListY = this.leftY + SEARCH_HEIGHT + SECTION_GAP;
        this.leftListHeight = Math.max(SodiumGui.ROW_HEIGHT, this.height - this.leftListY - BOTTOM_BAR_HEIGHT);
        this.optionListY = this.rightY + HudPreviewMath.PANEL_HEIGHT + SECTION_GAP + 26;
        this.optionListHeight = Math.max(SodiumGui.ROW_HEIGHT, this.height - this.optionListY - BOTTOM_BAR_HEIGHT);

        this.searchField.xPosition = this.leftX + 6;
        this.searchField.yPosition = this.leftY + SEARCH_TEXT_Y_OFFSET;
        this.searchField.width = Math.max(10, this.leftWidth - 12);
        this.searchField.height = this.fontRendererObj.FONT_HEIGHT;

        int pageWidth = 16 + this.fontRendererObj.getStringWidth(this.pageButton.getLabel());
        this.pageButton.setBounds(OUTER_PADDING, PAGE_BUTTON_Y, pageWidth, 18);

        this.saveButton.setBounds(this.width - 73, this.height - 26, 65, 20);
        this.cancelButton.setBounds(this.width - 142, this.height - 26, 65, 20);
        this.undoButton.setBounds(this.width - 211, this.height - 26, 65, 20);

        boolean dirty = this.state.isDirty();
        this.undoButton.setVisible(true);
        this.undoButton.setEnabled(dirty);
        this.cancelButton.setEnabled(true);
        this.saveButton.setEnabled(dirty);
        this.saveButton.setSelected(true);
        this.mobScroll.clampTo(
            SodiumGui.getContentHeight(
                this.state.getFilteredOptions()
                    .size()),
            this.leftListHeight);
        this.optionScroll.clampTo(SodiumGui.getContentHeight(this.optionRows.size()), this.optionListHeight);
    }

    private void drawTopBar(int mouseX, int mouseY, float partialTicks) {
        this.pageButton.render(this.fontRendererObj, mouseX, mouseY);
        int titleX = this.pageButton.getX() + this.pageButton.getWidth() + COLUMN_GAP;
        int titleWidth = Math.max(0, this.width - titleX - OUTER_PADDING);
        String selected = SodiumGui.ellipsize(this.fontRendererObj, this.state.getWindowTitle(), titleWidth);
        this.fontRendererObj.drawString(
            selected,
            titleX + Math.max(0, (titleWidth - this.fontRendererObj.getStringWidth(selected)) / 2),
            11,
            0xFFFFFFFF);
    }

    private void drawMobList(int mouseX, int mouseY) {
        SodiumGui.drawFlatBox(this.leftX, this.leftY, this.leftWidth, SEARCH_HEIGHT, SodiumGui.ROW_IDLE_COLOR);
        if (this.searchField.getText()
            .isEmpty() && !this.searchField.isFocused()) {
            this.fontRendererObj
                .drawString("Search", this.searchField.xPosition, this.searchField.yPosition, 0xFF808080);
        }
        this.searchField.drawTextBox();

        List<EntityOption> options = this.state.getFilteredOptions();
        if (options.isEmpty()) {
            this.fontRendererObj
                .drawString("No mobs found.", this.leftX + TEXT_PADDING, this.leftListY + 5, 0xFFAAAAAA);
            return;
        }

        int visibleRows = SodiumGui.getVisibleRows(this.leftListHeight);
        int first = this.mobScroll.getFirstRow();
        int end = Math.min(options.size(), first + visibleRows + 2);
        float maxScroll = getMaxMobScroll();
        int rowWidth = this.leftWidth - (maxScroll > 0 ? SodiumGui.SCROLLBAR_AREA_WIDTH : 0);
        int y = this.mobScroll.getFirstRowY(this.leftListY);
        SodiumGui.beginScissor(this.mc, this.leftX, this.leftListY, this.leftWidth, this.leftListHeight);
        for (int i = first; i < end; i++) {
            EntityOption option = options.get(i);
            boolean hovered = SodiumGui
                .isInside(mouseX, mouseY, this.leftX, this.leftListY, rowWidth, this.leftListHeight)
                && SodiumGui.isInside(mouseX, mouseY, this.leftX, y, rowWidth, SodiumGui.ROW_HEIGHT);
            boolean selected = this.state.isSelected(option);
            int background = selected ? SodiumGui.ROW_SELECTED_COLOR
                : hovered ? SodiumGui.ROW_HOVER_COLOR : SodiumGui.ROW_IDLE_COLOR;
            SodiumGui.drawFlatBox(this.leftX, y, rowWidth, SodiumGui.ROW_HEIGHT, background);
            if (selected) {
                drawRect(this.leftX, y, this.leftX + 2, y + SodiumGui.ROW_HEIGHT, SodiumGui.getDefaultAccentColor());
            }
            String label = this.state.getDisplayName(option);
            label = SodiumGui.ellipsize(this.fontRendererObj, label, rowWidth - 12);
            this.fontRendererObj
                .drawString(label, this.leftX + TEXT_PADDING, y + 5, selected ? 0xFFFFFFFF : 0xFFE0E0E0);
            if (hovered) {
                this.activeTooltip = option.className;
            }
            y += SodiumGui.ROW_STEP;
        }
        SodiumGui.drawScrollbar(
            this.leftX + this.leftWidth - SodiumGui.SCROLLBAR_WIDTH,
            this.leftListY,
            SodiumGui.SCROLLBAR_WIDTH,
            this.leftListHeight,
            SodiumGui.getContentHeight(options.size()),
            this.mobScroll.getScroll());
        SodiumGui.endScissor();
    }

    private boolean clickMobList(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }
        List<EntityOption> options = this.state.getFilteredOptions();
        if (getMaxMobScroll() > 0 && mouseX >= this.leftX + this.leftWidth - SodiumGui.SCROLLBAR_AREA_WIDTH) {
            this.draggingScrollState = this.mobScroll;
            this.mobScroll
                .beginDrag(this.leftListY, this.leftListHeight, SodiumGui.getContentHeight(options.size()), mouseY);
            return true;
        }
        int index = this.mobScroll.getRowAt(mouseY, this.leftListY, options.size());
        if (index < 0) {
            return false;
        }
        this.state.select(options.get(index));
        this.rebuildOptionRows();
        this.optionScroll.setScroll(0.0F, SodiumGui.getContentHeight(this.optionRows.size()), this.optionListHeight);
        SodiumGuiButton.playClickSound();
        return true;
    }

    private void drawPreviewPane(int mouseX, int mouseY, float partialTicks) {
        int previewX = this.rightX + Math.max(0, (this.rightWidth - HudPreviewMath.PANEL_WIDTH) / 2);
        int previewY = this.rightY;
        drawHudPreview(previewX, previewY);

        int rowY = previewY + HudPreviewMath.PANEL_HEIGHT + SECTION_GAP;
        String registryName = this.state.getSelectedOption().registryName;
        String className = this.state.getSelectedOption().className;
        SodiumGui.drawFlatBox(this.rightX, rowY, this.rightWidth, SodiumGui.ROW_HEIGHT, SodiumGui.ROW_IDLE_COLOR);
        this.fontRendererObj.drawString(
            SodiumGui.ellipsize(this.fontRendererObj, registryName, Math.max(20, this.rightWidth / 2 - 10)),
            this.rightX + TEXT_PADDING,
            rowY + 5,
            PREVIEW_TITLE_COLOR);
        String detail = SodiumGui.ellipsize(this.fontRendererObj, className, Math.max(20, this.rightWidth / 2 - 10));
        this.fontRendererObj.drawString(
            detail,
            this.rightX + this.rightWidth - this.fontRendererObj.getStringWidth(detail) - TEXT_PADDING,
            rowY + 5,
            PREVIEW_DETAIL_COLOR);
        if (SodiumGui.isInside(mouseX, mouseY, this.rightX, rowY, this.rightWidth, SodiumGui.ROW_HEIGHT)) {
            this.activeTooltip = className;
        }
    }

    private void drawOptionRows(int mouseX, int mouseY) {
        if (this.state.options.isEmpty()) {
            this.fontRendererObj
                .drawString("No editable mobs found.", this.rightX + TEXT_PADDING, this.optionListY + 5, 0xFFAAAAAA);
            return;
        }

        int visibleRows = SodiumGui.getVisibleRows(this.optionListHeight);
        int first = this.optionScroll.getFirstRow();
        int end = Math.min(this.optionRows.size(), first + visibleRows + 2);
        float maxScroll = getMaxOptionScroll();
        int rowWidth = this.rightWidth - (maxScroll > 0 ? SodiumGui.SCROLLBAR_AREA_WIDTH : 0);
        int y = this.optionScroll.getFirstRowY(this.optionListY);
        SodiumGui.beginScissor(this.mc, this.rightX, this.optionListY, this.rightWidth, this.optionListHeight);
        for (int i = first; i < end; i++) {
            OptionRow row = this.optionRows.get(i);
            boolean mouseInViewport = SodiumGui
                .isInside(mouseX, mouseY, this.rightX, this.optionListY, rowWidth, this.optionListHeight);
            row.render(
                this,
                this.rightX,
                y,
                rowWidth,
                SodiumGui.ROW_HEIGHT,
                mouseInViewport ? mouseX : Integer.MIN_VALUE,
                mouseInViewport ? mouseY : Integer.MIN_VALUE);
            if (mouseInViewport && row.isHovered(mouseX, mouseY) && row.tooltip != null && !row.tooltip.isEmpty()) {
                this.activeTooltip = row.tooltip;
            }
            y += SodiumGui.ROW_STEP;
        }
        SodiumGui.drawScrollbar(
            this.rightX + this.rightWidth - SodiumGui.SCROLLBAR_WIDTH,
            this.optionListY,
            SodiumGui.SCROLLBAR_WIDTH,
            this.optionListHeight,
            SodiumGui.getContentHeight(this.optionRows.size()),
            this.optionScroll.getScroll());
        SodiumGui.endScissor();
    }

    private boolean clickOptionRows(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && getMaxOptionScroll() > 0
            && mouseX >= this.rightX + this.rightWidth - SodiumGui.SCROLLBAR_AREA_WIDTH) {
            this.draggingScrollState = this.optionScroll;
            this.optionScroll.beginDrag(
                this.optionListY,
                this.optionListHeight,
                SodiumGui.getContentHeight(this.optionRows.size()),
                mouseY);
            return true;
        }

        int rowIndex = this.optionScroll.getRowAt(mouseY, this.optionListY, this.optionRows.size());
        if (rowIndex < 0) {
            return false;
        }
        return this.optionRows.get(rowIndex)
            .mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void dragScrollbar(int mouseY) {
        if (this.draggingScrollState == this.mobScroll) {
            this.mobScroll.dragTo(
                this.leftListY,
                this.leftListHeight,
                SodiumGui.getContentHeight(
                    this.state.getFilteredOptions()
                        .size()),
                mouseY);
            return;
        }
        if (this.draggingScrollState == this.optionScroll) {
            this.optionScroll.dragTo(
                this.optionListY,
                this.optionListHeight,
                SodiumGui.getContentHeight(this.optionRows.size()),
                mouseY);
        }
    }

    private void drawBottomBar(int mouseX, int mouseY, float partialTicks) {
        this.undoButton.render(this.fontRendererObj, mouseX, mouseY);
        this.cancelButton.render(this.fontRendererObj, mouseX, mouseY);
        this.saveButton.render(this.fontRendererObj, mouseX, mouseY);
    }

    private void drawHudPreview(int x, int y) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityLivingBase entity = this.state.getPreviewEntity();
        VarInstanceCommon.EntityOverride override = this.state.getPreviewOverride();
        MobTypes mobType = this.state.getPreviewMobType(entity);

        drawPreviewBackground(minecraft);
        if (entity != null) {
            HudPreviewMath.PreviewSettings preview = HudPreviewMath
                .resolvePreviewSettings(entity, override, this.state.previewYaw, this.state.previewPitch);
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
        drawPreviewFrame(minecraft);
        drawMobTypeIcon(minecraft, mobType);
        drawHealthBar(minecraft, entity);
        drawPreviewName(minecraft.fontRenderer, this.state.getPreviewName(entity));
        drawHealthText(minecraft.fontRenderer, entity);

        GL11.glPopMatrix();
    }

    private void drawPreviewBackground(Minecraft minecraft) {
        minecraft.getTextureManager()
            .bindTexture(BACKGROUND_TEXTURE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(
            ((PREVIEW_BACKGROUND_RGB >> 16) & 255) / 255.0F,
            ((PREVIEW_BACKGROUND_RGB >> 8) & 255) / 255.0F,
            (PREVIEW_BACKGROUND_RGB & 255) / 255.0F,
            PREVIEW_BACKGROUND_ALPHA / 255.0F);
        drawTexturedModalRect(0, 0, 0, 0, HudPreviewMath.PANEL_WIDTH, HudPreviewMath.PANEL_HEIGHT);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawNameUnderlay() {
        int alpha = Math.max(0, Math.min(255, Math.round(Config.hudIndicatorBackgroundOpacity * 255.0F)));
        int color = (alpha << 24) | PREVIEW_BACKGROUND_RGB;
        drawRect(
            NAME_UNDERLAY_X,
            NAME_UNDERLAY_Y,
            NAME_UNDERLAY_X + NAME_UNDERLAY_WIDTH,
            NAME_UNDERLAY_Y + NAME_UNDERLAY_HEIGHT,
            color);
    }

    private void drawPreviewFrame(Minecraft minecraft) {
        minecraft.getTextureManager()
            .bindTexture(FRAME_TEXTURE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawTexturedModalRect(0, 0, 0, 0, HudPreviewMath.PANEL_WIDTH, HudPreviewMath.PANEL_HEIGHT);
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

    private void drawHealthBar(Minecraft minecraft, EntityLivingBase entity) {
        float health = entity == null ? 0.0F : Math.max(0.0F, Math.min(entity.getHealth(), entity.getMaxHealth()));
        float maxHealth = entity == null ? 0.0F : Math.max(entity.getMaxHealth(), 0.0F);
        float ratio = maxHealth <= 0.0F ? 0.0F : health / maxHealth;
        int currentWidth = Math.round(HudPreviewMath.HEALTH_BAR_WIDTH * ratio);
        int healthBarV = Config.colorblindHealthBar ? 36 : 0;

        minecraft.getTextureManager()
            .bindTexture(HEALTH_TEXTURE);
        drawTexturedModalRect(
            HudPreviewMath.HEALTH_BAR_X,
            HudPreviewMath.HEALTH_BAR_Y,
            0,
            healthBarV + HudPreviewMath.HEALTH_BAR_HEIGHT,
            HudPreviewMath.HEALTH_BAR_WIDTH,
            HudPreviewMath.HEALTH_BAR_HEIGHT);
        if (currentWidth > 0) {
            drawTexturedModalRect(
                HudPreviewMath.HEALTH_BAR_X,
                HudPreviewMath.HEALTH_BAR_Y,
                0,
                healthBarV,
                currentWidth,
                HudPreviewMath.HEALTH_BAR_HEIGHT);
        }
    }

    private void drawPreviewName(FontRenderer fontRenderer, String name) {
        if (name == null) {
            name = "";
        }
        if (!Config.hudIndicatorEnabled) {
            drawNameUnderlay();
        }
        int width = fontRenderer.getStringWidth(name);
        float textScale = Math.min(1.0F, HudPreviewMath.NAME_MAX_WIDTH / (float) Math.max(width, 1));
        GL11.glPushMatrix();
        GL11.glTranslatef(HudPreviewMath.NAME_CENTER_X, HudPreviewMath.NAME_Y, 0.0F);
        GL11.glScalef(textScale, textScale, 1.0F);
        int scaledWidth = Math.round(width * textScale);
        int drawX = -Math.round(scaledWidth / (2.0F * textScale));
        if (Config.hudNameTextOutline) {
            fontRenderer.drawStringWithShadow(name, drawX, 0, 0xFFFFFF);
        } else {
            fontRenderer.drawString(name, drawX, 0, 0xFFFFFF);
        }
        GL11.glPopMatrix();
    }

    private void drawHealthText(FontRenderer fontRenderer, EntityLivingBase entity) {
        String healthText = "--";
        if (entity != null) {
            float health = Math.max(0.0F, entity.getHealth());
            float maxHealth = Math.max(0.0F, entity.getMaxHealth());
            String separator = Config.healthSeparator ? " | " : "/";
            healthText = formatHealth(health) + separator + formatHealth(maxHealth);
            if (!Config.healthDecimals) {
                healthText = (int) health + separator + (int) maxHealth;
            }
        }

        int width = fontRenderer.getStringWidth(healthText);
        float textScale = Math.min(1.0F, HudPreviewMath.HEALTH_TEXT_MAX_WIDTH / (float) Math.max(width, 1));
        GL11.glPushMatrix();
        GL11.glTranslatef(HudPreviewMath.HEALTH_TEXT_CENTER_X, HudPreviewMath.HEALTH_TEXT_Y, 0.0F);
        GL11.glScalef(textScale, textScale, 1.0F);
        int drawX = -Math.round(width / 2.0F);
        if (Config.hudHealthTextOutline) {
            fontRenderer.drawStringWithShadow(healthText, drawX, 0, 0xFFFFFF);
        } else {
            fontRenderer.drawString(healthText, drawX, 0, 0xFFFFFF);
        }
        GL11.glPopMatrix();
    }

    private void drawTooltip(int mouseX, int mouseY) {
        if (this.activeTooltip == null || this.activeTooltip.isEmpty()) {
            return;
        }
        List<String> lines = this.fontRendererObj.listFormattedStringToWidth(this.activeTooltip, TOOLTIP_WIDTH - 8);
        int tooltipHeight = lines.size() * 12 + 6;
        int x = mouseX + 10;
        int y = mouseY + 10;
        if (x + TOOLTIP_WIDTH > this.width) {
            x = mouseX - TOOLTIP_WIDTH - 10;
        }
        if (y + tooltipHeight > this.height - BOTTOM_BAR_HEIGHT) {
            y = this.height - BOTTOM_BAR_HEIGHT - tooltipHeight;
        }
        drawGradientRect(x, y, x + TOOLTIP_WIDTH, y + tooltipHeight, 0xF0000000, 0xF0000000);
        for (int i = 0; i < lines.size(); i++) {
            this.fontRendererObj.drawString(lines.get(i), x + 4, y + 4 + i * 12, 0xFFFFFFFF);
        }
    }

    private void rebuildOptionRows() {
        this.optionRows.clear();
        this.optionRows.add(
            new ToggleRow(
                "Enabled",
                "Whether this override is active for the selected mob.",
                () -> this.state.getSelectedOverrideOrDefault().enable,
                value -> this.state.getOrCreateSelectedOverride().enable = value));
        this.optionRows.add(
            new ToggleRow(
                "Popoff",
                "Whether damage popoff particles are shown for this mob.",
                () -> this.state.getSelectedOverrideOrDefault().popoffEnabled,
                value -> this.state.getOrCreateSelectedOverride().popoffEnabled = value));
        this.optionRows.add(
            new ToggleRow(
                "Append Baby Name",
                "Adds Baby before the display name when previewing a child entity.",
                () -> this.state.getSelectedOverrideOrDefault().appendBabyName,
                value -> this.state.getOrCreateSelectedOverride().appendBabyName = value));
        this.optionRows.add(
            new TextRow(
                "Name",
                "Display name used by the HUD.",
                () -> this.state.getSelectedOverrideOrDefault().displayName == null ? ""
                    : this.state.getSelectedOverrideOrDefault().displayName,
                value -> this.state.getOrCreateSelectedOverride().displayName = value,
                false));
        this.optionRows.add(
            new CycleRow(
                "Preview Age",
                "Toggles the live preview between adult and child forms.",
                () -> this.state.previewChild ? "Child" : "Adult",
                direction -> this.state.previewChild = !this.state.previewChild));
        this.optionRows.add(new MobTypeRow("Mob Type", "Icon category shown on the HUD."));
        this.optionRows.add(
            new TextRow(
                "Preview Yaw",
                "Base yaw used by the HUD preview and live HUD render.",
                this.state::previewYawText,
                value -> this.state.previewYaw = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Preview Pitch",
                "Base pitch used by the HUD preview and live HUD render.",
                this.state::previewPitchText,
                value -> this.state.previewPitch = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Adult Scale Mult",
                "Multiplier applied to the selected adult entity render scale.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().scaleFactor),
                value -> this.state.getOrCreateSelectedOverride().scaleFactor = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Adult X Offset",
                "Horizontal preview/render offset for adult entities.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().xOffset),
                value -> this.state.getOrCreateSelectedOverride().xOffset = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Adult Y Offset",
                "Vertical preview/render offset for adult entities.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().yOffset),
                value -> this.state.getOrCreateSelectedOverride().yOffset = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Baby Scale Mult",
                "Multiplier applied to child entity render scale.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().babyScaleFactor),
                value -> this.state.getOrCreateSelectedOverride().babyScaleFactor = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Baby X Offset",
                "Horizontal preview/render offset for child entities.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().babyXOffset),
                value -> this.state.getOrCreateSelectedOverride().babyXOffset = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Baby Y Offset",
                "Vertical preview/render offset for child entities.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().babyYOffset),
                value -> this.state.getOrCreateSelectedOverride().babyYOffset = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Baby Fallback Mult",
                "Fallback child scale multiplier for entities without a dedicated value.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().babyScaleModifier),
                value -> this.state.getOrCreateSelectedOverride().babyScaleModifier = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Yaw Offset",
                "Additional yaw offset for this mob.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().yawOffset),
                value -> this.state.getOrCreateSelectedOverride().yawOffset = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Pitch Offset",
                "Additional pitch offset for this mob.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().pitchOffset),
                value -> this.state.getOrCreateSelectedOverride().pitchOffset = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Roll Offset",
                "Additional roll offset for this mob.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().rollOffset),
                value -> this.state.getOrCreateSelectedOverride().rollOffset = parseFloat(value),
                true));
        this.optionRows.add(
            new TextRow(
                "Size Modifier",
                "Applies an entity-height based scale adjustment.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().sizeModifier),
                value -> this.state.getOrCreateSelectedOverride().sizeModifier = parseFloat(value),
                true));
        this.optionRows
            .add(new ButtonRow("Reset Selected Override", "Removes custom values for the selected mob.", () -> {
                this.state.resetSelectedOverride();
                this.rebuildOptionRows();
            }));
    }

    private void undoChanges() {
        this.state.resetToBaseline();
        this.rebuildOptionRows();
        this.mobScroll.clampTo(
            SodiumGui.getContentHeight(
                this.state.getFilteredOptions()
                    .size()),
            this.leftListHeight);
        this.optionScroll.clampTo(SodiumGui.getContentHeight(this.optionRows.size()), this.optionListHeight);
    }

    private void saveAndClose() {
        if (this.state.isDirty()) {
            this.state.save();
        }
        this.state.onClose();
        this.mc.displayGuiScreen(this.parentScreen);
    }

    private void discardAndClose() {
        this.state.resetToBaseline();
        this.state.onClose();
        this.mc.displayGuiScreen(this.parentScreen);
    }

    private void clearTextFocus() {
        this.searchField.setFocused(false);
        for (OptionRow row : this.optionRows) {
            row.clearFocus();
        }
    }

    private boolean isAnyTextFocused() {
        if (this.searchField.isFocused()) {
            return true;
        }
        for (OptionRow row : this.optionRows) {
            if (row.isFocused()) {
                return true;
            }
        }
        return false;
    }

    private float getMaxMobScroll() {
        return SodiumGui.getMaxScroll(
            SodiumGui.getContentHeight(
                this.state.getFilteredOptions()
                    .size()),
            this.leftListHeight);
    }

    private float getMaxOptionScroll() {
        return SodiumGui.getMaxScroll(SodiumGui.getContentHeight(this.optionRows.size()), this.optionListHeight);
    }

    private static String formatFloat(float value) {
        synchronized (DECIMAL_FORMAT) {
            return DECIMAL_FORMAT.format(value);
        }
    }

    private static float parseFloat(String value) {
        if (value == null || value.trim()
            .isEmpty()) {
            return 0.0F;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0.0F;
        }
    }

    private static String formatHealth(float value) {
        float rounded = Math.round(value * 5.0F) / 5.0F;
        String text = Float.toString(rounded);
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }

    private static String formatMobType(MobTypes type) {
        String[] parts = type.name()
            .toLowerCase(Locale.ROOT)
            .split("_");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            if (!parts[i].isEmpty()) {
                builder.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    builder.append(parts[i].substring(1));
                }
            }
        }
        return builder.toString();
    }

    private interface BooleanGetter {

        boolean get();
    }

    private interface BooleanSetter {

        void set(boolean value);
    }

    private interface TextGetter {

        String get();
    }

    private interface TextSetter {

        void set(String value);
    }

    private interface DirectionAction {

        void apply(int direction);
    }

    private abstract class OptionRow {

        private final String label;
        private final String tooltip;
        private int x;
        private int y;
        private int width;
        private int height;

        private OptionRow(String label, String tooltip) {
            this.label = label;
            this.tooltip = tooltip;
        }

        private void render(EntityOverrideEditorScreen screen, int x, int y, int width, int height, int mouseX,
            int mouseY) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            boolean hovered = this.isHovered(mouseX, mouseY);
            SodiumGui.drawFlatBox(x, y, width, height, hovered ? SodiumGui.ROW_HOVER_COLOR : SodiumGui.ROW_IDLE_COLOR);
            this.drawLabel(screen, x, y, width);
            this.renderControl(screen, x, y, width, height, mouseX, mouseY);
        }

        private void drawLabel(EntityOverrideEditorScreen screen, int x, int y, int width) {
            String text = screen.fontRendererObj
                .trimStringToWidth(this.label, Math.max(20, width - CONTROL_WIDTH - 18));
            screen.fontRendererObj.drawString(text, x + TEXT_PADDING, y + 5, 0xFFFFFFFF);
        }

        protected boolean isHovered(int mouseX, int mouseY) {
            return SodiumGui.isInside(mouseX, mouseY, this.x, this.y, this.width, this.height);
        }

        protected abstract void renderControl(EntityOverrideEditorScreen screen, int x, int y, int width, int height,
            int mouseX, int mouseY);

        protected boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            return false;
        }

        protected boolean keyTyped(char typedChar, int keyCode) {
            return false;
        }

        protected void update() {}

        protected void clearFocus() {}

        protected boolean isFocused() {
            return false;
        }
    }

    private final class ToggleRow extends OptionRow {

        private final BooleanGetter getter;
        private final BooleanSetter setter;

        private ToggleRow(String label, String tooltip, BooleanGetter getter, BooleanSetter setter) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        protected void renderControl(EntityOverrideEditorScreen screen, int x, int y, int width, int height, int mouseX,
            int mouseY) {
            int boxX = x + width - CHECKBOX_SIZE - TEXT_PADDING;
            int boxY = y + (height - CHECKBOX_SIZE) / 2;
            int color = this.getter.get() ? SodiumGui.getDefaultAccentColor() : 0xFFFFFFFF;
            if (this.getter.get()) {
                drawRect(boxX + 2, boxY + 2, boxX + CHECKBOX_SIZE - 2, boxY + CHECKBOX_SIZE - 2, color);
            }
            SodiumGui.drawRectOutline(boxX, boxY, CHECKBOX_SIZE, CHECKBOX_SIZE, color);
        }

        @Override
        protected boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0 || !isHovered(mouseX, mouseY)) {
                return false;
            }
            this.setter.set(!this.getter.get());
            SodiumGuiButton.playClickSound();
            return true;
        }
    }

    private final class CycleRow extends OptionRow {

        private final TextGetter getter;
        private final DirectionAction action;

        private CycleRow(String label, String tooltip, TextGetter getter, DirectionAction action) {
            super(label, tooltip);
            this.getter = getter;
            this.action = action;
        }

        @Override
        protected void renderControl(EntityOverrideEditorScreen screen, int x, int y, int width, int height, int mouseX,
            int mouseY) {
            String text = this.getter.get();
            text = screen.fontRendererObj.trimStringToWidth(text, CONTROL_WIDTH);
            screen.fontRendererObj.drawString(
                text,
                x + width - screen.fontRendererObj.getStringWidth(text) - TEXT_PADDING,
                y + 5,
                0xFFFFFFFF);
        }

        @Override
        protected boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (!isHovered(mouseX, mouseY) || (mouseButton != 0 && mouseButton != 1)) {
                return false;
            }
            this.action.apply(mouseButton == 0 ? 1 : -1);
            SodiumGuiButton.playClickSound();
            return true;
        }
    }

    private final class MobTypeRow extends OptionRow {

        private MobTypeRow(String label, String tooltip) {
            super(label, tooltip);
        }

        @Override
        protected void renderControl(EntityOverrideEditorScreen screen, int x, int y, int width, int height, int mouseX,
            int mouseY) {
            MobTypes type = EntityOverrideEditorScreen.this.state.getSelectedMobType();
            String text = formatMobType(type);
            text = screen.fontRendererObj.trimStringToWidth(text, CONTROL_WIDTH);
            screen.fontRendererObj.drawString(
                text,
                x + width - screen.fontRendererObj.getStringWidth(text) - TEXT_PADDING,
                y + 5,
                0xFFFFFFFF);
        }

        @Override
        protected boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (!isHovered(mouseX, mouseY) || (mouseButton != 0 && mouseButton != 1)) {
                return false;
            }
            EntityOverrideEditorScreen.this.state.cycleSelectedMobType(mouseButton == 0 ? 1 : -1);
            SodiumGuiButton.playClickSound();
            return true;
        }
    }

    private final class TextRow extends OptionRow {

        private final TextGetter getter;
        private final TextSetter setter;
        private final GuiTextField field;

        private TextRow(String label, String tooltip, TextGetter getter, TextSetter setter, boolean numeric) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
            this.field = new GuiTextField(EntityOverrideEditorScreen.this.fontRendererObj, 0, 0, CONTROL_WIDTH, 14);
            this.field.setEnableBackgroundDrawing(false);
            this.field.setCanLoseFocus(true);
            this.field.setMaxStringLength(numeric ? 16 : 64);
            this.field.setTextColor(0xFFFFFFFF);
            this.field.setText(getter.get());
        }

        @Override
        protected void renderControl(EntityOverrideEditorScreen screen, int x, int y, int width, int height, int mouseX,
            int mouseY) {
            int fieldWidth = Math.min(CONTROL_WIDTH, Math.max(50, width / 2 - 8));
            int fieldX = x + width - fieldWidth - TEXT_PADDING;
            int fieldY = y + 3;
            if (!this.field.isFocused()) {
                String current = this.getter.get();
                if (!this.field.getText()
                    .equals(current)) {
                    this.field.setText(current);
                }
            }
            this.field.xPosition = fieldX + 2;
            this.field.yPosition = fieldY + 1;
            this.field.width = fieldWidth - 4;
            this.field.height = 12;
            drawRect(fieldX, fieldY, fieldX + fieldWidth, fieldY + 14, 0x60000000);
            this.field.drawTextBox();
        }

        @Override
        protected boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            this.field.mouseClicked(mouseX, mouseY, mouseButton);
            return this.field.isFocused();
        }

        @Override
        protected boolean keyTyped(char typedChar, int keyCode) {
            if (!this.field.isFocused()) {
                return false;
            }
            if (this.field.textboxKeyTyped(typedChar, keyCode)) {
                this.setter.set(this.field.getText());
                return true;
            }
            return false;
        }

        @Override
        protected void update() {
            this.field.updateCursorCounter();
        }

        @Override
        protected void clearFocus() {
            this.field.setFocused(false);
        }

        @Override
        protected boolean isFocused() {
            return this.field.isFocused();
        }
    }

    private final class ButtonRow extends OptionRow {

        private final Runnable action;

        private ButtonRow(String label, String tooltip, Runnable action) {
            super(label, tooltip);
            this.action = action;
        }

        @Override
        protected void renderControl(EntityOverrideEditorScreen screen, int x, int y, int width, int height, int mouseX,
            int mouseY) {
            String text = "Reset";
            screen.fontRendererObj.drawString(
                text,
                x + width - screen.fontRendererObj.getStringWidth(text) - TEXT_PADDING,
                y + 5,
                0xFFFFFFFF);
        }

        @Override
        protected boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0 || !isHovered(mouseX, mouseY)) {
                return false;
            }
            this.action.run();
            SodiumGuiButton.playClickSound();
            return true;
        }
    }

    private static final class EditorState {

        private static final int PREVIEW_MAX_CUSTOM_AGE = 100;
        private static final int PREVIEW_MIN_CUSTOM_AGE = 35;

        private final List<EntityOption> options;
        private final boolean configGuiContext;
        private final Map<String, VarInstanceCommon.EntityOverride> overridesByClassName = new LinkedHashMap<>();
        private final List<String> preservedEntries = new ArrayList<>();
        private int selectedIndex;
        private boolean previewChild;
        private float previewYaw = Config.hudEntityYaw;
        private float previewPitch = Config.hudEntityPitch;
        private EntityLivingBase previewEntity;
        private String previewEntityClassName;
        private boolean previewEntityCreationAttempted;
        private String search = "";
        private String baselineSnapshot;
        private Map<String, VarInstanceCommon.EntityOverride> baselineOverridesByClassName = new LinkedHashMap<>();
        private List<String> baselinePreservedEntries = new ArrayList<>();
        private float baselinePreviewYaw;
        private float baselinePreviewPitch;

        private EditorState(boolean configGuiContext) {
            this.configGuiContext = configGuiContext;
            this.options = buildOptions();
            loadExistingOverrides();
            captureBaseline();
        }

        private List<EntityOption> buildOptions() {
            List<String> names = new ArrayList<>();
            for (Object key : EntityList.stringToClassMapping.keySet()) {
                names.add(String.valueOf(key));
            }
            Collections.sort(names, String.CASE_INSENSITIVE_ORDER);

            Map<String, EntityOption> optionsByClassName = new LinkedHashMap<>();
            for (String registryName : names) {
                Object clsObject = EntityList.stringToClassMapping.get(registryName);
                if (!(clsObject instanceof Class)) {
                    continue;
                }
                Class<?> entityClass = (Class<?>) clsObject;
                if (!EntityLivingBase.class.isAssignableFrom(entityClass)
                    || Modifier.isAbstract(entityClass.getModifiers())) {
                    continue;
                }
                String className = entityClass.getName();
                if (!optionsByClassName.containsKey(className)) {
                    optionsByClassName.put(className, new EntityOption(registryName, className));
                }
            }
            return new ArrayList<>(optionsByClassName.values());
        }

        private void loadExistingOverrides() {
            this.overridesByClassName.clear();
            this.preservedEntries.clear();

            Set<String> editableClassNames = new java.util.HashSet<>();
            for (EntityOption option : this.options) {
                editableClassNames.add(option.className);
            }

            if (this.configGuiContext) {
                GuiFactory.PendingEntityOverrideState pending = GuiFactory.copyPendingEntityOverrideState();
                if (pending != null) {
                    this.previewYaw = pending.previewYaw;
                    this.previewPitch = pending.previewPitch;
                    for (Map.Entry<String, VarInstanceCommon.EntityOverride> entry : pending.overridesByClassName
                        .entrySet()) {
                        if (editableClassNames.contains(entry.getKey())) {
                            this.overridesByClassName.put(
                                entry.getKey(),
                                entry.getValue()
                                    .copy());
                        } else {
                            addPreservedEntry(
                                entry.getValue()
                                    .serialize());
                        }
                    }
                    for (String preserved : pending.preservedEntries) {
                        addPreservedEntry(preserved);
                    }
                    return;
                }
            }

            if (VintageDamageIndicators.varInstanceCommon != null) {
                for (Map.Entry<String, VarInstanceCommon.EntityOverride> entry : VintageDamageIndicators.varInstanceCommon
                    .copyEntityOverridesByClassName()
                    .entrySet()) {
                    if (editableClassNames.contains(entry.getKey())) {
                        this.overridesByClassName.put(entry.getKey(), entry.getValue());
                    } else {
                        addPreservedEntry(
                            entry.getValue()
                                .serialize());
                    }
                }
                for (String preserved : VintageDamageIndicators.varInstanceCommon
                    .copyPreservedEntityOverrideEntries()) {
                    addPreservedEntry(preserved);
                }
                return;
            }

            for (VarInstanceCommon.EntityOverride override : Config.getEntityOverrides()) {
                if (override == null || override.className == null
                    || override.className.trim()
                        .isEmpty()) {
                    continue;
                }
                if (editableClassNames.contains(override.className)) {
                    this.overridesByClassName.put(override.className, override.copy());
                } else {
                    addPreservedEntry(
                        override.copy()
                            .serialize());
                }
            }
        }

        private static Map<String, VarInstanceCommon.EntityOverride> copyOverrides(
            Map<String, VarInstanceCommon.EntityOverride> source) {
            Map<String, VarInstanceCommon.EntityOverride> copy = new LinkedHashMap<>();
            for (Map.Entry<String, VarInstanceCommon.EntityOverride> entry : source.entrySet()) {
                if (entry.getValue() != null) {
                    copy.put(
                        entry.getKey(),
                        entry.getValue()
                            .copy());
                }
            }
            return copy;
        }

        private void captureBaseline() {
            this.baselineSnapshot = createSnapshot();
            this.baselineOverridesByClassName = copyOverrides(this.overridesByClassName);
            this.baselinePreservedEntries = new ArrayList<>(this.preservedEntries);
            this.baselinePreviewYaw = this.previewYaw;
            this.baselinePreviewPitch = this.previewPitch;
        }

        private boolean isDirty() {
            return !createSnapshot().equals(this.baselineSnapshot);
        }

        private String createSnapshot() {
            StringBuilder builder = new StringBuilder();
            builder.append(formatFloat(this.previewYaw))
                .append('|')
                .append(formatFloat(this.previewPitch))
                .append('|');
            for (Map.Entry<String, VarInstanceCommon.EntityOverride> entry : this.overridesByClassName.entrySet()) {
                if (entry.getValue() != null) {
                    builder.append(entry.getKey())
                        .append('=')
                        .append(
                            entry.getValue()
                                .serialize())
                        .append('\n');
                }
            }
            builder.append("--preserved--\n");
            for (String preserved : this.preservedEntries) {
                builder.append(preserved)
                    .append('\n');
            }
            return builder.toString();
        }

        private void save() {
            GuiFactory.saveEntityOverrideState(
                this.overridesByClassName,
                this.preservedEntries,
                this.previewYaw,
                this.previewPitch);
            captureBaseline();
        }

        private void resetToBaseline() {
            this.overridesByClassName.clear();
            this.overridesByClassName.putAll(copyOverrides(this.baselineOverridesByClassName));
            this.preservedEntries.clear();
            this.preservedEntries.addAll(this.baselinePreservedEntries);
            this.previewYaw = this.baselinePreviewYaw;
            this.previewPitch = this.baselinePreviewPitch;
            this.previewEntity = null;
            this.previewEntityClassName = null;
            this.previewEntityCreationAttempted = false;
        }

        private void onClose() {
            GuiFactory.clearPendingEntityOverrideState();
        }

        private void addPreservedEntry(String serialized) {
            if (serialized != null && !this.preservedEntries.contains(serialized)) {
                this.preservedEntries.add(serialized);
            }
        }

        private List<EntityOption> getFilteredOptions() {
            if (this.search == null || this.search.trim()
                .isEmpty()) {
                return this.options;
            }
            String needle = this.search.toLowerCase(Locale.ROOT);
            List<EntityOption> filtered = new ArrayList<>();
            for (EntityOption option : this.options) {
                if (option.registryName.toLowerCase(Locale.ROOT)
                    .contains(needle)
                    || option.className.toLowerCase(Locale.ROOT)
                        .contains(needle)
                    || getDisplayName(option).toLowerCase(Locale.ROOT)
                        .contains(needle)) {
                    filtered.add(option);
                }
            }
            return filtered;
        }

        private boolean isSelected(EntityOption option) {
            return this.options.indexOf(option) == this.selectedIndex;
        }

        private void select(EntityOption option) {
            int index = this.options.indexOf(option);
            if (index >= 0) {
                this.selectedIndex = index;
                this.previewEntity = null;
                this.previewEntityClassName = null;
                this.previewEntityCreationAttempted = false;
            }
        }

        private EntityOption getSelectedOption() {
            if (this.options.isEmpty()) {
                return new EntityOption("Unavailable", "");
            }
            this.selectedIndex = Math.max(0, Math.min(this.selectedIndex, this.options.size() - 1));
            return this.options.get(this.selectedIndex);
        }

        private boolean hasOverride(EntityOption option) {
            return this.overridesByClassName.containsKey(option.className);
        }

        private VarInstanceCommon.EntityOverride getExistingSelectedOverride() {
            return this.overridesByClassName.get(getSelectedOption().className);
        }

        private VarInstanceCommon.EntityOverride getPreviewOverride() {
            return getExistingSelectedOverride();
        }

        private VarInstanceCommon.EntityOverride getSelectedOverrideOrDefault() {
            VarInstanceCommon.EntityOverride override = getExistingSelectedOverride();
            if (override != null) {
                return override;
            }
            VarInstanceCommon.EntityOverride defaults = new VarInstanceCommon.EntityOverride();
            defaults.className = getSelectedOption().className;
            defaults.displayName = getSelectedOption().registryName;
            defaults.type = getDetectedTypeForSelectedOption();
            return defaults;
        }

        private VarInstanceCommon.EntityOverride getOrCreateSelectedOverride() {
            EntityOption option = getSelectedOption();
            VarInstanceCommon.EntityOverride override = this.overridesByClassName.get(option.className);
            if (override == null) {
                override = new VarInstanceCommon.EntityOverride();
                override.className = option.className;
                override.displayName = option.registryName;
                override.type = getDetectedTypeForSelectedOption();
                this.overridesByClassName.put(option.className, override);
            }
            return override;
        }

        private void resetSelectedOverride() {
            this.overridesByClassName.remove(getSelectedOption().className);
        }

        private String getWindowTitle() {
            EntityOption option = getSelectedOption();
            return getDisplayName(option) + " | " + option.className;
        }

        private String getDisplayName(EntityOption option) {
            return getDisplayName(option, null);
        }

        private String getDisplayName(EntityOption option, EntityLivingBase entity) {
            VarInstanceCommon.EntityOverride override = this.overridesByClassName.get(option.className);
            if (hasCustomDisplayName(override, option)) {
                return override.displayName;
            }
            String localized = getLocalizedEntityName(entity, option.registryName);
            if (!localized.isEmpty()) {
                return localized;
            }
            return option.registryName;
        }

        private boolean hasCustomDisplayName(VarInstanceCommon.EntityOverride override, EntityOption option) {
            if (override == null || override.displayName == null) {
                return false;
            }
            String trimmed = override.displayName.trim();
            return !trimmed.isEmpty() && !trimmed.equals(option.registryName);
        }

        private String getLocalizedEntityName(EntityLivingBase entity, String fallback) {
            if (entity != null) {
                String localizedFromEntity = StatCollector.translateToLocal(entity.getCommandSenderName());
                if (localizedFromEntity != null && !localizedFromEntity.trim()
                    .isEmpty()) {
                    return localizedFromEntity;
                }
            }
            if (fallback != null && !fallback.trim()
                .isEmpty()) {
                String localizedFallback = StatCollector.translateToLocal(fallback);
                if (localizedFallback != null && !localizedFallback.trim()
                    .isEmpty()) {
                    return localizedFallback;
                }
            }
            return "";
        }

        private String getPreviewName(EntityLivingBase entity) {
            String name = getDisplayName(getSelectedOption(), entity);
            if (this.previewChild && getSelectedOverrideOrDefault().appendBabyName) {
                return "Baby " + name;
            }
            return name;
        }

        private MobTypes getPreviewMobType(EntityLivingBase entity) {
            VarInstanceCommon.EntityOverride override = getExistingSelectedOverride();
            if (override != null) {
                return override.type;
            }
            if (entity != null) {
                return MobTypes.getTypeFor(entity);
            }
            return MobTypes.UNKNOWN;
        }

        private MobTypes getSelectedMobType() {
            VarInstanceCommon.EntityOverride override = getExistingSelectedOverride();
            if (override != null) {
                return override.type;
            }
            return getDetectedTypeForSelectedOption();
        }

        private void cycleSelectedMobType(int direction) {
            MobTypes[] values = MobTypes.values();
            MobTypes current = getSelectedMobType();
            int index = current.ordinal();
            index = Math.floorMod(index + direction, values.length);
            getOrCreateSelectedOverride().type = values[index];
        }

        private EntityLivingBase getPreviewEntity() {
            EntityOption option = getSelectedOption();
            if (option.className.isEmpty()) {
                return null;
            }

            Minecraft minecraft = Minecraft.getMinecraft();
            if (!option.className.equals(this.previewEntityClassName)) {
                this.previewEntity = null;
                this.previewEntityClassName = option.className;
                this.previewEntityCreationAttempted = false;
            }
            if (this.previewEntity == null && !this.previewEntityCreationAttempted) {
                this.previewEntity = createPreviewEntity(option, minecraft);
                this.previewEntityCreationAttempted = true;
            }

            applyPreviewChildState(this.previewEntity);
            syncPreviewEntityPosition(this.previewEntity, minecraft);
            return this.previewEntity;
        }

        private EntityLivingBase createPreviewEntity(EntityOption option, Minecraft minecraft) {
            World world = minecraft.theWorld;
            if (world == null && minecraft.thePlayer != null) {
                world = minecraft.thePlayer.worldObj;
            }
            if (world == null) {
                world = HudPreviewWorld.INSTANCE;
            }

            EntityLivingBase entity = PreviewEntityFactory.create(option.registryName, world, "entity override editor");
            if (entity != null) {
                syncPreviewEntityPosition(entity, minecraft);
                return entity;
            }
            return null;
        }

        private void syncPreviewEntityPosition(EntityLivingBase entity, Minecraft minecraft) {
            if (entity == null) {
                return;
            }

            if (minecraft.renderViewEntity != null && entity.worldObj == minecraft.renderViewEntity.worldObj) {
                entity.setPosition(
                    minecraft.renderViewEntity.posX,
                    minecraft.renderViewEntity.posY,
                    minecraft.renderViewEntity.posZ);
                return;
            }

            if (minecraft.thePlayer != null && entity.worldObj == minecraft.thePlayer.worldObj) {
                entity.setPosition(minecraft.thePlayer.posX, minecraft.thePlayer.posY, minecraft.thePlayer.posZ);
                return;
            }

            entity.setPosition(0.0D, 0.0D, 0.0D);
        }

        private void applyPreviewChildState(EntityLivingBase entity) {
            if (entity == null) {
                return;
            }
            if (entity instanceof EntityAgeable) {
                ((EntityAgeable) entity).setGrowingAge(this.previewChild ? -24000 : 0);
            } else if (entity instanceof EntityZombie) {
                ((EntityZombie) entity).setChild(this.previewChild);
            }
            applyCustomPreviewAgeState(entity, this.previewChild);
        }

        private void applyCustomPreviewAgeState(EntityLivingBase entity, boolean child) {
            if (child) {
                invokeBooleanSetter(entity, "setAdult", false);
                invokeIntSetter(entity, "setMoCAge", PREVIEW_MIN_CUSTOM_AGE);
                return;
            }
            invokeIntSetter(entity, "setMoCAge", PREVIEW_MAX_CUSTOM_AGE);
            invokeBooleanSetter(entity, "setAdult", true);
        }

        private void invokeBooleanSetter(EntityLivingBase entity, String methodName, boolean value) {
            invokeSetter(entity, methodName, Boolean.TYPE, Boolean.valueOf(value));
        }

        private void invokeIntSetter(EntityLivingBase entity, String methodName, int value) {
            invokeSetter(entity, methodName, Integer.TYPE, Integer.valueOf(value));
        }

        private void invokeSetter(EntityLivingBase entity, String methodName, Class<?> parameterType, Object value) {
            try {
                Method method = entity.getClass()
                    .getMethod(methodName, parameterType);
                method.invoke(entity, value);
            } catch (ReflectiveOperationException ignored) {}
        }

        private MobTypes getDetectedTypeForSelectedOption() {
            EntityLivingBase entity = getPreviewEntity();
            return entity == null ? MobTypes.UNKNOWN : MobTypes.getTypeFor(entity);
        }

        private String previewYawText() {
            return formatFloat(this.previewYaw);
        }

        private String previewPitchText() {
            return formatFloat(this.previewPitch);
        }
    }

    private static final class EntityOption {

        private final String registryName;
        private final String className;

        private EntityOption(String registryName, String className) {
            this.registryName = registryName;
            this.className = className;
        }
    }
}
