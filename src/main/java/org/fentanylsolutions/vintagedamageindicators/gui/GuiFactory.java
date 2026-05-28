package org.fentanylsolutions.vintagedamageindicators.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;

import org.fentanylsolutions.vintagedamageindicators.Config;
import org.fentanylsolutions.vintagedamageindicators.VintageDamageIndicators;
import org.fentanylsolutions.vintagedamageindicators.gui.angelica.EntityOverrideEditorScreen;
import org.fentanylsolutions.vintagedamageindicators.varinstances.VarInstanceCommon;

import com.google.common.collect.Lists;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.DummyConfigElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.IConfigElement;

@SuppressWarnings("unused")
public class GuiFactory implements IModGuiFactory {

    private static PendingEntityOverrideState pendingEntityOverrideState;

    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigGui.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class ConfigGui extends GuiConfig {

        private static final int LOGO_PREVIEW_BUTTON_ID = 9107;
        private static final int HUD_POSITION_BUTTON_ID = 9108;
        private GuiButton logoPreviewButton;
        private GuiButton hudPositionButton;

        public ConfigGui(GuiScreen parentScreen) {
            super(
                parentScreen,
                createRootElements(),
                VintageDamageIndicators.MODID,
                VintageDamageIndicators.MODID,
                false,
                false,
                "Vintage Damage Indicators");
            clearPendingEntityOverrideState();
        }

        @Override
        public void initGui() {
            super.initGui();
            this.logoPreviewButton = new GuiButton(LOGO_PREVIEW_BUTTON_ID, 4, 4, 14, 14, "L");
            this.hudPositionButton = new GuiButton(HUD_POSITION_BUTTON_ID, 20, 4, 78, 14, "Position HUD");
            this.buttonList.add(this.logoPreviewButton);
            this.buttonList.add(this.hudPositionButton);
            VintageDamageIndicators.debug("Initializing config gui");
        }

        @Override
        protected void actionPerformed(GuiButton b) {
            if (b.id == LOGO_PREVIEW_BUTTON_ID) {
                this.mc.displayGuiScreen(new LogoPreviewScreen(this));
                return;
            }
            if (b.id == HUD_POSITION_BUTTON_ID) {
                this.mc.displayGuiScreen(new HudPositionScreen(this));
                return;
            }
            VintageDamageIndicators.debug("Config button id " + b.id + " pressed");
            super.actionPerformed(b);
            /* "Done" button */
            if (b.id == 2000) {
                /* Syncing config */
                VintageDamageIndicators.debug("Saving config");
                applyPendingEntityOverrideState();
                Config.save();
                Config.loadConfig(VintageDamageIndicators.confFile);
            }
        }

        private static List<IConfigElement> createRootElements() {
            return Lists.newArrayList(
                new ConfigElement(
                    Config.getRawConfig()
                        .getCategory(Config.Categories.damageParticles)),
                new ConfigElement(
                    Config.getRawConfig()
                        .getCategory(Config.Categories.hudIndicator)),
                createEntityOverrideCategory(),
                new ConfigElement(
                    Config.getRawConfig()
                        .getCategory(Config.Categories.debug)));
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static IConfigElement createEntityOverrideCategory() {
            IConfigElement backingElement = new ConfigElement(
                Config.getRawConfig()
                    .getCategory(Config.Categories.entityOverrides));
            return new DummyConfigElement.DummyCategoryElement(
                "Entity Display Overrides",
                "vintagedamageindicators.configgui.entity-display-overrides",
                backingElement.getChildElements(),
                EntityOverrideCategoryEntry.class);
        }
    }

    public static class EntityOverrideCategoryEntry extends GuiConfigEntries.CategoryEntry {

        public EntityOverrideCategoryEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList,
            IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
        }

        @Override
        protected GuiScreen buildChildScreen() {
            return createEntityOverrideScreen(this.owningScreen);
        }
    }

    public static boolean openEntityOverrideScreen(GuiScreen parentScreen) {
        GuiScreen customScreen = createEntityOverrideScreen(parentScreen);
        Minecraft.getMinecraft()
            .displayGuiScreen(customScreen);
        return true;
    }

    private static GuiScreen createEntityOverrideScreen(GuiScreen parentScreen) {
        return new EntityOverrideEditorScreen(parentScreen);
    }

    public static PendingEntityOverrideState copyPendingEntityOverrideState() {
        return pendingEntityOverrideState == null ? null : pendingEntityOverrideState.copy();
    }

    public static void stageEntityOverrideState(Map<String, VarInstanceCommon.EntityOverride> overridesByClassName,
        List<String> preservedEntries, float previewYaw, float previewPitch) {
        pendingEntityOverrideState = new PendingEntityOverrideState(
            overridesByClassName,
            preservedEntries,
            previewYaw,
            previewPitch);
    }

    public static void clearPendingEntityOverrideState() {
        pendingEntityOverrideState = null;
    }

    public static void applyPendingEntityOverrideState() {
        if (pendingEntityOverrideState == null) {
            return;
        }
        applyEntityOverrideState(pendingEntityOverrideState);
        pendingEntityOverrideState = null;
    }

    public static void saveEntityOverrideState(Map<String, VarInstanceCommon.EntityOverride> overridesByClassName,
        List<String> preservedEntries, float previewYaw, float previewPitch) {
        applyEntityOverrideState(
            new PendingEntityOverrideState(overridesByClassName, preservedEntries, previewYaw, previewPitch));
        clearPendingEntityOverrideState();
        Config.save();
    }

    private static void applyEntityOverrideState(PendingEntityOverrideState state) {
        Config.setHudPreviewAngles(state.previewYaw, state.previewPitch);

        if (VintageDamageIndicators.varInstanceCommon != null) {
            VintageDamageIndicators.varInstanceCommon
                .replaceEntityOverrides(state.overridesByClassName, state.preservedEntries);
            return;
        }

        List<VarInstanceCommon.EntityOverride> serialized = new ArrayList<>();
        for (VarInstanceCommon.EntityOverride override : state.overridesByClassName.values()) {
            if (override != null) {
                serialized.add(override.copy());
            }
        }
        for (String preserved : state.preservedEntries) {
            if (preserved == null || preserved.trim()
                .isEmpty()) {
                continue;
            }
            VarInstanceCommon.EntityOverride parsed = VarInstanceCommon.EntityOverride.deserialize(preserved);
            if (parsed.className == null || parsed.className.trim()
                .isEmpty()) {
                continue;
            }
            serialized.add(parsed);
        }
        Config.setEntityOverrides(serialized);
    }

    private static LinkedHashMap<String, VarInstanceCommon.EntityOverride> copyOverrides(
        Map<String, VarInstanceCommon.EntityOverride> overridesByClassName) {
        LinkedHashMap<String, VarInstanceCommon.EntityOverride> copy = new LinkedHashMap<>();
        if (overridesByClassName == null) {
            return copy;
        }
        for (Map.Entry<String, VarInstanceCommon.EntityOverride> entry : overridesByClassName.entrySet()) {
            VarInstanceCommon.EntityOverride override = entry.getValue();
            if (override != null) {
                copy.put(entry.getKey(), override.copy());
            }
        }
        return copy;
    }

    public static final class PendingEntityOverrideState {

        public final LinkedHashMap<String, VarInstanceCommon.EntityOverride> overridesByClassName;
        public final ArrayList<String> preservedEntries;
        public final float previewYaw;
        public final float previewPitch;

        private PendingEntityOverrideState(Map<String, VarInstanceCommon.EntityOverride> overridesByClassName,
            List<String> preservedEntries, float previewYaw, float previewPitch) {
            this.overridesByClassName = copyOverrides(overridesByClassName);
            this.preservedEntries = preservedEntries == null ? new ArrayList<>() : new ArrayList<>(preservedEntries);
            this.previewYaw = previewYaw;
            this.previewPitch = previewPitch;
        }

        private PendingEntityOverrideState copy() {
            return new PendingEntityOverrideState(
                this.overridesByClassName,
                this.preservedEntries,
                this.previewYaw,
                this.previewPitch);
        }
    }
}
