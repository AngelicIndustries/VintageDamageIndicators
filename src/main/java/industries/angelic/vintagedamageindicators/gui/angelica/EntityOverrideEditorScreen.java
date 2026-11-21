package industries.angelic.vintagedamageindicators.gui.angelica;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.angellib.gui.sodiumgui.SodiumGui;
import industries.angelic.angellib.gui.sodiumgui.SodiumGuiButton;
import industries.angelic.angellib.gui.sodiumgui.SodiumGuiScrollState;
import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.MobTypes;
import industries.angelic.vintagedamageindicators.VintageDamageIndicators;
import industries.angelic.vintagedamageindicators.client.HudEntityRenderer;
import industries.angelic.vintagedamageindicators.client.HudPreviewMath;
import industries.angelic.vintagedamageindicators.client.HudPreviewWorld;
import industries.angelic.vintagedamageindicators.client.PreviewEntityFactory;
import industries.angelic.vintagedamageindicators.client.PreviewFit;
import industries.angelic.vintagedamageindicators.gui.GuiFactory;
import industries.angelic.vintagedamageindicators.varinstances.VarInstanceCommon;

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
    private static final int NAME_UNDERLAY_X = HudPreviewMath.HEALTH_BAR_X + 1;
    private static final int NAME_UNDERLAY_Y = 4;
    private static final int NAME_UNDERLAY_WIDTH = HudPreviewMath.HEALTH_BAR_WIDTH - 2;
    private static final int NAME_UNDERLAY_HEIGHT = 16;
    /** A grid cell is the HUD's mob window, at its own size, with the name under it. */
    private static final int GRID_WINDOW_X = 15;
    private static final int GRID_WINDOW_Y = 3;
    private static final int GRID_WINDOW_SIZE = 60;
    private static final int GRID_LABEL_HEIGHT = 11;
    private static final int GRID_GAP = 4;
    private static final int GRID_FAILED_COLOR = 0xFFC04040;
    private static final int GRID_HIDDEN_COLOR = 0xA0000000;
    private static final String CHECK_REPORT = "config/vintagedamageindicators/preview-check.txt";

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
    private int previewX;
    private int previewY;
    private boolean draggingPreview;
    private int dragStartMouseX;
    private int dragStartMouseY;
    private float dragStartOffsetX;
    private float dragStartOffsetY;
    private final SodiumGuiButton gridButton;
    private final SodiumGuiButton checkButton;
    private boolean gridMode;
    private int gridScrollRows;
    private PreviewCheck check;
    private String checkStatus;

    public EntityOverrideEditorScreen(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.state = new EditorState(parentScreen instanceof GuiFactory.ConfigGui);
        this.pageButton = new SodiumGuiButton("Entity Overrides", () -> setGridMode(false));
        this.gridButton = new SodiumGuiButton("All Mobs", () -> setGridMode(true));
        this.checkButton = new SodiumGuiButton("Check All", this::startCheck);
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
        this.pageButton.setSelected(!this.gridMode);
        this.gridButton.setSelected(this.gridMode);
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
        if (this.gridMode) {
            this.drawGrid(mouseX, mouseY);
        } else {
            this.drawPreviewPane(mouseX, mouseY, partialTicks);
            this.drawOptionRows(mouseX, mouseY);
        }
        this.drawBottomBar(mouseX, mouseY, partialTicks);
        this.drawTooltip(mouseX, mouseY);
        if (this.check != null) {
            this.check.step();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        this.clearTextFocus();
        boolean handled = false;

        if (this.pageButton.mouseClicked(mouseX, mouseY, mouseButton)
            || this.gridButton.mouseClicked(mouseX, mouseY, mouseButton)
            || this.gridMode && this.checkButton.mouseClicked(mouseX, mouseY, mouseButton)
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

        if (!handled && this.gridMode) {
            handled = this.clickGrid(mouseX, mouseY, mouseButton);
        }

        if (!handled && !this.gridMode && mouseButton == 0 && isOverPreviewWindow(mouseX, mouseY)) {
            VarInstanceCommon.EntityOverride override = this.state.getOrCreateSelectedOverride();
            this.draggingPreview = true;
            this.dragStartMouseX = mouseX;
            this.dragStartMouseY = mouseY;
            this.dragStartOffsetX = this.state.previewChild ? override.babyXOffset : override.xOffset;
            this.dragStartOffsetY = this.state.previewChild ? override.babyYOffset : override.yOffset;
            handled = true;
        }

        if (!handled && !this.gridMode
            && SodiumGui
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
        if (!this.gridMode && isOverPreviewWindow(this.lastMouseX, this.lastMouseY)) {
            resizePreview(wheel);
            return;
        }
        if (!this.gridMode && this.lastMouseX >= this.rightX + this.rightWidth / 2
            && SodiumGui.isInside(
                this.lastMouseX,
                this.lastMouseY,
                this.rightX,
                this.optionListY,
                this.rightWidth,
                this.optionListHeight)) {
            int rowIndex = this.optionScroll.getRowAt(this.lastMouseY, this.optionListY, this.optionRows.size());
            if (rowIndex >= 0 && this.optionRows.get(rowIndex)
                .scrolled(wheel, isShiftKeyDown())) {
                return;
            }
        }
        if (this.gridMode && this.lastMouseX >= this.rightX) {
            this.gridScrollRows = Math
                .max(0, Math.min(getMaxGridScroll(), this.gridScrollRows - Integer.signum(wheel)));
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
        if (mouseButton == 0 && this.draggingPreview) {
            VarInstanceCommon.EntityOverride override = this.state.getOrCreateSelectedOverride();
            float x = this.dragStartOffsetX + (mouseX - this.dragStartMouseX);
            float y = this.dragStartOffsetY + (mouseY - this.dragStartMouseY);
            if (this.state.previewChild) {
                override.babyXOffset = x;
                override.babyYOffset = y;
            } else {
                override.xOffset = x;
                override.yOffset = y;
            }
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
            this.draggingPreview = false;
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
        this.optionListY = this.rightY + HudPreviewMath.PANEL_HEIGHT + SECTION_GAP;
        this.optionListHeight = Math.max(SodiumGui.ROW_HEIGHT, this.height - this.optionListY - BOTTOM_BAR_HEIGHT);

        this.searchField.xPosition = this.leftX + 6;
        this.searchField.yPosition = this.leftY + SEARCH_TEXT_Y_OFFSET;
        this.searchField.width = Math.max(10, this.leftWidth - 12);
        this.searchField.height = this.fontRendererObj.FONT_HEIGHT;

        int pageWidth = 16 + this.fontRendererObj.getStringWidth(this.pageButton.getLabel());
        this.pageButton.setBounds(OUTER_PADDING, PAGE_BUTTON_Y, pageWidth, 18);
        int gridWidth = 16 + this.fontRendererObj.getStringWidth(this.gridButton.getLabel());
        this.gridButton.setBounds(OUTER_PADDING + pageWidth + 4, PAGE_BUTTON_Y, gridWidth, 18);
        this.checkButton.setBounds(OUTER_PADDING, this.height - 26, 72, 20);
        this.checkButton.setEnabled(this.check == null);

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
        this.gridButton.render(this.fontRendererObj, mouseX, mouseY);
        int titleX = this.gridButton.getX() + this.gridButton.getWidth() + COLUMN_GAP;
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
            VarInstanceCommon.EntityOverride override = this.state.overridesByClassName.get(option.className);
            boolean hidden = override != null && !override.enable;
            String label = this.state.getDisplayName(option);
            label = SodiumGui.ellipsize(this.fontRendererObj, label, rowWidth - 16);
            this.fontRendererObj.drawString(
                label,
                this.leftX + TEXT_PADDING,
                y + 5,
                hidden ? 0xFF808080 : selected ? 0xFFFFFFFF : 0xFFE0E0E0);
            if (override != null && !hidden) {
                SodiumGui.drawFlatBox(
                    this.leftX + rowWidth - 7,
                    y + SodiumGui.ROW_HEIGHT / 2 - 1,
                    3,
                    3,
                    SodiumGui.getDefaultAccentColor());
            }
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
        this.previewX = previewX;
        this.previewY = previewY;
        drawHudPreview(previewX, previewY);
        if (isOverPreviewWindow(mouseX, mouseY)) {
            this.activeTooltip = "Drag to move the mob, scroll to resize it. Hold Shift for small steps.";
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
        if (this.gridMode) {
            this.checkButton.render(this.fontRendererObj, mouseX, mouseY);
            if (this.checkStatus != null) {
                String status = SodiumGui.ellipsize(
                    this.fontRendererObj,
                    this.checkStatus,
                    this.undoButton.getX() - this.checkButton.getX() - this.checkButton.getWidth() - 12);
                this.fontRendererObj.drawString(
                    status,
                    this.checkButton.getX() + this.checkButton.getWidth() + 6,
                    this.height - 20,
                    0xFFE0E0E0);
            }
        }
        this.undoButton.render(this.fontRendererObj, mouseX, mouseY);
        this.cancelButton.render(this.fontRendererObj, mouseX, mouseY);
        this.saveButton.render(this.fontRendererObj, mouseX, mouseY);
    }

    private void setGridMode(boolean grid) {
        this.gridMode = grid;
        this.pageButton.setSelected(!grid);
        this.gridButton.setSelected(grid);
    }

    private int getGridColumns() {
        return Math.max(1, (this.rightWidth + GRID_GAP) / (GRID_WINDOW_SIZE + GRID_GAP));
    }

    private int getGridVisibleRows() {
        int listHeight = this.height - CONTENT_TOP - BOTTOM_BAR_HEIGHT;
        return Math.max(1, (listHeight + GRID_GAP) / (GRID_WINDOW_SIZE + GRID_LABEL_HEIGHT + GRID_GAP));
    }

    private int getMaxGridScroll() {
        int rows = (this.state.getFilteredOptions()
            .size() + getGridColumns() - 1) / getGridColumns();
        return Math.max(0, rows - getGridVisibleRows());
    }

    /** Every mob that matches the search, each in the HUD's mob window, so badly fitted ones stand out. */
    private void drawGrid(int mouseX, int mouseY) {
        List<EntityOption> options = this.state.getFilteredOptions();
        this.gridScrollRows = Math.min(this.gridScrollRows, getMaxGridScroll());
        int columns = getGridColumns();
        int first = this.gridScrollRows * columns;
        int end = Math.min(options.size(), first + getGridVisibleRows() * columns);
        for (int i = first; i < end; i++) {
            int cell = i - first;
            int x = this.rightX + cell % columns * (GRID_WINDOW_SIZE + GRID_GAP);
            int y = CONTENT_TOP + cell / columns * (GRID_WINDOW_SIZE + GRID_LABEL_HEIGHT + GRID_GAP);
            EntityOption option = options.get(i);
            drawGridCell(option, x, y);
            if (SodiumGui.isInside(mouseX, mouseY, x, y, GRID_WINDOW_SIZE, GRID_WINDOW_SIZE + GRID_LABEL_HEIGHT)) {
                SodiumGui.drawRectOutline(x - 1, y - 1, GRID_WINDOW_SIZE + 2, GRID_WINDOW_SIZE + 2, 0xFFFFFFFF);
                this.activeTooltip = this.state.getDisplayName(option) + "\n" + option.className;
            }
        }
    }

    private void drawGridCell(EntityOption option, int x, int y) {
        Minecraft minecraft = Minecraft.getMinecraft();
        SodiumGui.drawFlatBox(x, y, GRID_WINDOW_SIZE, GRID_WINDOW_SIZE, 0xFF8A929C);
        EntityLivingBase entity = this.state.getGridEntity(option, minecraft);
        VarInstanceCommon.EntityOverride override = this.state.overridesByClassName.get(option.className);
        boolean failed = entity == null || HudEntityRenderer.hasRenderFailed(entity.getClass());
        if (entity != null && !failed) {
            HudPreviewMath.PreviewSettings preview = HudPreviewMath
                .resolvePreviewSettings(entity, override, this.state.previewYaw, this.state.previewPitch);
            SodiumGui.beginScissor(minecraft, x, y, GRID_WINDOW_SIZE, GRID_WINDOW_SIZE);
            GL11.glPushMatrix();
            // The window sits this far into the HUD panel, so the mob lands where it would there
            GL11.glTranslatef(x - GRID_WINDOW_X, y - GRID_WINDOW_Y, 0.0F);
            HudEntityRenderer
                .drawEntity(preview.x, preview.y, preview.scale, preview.yaw, preview.pitch, preview.roll, entity);
            GL11.glPopMatrix();
            SodiumGui.endScissor();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
        if (failed) {
            SodiumGui.drawRectOutline(x, y, GRID_WINDOW_SIZE, GRID_WINDOW_SIZE, GRID_FAILED_COLOR);
            this.fontRendererObj
                .drawString("!", x + GRID_WINDOW_SIZE / 2 - 1, y + GRID_WINDOW_SIZE / 2 - 4, GRID_FAILED_COLOR);
        }
        if (override != null && !override.enable) {
            SodiumGui.drawFlatBox(x, y, GRID_WINDOW_SIZE, GRID_WINDOW_SIZE, GRID_HIDDEN_COLOR);
            this.fontRendererObj.drawString("hidden", x + 2, y + 2, 0xFFAAAAAA);
        } else if (override != null) {
            SodiumGui.drawFlatBox(x + GRID_WINDOW_SIZE - 5, y + 2, 3, 3, SodiumGui.getDefaultAccentColor());
        }
        String label = SodiumGui.ellipsize(this.fontRendererObj, this.state.getDisplayName(option), GRID_WINDOW_SIZE);
        this.fontRendererObj.drawString(label, x, y + GRID_WINDOW_SIZE + 2, 0xFFE0E0E0);
    }

    private boolean clickGrid(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || mouseX < this.rightX) {
            return false;
        }
        List<EntityOption> options = this.state.getFilteredOptions();
        int columns = getGridColumns();
        int column = (mouseX - this.rightX) / (GRID_WINDOW_SIZE + GRID_GAP);
        int row = (mouseY - CONTENT_TOP) / (GRID_WINDOW_SIZE + GRID_LABEL_HEIGHT + GRID_GAP);
        if (column >= columns || row < 0 || row >= getGridVisibleRows() || mouseY < CONTENT_TOP) {
            return false;
        }
        int index = (this.gridScrollRows + row) * columns + column;
        if (index >= options.size()) {
            return false;
        }
        this.state.select(options.get(index));
        this.rebuildOptionRows();
        this.optionScroll.setScroll(0.0F, SodiumGui.getContentHeight(this.optionRows.size()), this.optionListHeight);
        setGridMode(false);
        SodiumGuiButton.playClickSound();
        return true;
    }

    private void startCheck() {
        try {
            this.check = new PreviewCheck(new File(Minecraft.getMinecraft().mcDataDir, CHECK_REPORT));
            this.checkStatus = "Checking...";
        } catch (IOException e) {
            this.checkStatus = "Couldn't write " + CHECK_REPORT;
        }
    }

    /**
     * Spawns, measures and draws every mob in turn, one a frame, logging each to the report. The mob's name is
     * written before it's tried, so if one brings the game down, the report's last line names it.
     */
    private final class PreviewCheck {

        private final PrintWriter out;
        private int index;
        private int spawnFailed;
        private int drawFailed;
        private int drewNothing;

        private PreviewCheck(File report) throws IOException {
            File parent = report.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            this.out = new PrintWriter(new FileWriter(report));
            this.out.println(
                "# Every living mob, spawned without a world and drawn as the HUD draws it. Fit is the drawing's size in"
                    + " blocks at the HUD's angle.");
        }

        private void step() {
            List<EntityOption> options = EntityOverrideEditorScreen.this.state.options;
            if (this.index >= options.size()) {
                finish(options.size());
                return;
            }
            EntityOption option = options.get(this.index++);
            this.out.println("checking " + option.className);
            this.out.flush();
            long start = System.nanoTime();
            EntityLivingBase entity = EntityOverrideEditorScreen.this.state
                .createPreviewEntity(option, Minecraft.getMinecraft());
            String result;
            if (entity == null) {
                this.spawnFailed++;
                result = "SPAWN FAILED";
            } else {
                VarInstanceCommon.EntityOverride override = EntityOverrideEditorScreen.this.state.overridesByClassName
                    .get(option.className);
                float yaw = Config.hudEntityYaw + (override != null ? override.yawOffset : 0.0F);
                float pitch = Config.hudEntityPitch + (override != null ? override.pitchOffset : 0.0F);
                float roll = override != null ? override.rollOffset : 0.0F;
                PreviewFit.Bounds bounds = PreviewFit.get(entity, false, yaw, pitch, roll);
                if (HudEntityRenderer.hasRenderFailed(entity.getClass())) {
                    this.drawFailed++;
                    result = "DRAW FAILED";
                } else if (bounds == null || bounds == PreviewFit.NOTHING) {
                    this.drewNothing++;
                    result = "DREW NOTHING";
                } else {
                    result = String.format(
                        Locale.ROOT,
                        "ok, fit %.2f x %.2f, hitbox %.2f x %.2f",
                        bounds.width(),
                        bounds.height(),
                        entity.width,
                        entity.height);
                }
            }
            this.out.println(
                String.format(
                    Locale.ROOT,
                    "%s %s (%s), %d ms",
                    result,
                    option.className,
                    option.registryName,
                    (System.nanoTime() - start) / 1000000L));
            this.out.flush();
            EntityOverrideEditorScreen.this.checkStatus = "Checking " + this.index + " of " + options.size() + "...";
        }

        private void finish(int total) {
            String summary = String.format(
                Locale.ROOT,
                "Checked %d mobs: %d failed to spawn, %d failed to draw, %d drew nothing. Report in %s",
                total,
                this.spawnFailed,
                this.drawFailed,
                this.drewNothing,
                CHECK_REPORT);
            this.out.println("# " + summary);
            this.out.close();
            EntityOverrideEditorScreen.this.checkStatus = summary;
            EntityOverrideEditorScreen.this.check = null;
        }
    }

    /** The HUD panel's mob window in the preview, where dragging moves the mob and scrolling resizes it. */
    private boolean isOverPreviewWindow(int mouseX, int mouseY) {
        return SodiumGui.isInside(
            mouseX,
            mouseY,
            this.previewX + GRID_WINDOW_X,
            this.previewY + GRID_WINDOW_Y,
            GRID_WINDOW_SIZE,
            GRID_WINDOW_SIZE);
    }

    /** Grows or shrinks the shown mob by 5% a notch, 1% with Shift, for the age shown. */
    private void resizePreview(int wheel) {
        VarInstanceCommon.EntityOverride override = this.state.getOrCreateSelectedOverride();
        float factor = 1.0F + Integer.signum(wheel) * (isShiftKeyDown() ? 0.01F : 0.05F);
        if (this.state.previewChild) {
            float current = override.babyScaleFactor > 0.0F ? override.babyScaleFactor : HudPreviewMath.FIT_CHILD_SCALE;
            override.babyScaleFactor = Math.round(current * factor * 1000.0F) / 1000.0F;
        } else {
            override.scaleFactor = Math.round(scaleOrOne(override.scaleFactor) * factor * 1000.0F) / 1000.0F;
        }
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
                "Off hides this mob from the HUD.",
                () -> this.state.getSelectedOverrideOrDefault().enable,
                value -> this.state.getOrCreateSelectedOverride().enable = value));
        this.optionRows.add(
            new TextRow(
                "Name",
                "The name the HUD shows.",
                () -> this.state.getSelectedOverrideOrDefault().displayName == null ? ""
                    : this.state.getSelectedOverrideOrDefault().displayName,
                value -> this.state.getOrCreateSelectedOverride().displayName = value,
                false));
        this.optionRows.add(new MobTypeRow("Mob Type", "The icon shown beside the mob."));
        this.optionRows.add(
            new CycleRow(
                "Preview Age",
                "Shows the mob grown or as a child. The size and offsets below change for the age shown.",
                () -> this.state.previewChild ? "Child" : "Adult",
                direction -> this.state.previewChild = !this.state.previewChild));
        this.optionRows.add(
            new TextRow(
                "Scale",
                "Size against the automatic fit, which fills the window: 1 is the fit. Scroll over the preview to change it.",
                () -> formatFloat(scaleOrOne(this.state.getSelectedOverrideOrDefault().scaleFactor)),
                value -> this.state.getOrCreateSelectedOverride().scaleFactor = parseFloat(value),
                0.05F));
        this.optionRows.add(
            new TextRow(
                "X Offset",
                "Pixels from the automatic spot. Drag the mob in the preview to move it.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().xOffset),
                value -> this.state.getOrCreateSelectedOverride().xOffset = parseFloat(value),
                1.0F));
        this.optionRows.add(
            new TextRow(
                "Y Offset",
                "Pixels from the automatic spot, where the mob stands on the window's floor. Drag the mob to move it.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().yOffset),
                value -> this.state.getOrCreateSelectedOverride().yOffset = parseFloat(value),
                1.0F));
        this.optionRows.add(
            new TextRow(
                "Child Scale",
                "A child's size against the window it would fill; 0.6 when not set.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().babyScaleFactor),
                value -> this.state.getOrCreateSelectedOverride().babyScaleFactor = parseFloat(value),
                0.05F));
        this.optionRows.add(
            new TextRow(
                "Child X Offset",
                "Pixels from a child's automatic spot.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().babyXOffset),
                value -> this.state.getOrCreateSelectedOverride().babyXOffset = parseFloat(value),
                1.0F));
        this.optionRows.add(
            new TextRow(
                "Child Y Offset",
                "Pixels from a child's automatic spot.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().babyYOffset),
                value -> this.state.getOrCreateSelectedOverride().babyYOffset = parseFloat(value),
                1.0F));
        this.optionRows.add(
            new TextRow(
                "Yaw Offset",
                "Turns this mob further left or right, for models drawn facing an odd way.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().yawOffset),
                value -> this.state.getOrCreateSelectedOverride().yawOffset = parseFloat(value),
                5.0F));
        this.optionRows.add(
            new TextRow(
                "Pitch Offset",
                "Tilts this mob forward or back.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().pitchOffset),
                value -> this.state.getOrCreateSelectedOverride().pitchOffset = parseFloat(value),
                5.0F));
        this.optionRows.add(
            new TextRow(
                "Roll Offset",
                "Rolls this mob sideways, like a fish drawn lying on its side.",
                () -> formatFloat(this.state.getSelectedOverrideOrDefault().rollOffset),
                value -> this.state.getOrCreateSelectedOverride().rollOffset = parseFloat(value),
                5.0F));
        this.optionRows.add(
            new ToggleRow(
                "Popoff",
                "Damage numbers popping off this mob.",
                () -> this.state.getSelectedOverrideOrDefault().popoffEnabled,
                value -> this.state.getOrCreateSelectedOverride().popoffEnabled = value));
        this.optionRows.add(
            new ToggleRow(
                "Append Baby Name",
                "Puts Baby before a child's name.",
                () -> this.state.getSelectedOverrideOrDefault().appendBabyName,
                value -> this.state.getOrCreateSelectedOverride().appendBabyName = value));
        this.optionRows.add(
            new TextRow(
                "Preview Yaw",
                "The angle every mob is shown at, in the HUD too.",
                this.state::previewYawText,
                value -> this.state.previewYaw = parseFloat(value),
                5.0F));
        this.optionRows.add(
            new TextRow(
                "Preview Pitch",
                "The tilt every mob is shown at, in the HUD too.",
                this.state::previewPitchText,
                value -> this.state.previewPitch = parseFloat(value),
                5.0F));
        this.optionRows.add(new ButtonRow("Reset Selected Override", "Removes this mob's override.", () -> {
            this.state.resetSelectedOverride();
            this.rebuildOptionRows();
        }));
    }

    /** Scale fields count 0, not set, as 1. */
    private static float scaleOrOne(float scale) {
        return scale > 0.0F ? scale : 1.0F;
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

        /** A mouse wheel notch over the row; true if it changed something. */
        protected boolean scrolled(int wheel, boolean fine) {
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

        /** How much a wheel notch changes a number; 0 for text. */
        private final float step;

        private TextRow(String label, String tooltip, TextGetter getter, TextSetter setter, float step) {
            this(label, tooltip, getter, setter, true, step);
        }

        private TextRow(String label, String tooltip, TextGetter getter, TextSetter setter, boolean numeric) {
            this(label, tooltip, getter, setter, numeric, 0.0F);
        }

        private TextRow(String label, String tooltip, TextGetter getter, TextSetter setter, boolean numeric,
            float step) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
            this.step = step;
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
        protected boolean scrolled(int wheel, boolean fine) {
            if (this.step <= 0.0F) {
                return false;
            }
            float value = parseFloat(this.getter.get()) + Integer.signum(wheel) * this.step * (fine ? 0.2F : 1.0F);
            String text = formatFloat(Math.round(value * 1000.0F) / 1000.0F);
            this.setter.set(text);
            this.field.setText(text);
            return true;
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
                override.layout = VarInstanceCommon.EntityOverride.LAYOUT_FITTED;
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
                // Mobs are named by entity.<registry name>.name, the way a spawned one would be
                String entityKey = "entity." + fallback + ".name";
                if (StatCollector.canTranslate(entityKey)) {
                    return StatCollector.translateToLocal(entityKey);
                }
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

        private final Map<String, EntityLivingBase> gridEntities = new HashMap<>();

        /** A mob for the grid, made once per class; null if it can't be made. */
        private EntityLivingBase getGridEntity(EntityOption option, Minecraft minecraft) {
            if (!this.gridEntities.containsKey(option.className)) {
                EntityLivingBase entity = createPreviewEntity(option, minecraft);
                // The grid shows grown mobs; spawning can roll a child, like a zombie's
                if (entity instanceof EntityAgeable) {
                    ((EntityAgeable) entity).setGrowingAge(0);
                } else if (entity instanceof EntityZombie) {
                    ((EntityZombie) entity).setChild(false);
                }
                if (entity != null) {
                    applyCustomPreviewAgeState(entity, false);
                }
                this.gridEntities.put(option.className, entity);
            }
            return this.gridEntities.get(option.className);
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
