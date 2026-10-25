package industries.angelic.vintagedamageindicators.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.MobTypes;
import industries.angelic.vintagedamageindicators.VintageDamageIndicators;
import industries.angelic.vintagedamageindicators.client.HudEntityRenderer;
import industries.angelic.vintagedamageindicators.client.HudIndicatorState;
import industries.angelic.vintagedamageindicators.client.HudPreviewMath;
import industries.angelic.vintagedamageindicators.client.HudPreviewParticles;
import industries.angelic.vintagedamageindicators.client.PotionIconRenderer;
import industries.angelic.vintagedamageindicators.client.PreviewRenderPatches;
import industries.angelic.vintagedamageindicators.network.ClientPotionEffectsCache;
import industries.angelic.vintagedamageindicators.network.EntityPotionEffectsMessage;
import industries.angelic.vintagedamageindicators.varinstances.VarInstanceCommon;

public class HudEventHandler extends Gui {

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
    private static final int POTION_ENTRY_GAP = 4;
    private static final int POTION_TIME_GAP = 2;
    private static final int POTION_TIME_Y = 9;
    private static final int POTION_TIME_COLOR = 0xFFFFFF;
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

    private final WeakHashMap<EntityLivingBase, PotionStripData> potionStripCache = new WeakHashMap<>();

    @SubscribeEvent
    public void onBossBarRender(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH && Config.disableBossBar
            && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.gameSettings.hideGUI) {
            return;
        }
        if (!Config.hudIndicatorEnabled) {
            return;
        }
        if (minecraft.gameSettings.showDebugInfo && Config.hideOnDebug) {
            return;
        }
        if (minecraft.currentScreen != null && !(minecraft.currentScreen instanceof GuiChat)) {
            return;
        }
        if (VintageDamageIndicators.varInstanceClient == null) {
            return;
        }

        HudIndicatorState state = VintageDamageIndicators.varInstanceClient.hudIndicatorState;
        EntityLivingBase target = state.getTargetEntity();
        if (target == null || target.isDead || !isHudEnabledFor(target) || isHiddenInvisiblePlayer(target)) {
            state.clearTarget();
            return;
        }

        PreviewRenderPatches.hudPartialTicks = event.partialTicks;
        renderHud(event.resolution, target, state.getCurrentMobType());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null) {
            ClientPotionEffectsCache.clear();
            return;
        }

        ClientPotionEffectsCache.tick();
        updateTargetStateLinger();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        HudPreviewParticles.tick();
        if (minecraft.thePlayer == null || VintageDamageIndicators.varInstanceClient == null) {
            return;
        }
        if (!Config.hudIndicatorEnabled) {
            clearTargetState();
            return;
        }
        if (minecraft.gameSettings.hideGUI) {
            clearTargetState();
            return;
        }
        if (minecraft.gameSettings.showDebugInfo && Config.hideOnDebug) {
            clearTargetState();
            return;
        }
        if (minecraft.currentScreen != null && !(minecraft.currentScreen instanceof GuiChat)) {
            clearTargetState();
            return;
        }

        updateTargetState(event.renderTickTime);
    }

    private void updateTargetState(float partialTicks) {
        HudIndicatorState state = VintageDamageIndicators.varInstanceClient.hudIndicatorState;
        EntityLivingBase currentTarget = state.getTargetEntity();
        if (currentTarget != null && (!isHudEnabledFor(currentTarget) || isHiddenInvisiblePlayer(currentTarget))) {
            state.clearTarget();
        }
        EntityLivingBase target = findTarget(Config.maxDistance, partialTicks);
        target = PreviewRenderPatches.resolveHudTarget(target);
        if (target != null && !target.isDead && target.getHealth() > 0.0F) {
            state.setTarget(target, MobTypes.getTypeFor(target), Config.hudLingerTime, usesModelOnlyRender(target));
            return;
        }

        if (state.getTargetEntity() == null || state.getTargetEntity().isDead) {
            state.clearTarget();
        }
    }

    private void updateTargetStateLinger() {
        HudIndicatorState state = VintageDamageIndicators.varInstanceClient.hudIndicatorState;
        if (state.getLingerTicks() > 0) {
            state.setLingerTicks(state.getLingerTicks() - 1);
        } else {
            state.clearTarget();
        }
    }

    private EntityLivingBase findTarget(double maxDistance, float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityLivingBase viewEntity = minecraft.renderViewEntity instanceof EntityLivingBase
            ? (EntityLivingBase) minecraft.renderViewEntity
            : minecraft.thePlayer;
        if (viewEntity == null || viewEntity.worldObj == null) {
            return null;
        }

        Vec3 eyePosition = viewEntity.getPosition(partialTicks);
        MovingObjectPosition blockHit = viewEntity.rayTrace(maxDistance, partialTicks);
        double closestDistance = blockHit == null ? maxDistance : eyePosition.distanceTo(blockHit.hitVec);
        Vec3 look = viewEntity.getLook(partialTicks);
        Vec3 lookEnd = eyePosition
            .addVector(look.xCoord * maxDistance, look.yCoord * maxDistance, look.zCoord * maxDistance);
        AxisAlignedBB searchBox = viewEntity.boundingBox
            .addCoord(look.xCoord * maxDistance, look.yCoord * maxDistance, look.zCoord * maxDistance)
            .expand(1.0D, 1.0D, 1.0D);
        List<Entity> candidates = viewEntity.worldObj.getEntitiesWithinAABBExcludingEntity(viewEntity, searchBox);
        EntityLivingBase closestTarget = null;

        for (Entity candidate : candidates) {
            if (!(candidate instanceof EntityLivingBase) || candidate.isInvisible() || candidate.isDead) {
                continue;
            }
            if (isHiddenInvisiblePlayer((EntityLivingBase) candidate)) {
                continue;
            }
            if (!isHudEnabledFor((EntityLivingBase) candidate)
                || PreviewRenderPatches.isHudSuppressed((EntityLivingBase) candidate)) {
                continue;
            }

            AxisAlignedBB candidateBox = candidate.boundingBox.expand(0.1D, 0.1D, 0.1D);
            MovingObjectPosition intercept = candidateBox.calculateIntercept(eyePosition, lookEnd);
            if (intercept == null) {
                continue;
            }

            double hitDistance = eyePosition.distanceTo(intercept.hitVec);
            if (hitDistance <= closestDistance) {
                closestDistance = hitDistance;
                closestTarget = (EntityLivingBase) candidate;
            }
        }

        return closestTarget;
    }

    private boolean usesModelOnlyRender(EntityLivingBase entity) {
        String className = entity.getClass()
            .getName();
        for (String oldRenderEntity : Config.oldRenderEntities) {
            if (oldRenderEntity != null && className.equals(oldRenderEntity.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isHudEnabledFor(EntityLivingBase entity) {
        if (entity == null) {
            return true;
        }
        if (PreviewRenderPatches.isHudSuppressed(entity)) {
            return false;
        }
        if (VintageDamageIndicators.varInstanceCommon == null) {
            return true;
        }
        VarInstanceCommon.EntityOverride override = VintageDamageIndicators.varInstanceCommon.getEntityOverride(
            entity.getClass()
                .getName());
        return override == null || override.enable;
    }

    private boolean isHiddenInvisiblePlayer(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer viewer = minecraft.thePlayer;
        return viewer != null && entity.isInvisibleToPlayer(viewer);
    }

    private void clearTargetState() {
        if (VintageDamageIndicators.varInstanceClient != null) {
            VintageDamageIndicators.varInstanceClient.hudIndicatorState.clearTarget();
        }
    }

    private void renderHud(ScaledResolution resolution, EntityLivingBase target, MobTypes mobType) {
        Minecraft minecraft = Minecraft.getMinecraft();
        PotionStripData potionStrip = getPotionStripData(target, minecraft.fontRenderer);
        boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        float scale = Config.hudIndicatorSize;
        int hudWidth = Math.max(PANEL_WIDTH, POTION_PANEL_X + potionStrip.stripWidth);
        int x = Config.hudIndicatorAlignLeft ? Config.hudIndicatorPositionX
            : resolution.getScaledWidth() - Math.round(hudWidth * scale) - Config.hudIndicatorPositionX;
        int y = Config.hudIndicatorAlignTop ? Config.hudIndicatorPositionY
            : resolution.getScaledHeight() - Math.round(PANEL_HEIGHT * scale) - Config.hudIndicatorPositionY;
        x = Math.max(0, x);
        y = Math.max(0, y);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);

        drawBackground(minecraft);
        drawEntityPreview(target, mobType);
        HudPreviewParticles.render(
            target,
            minecraft.fontRenderer,
            x,
            y,
            scale,
            resolution.getScaledHeight(),
            resolution.getScaleFactor());
        drawFrame(minecraft);
        drawMobTypeIcon(minecraft, mobType);
        drawHealthBar(minecraft, target);
        drawName(minecraft.fontRenderer, resolveHudName(target));
        drawHealthText(minecraft.fontRenderer, target);
        drawPotionStrip(minecraft, minecraft.fontRenderer, potionStrip);

        GL11.glPopMatrix();
        if (depthTestEnabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }

    private void drawBackground(Minecraft minecraft) {
        minecraft.getTextureManager()
            .bindTexture(
                new ResourceLocation(VintageDamageIndicators.MODID, "textures/gui/damage_indicator_background.png"));
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, Config.hudIndicatorBackgroundOpacity);
        drawTexturedModalRect(0, 0, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawFrame(Minecraft minecraft) {
        minecraft.getTextureManager()
            .bindTexture(new ResourceLocation(VintageDamageIndicators.MODID, "textures/gui/damage_indicator.png"));
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawTexturedModalRect(0, 0, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawEntityPreview(EntityLivingBase target, MobTypes mobType) {
        VarInstanceCommon.EntityOverride override = getEntityOverride(target);
        HudPreviewMath.PreviewSettings preview = HudPreviewMath.resolvePreviewSettings(target, override);
        logPreviewSettings(target, mobType, override, preview);
        HudEntityRenderer
            .drawEntity(preview.x, preview.y, preview.scale, preview.yaw, preview.pitch, preview.roll, target);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private VarInstanceCommon.EntityOverride getEntityOverride(EntityLivingBase target) {
        if (VintageDamageIndicators.varInstanceCommon == null) {
            return null;
        }
        return VintageDamageIndicators.varInstanceCommon.getEntityOverride(target.getClass());
    }

    private String resolveHudName(EntityLivingBase target) {
        if (target == null) {
            return "";
        }
        VarInstanceCommon.EntityOverride override = getEntityOverride(target);
        String name = resolveBaseHudName(target, override);
        if (override != null && override.appendBabyName && target.isChild()) {
            return "Baby " + name;
        }
        return name;
    }

    private String resolveBaseHudName(EntityLivingBase target, VarInstanceCommon.EntityOverride override) {
        if (hasCustomDisplayName(target, override)) {
            return StatCollector.translateToLocal(override.displayName);
        }
        return StatCollector.translateToLocal(target.getCommandSenderName());
    }

    private boolean hasCustomDisplayName(EntityLivingBase target, VarInstanceCommon.EntityOverride override) {
        if (override == null || override.displayName == null) {
            return false;
        }
        String trimmed = override.displayName.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String registryName = EntityList.getEntityString(target);
        return registryName == null || !trimmed.equals(registryName);
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

    private void drawHealthBar(Minecraft minecraft, EntityLivingBase target) {
        float health = Math.max(0.0F, Math.min(target.getHealth(), target.getMaxHealth()));
        float maxHealth = Math.max(target.getMaxHealth(), 0.0F);
        float ratio = maxHealth <= 0.0F ? 0.0F : health / maxHealth;
        int currentWidth = Math.round(HEALTH_BAR_WIDTH * ratio);
        int healthBarV = Config.colorblindHealthBar ? 36 : 0;

        minecraft.getTextureManager()
            .bindTexture(
                new ResourceLocation(VintageDamageIndicators.MODID, "textures/gui/damage_indicator_health.png"));
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

    private void drawName(FontRenderer fontRenderer, String name) {
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

    private void drawHealthText(FontRenderer fontRenderer, EntityLivingBase target) {
        float health = Math.max(0.0F, target.getHealth());
        float maxHealth = Math.max(0.0F, target.getMaxHealth());
        String separator = Config.healthSeparator ? " | " : "/";
        String healthText = formatHealth(health) + separator + formatHealth(maxHealth);
        if (!Config.healthDecimals) {
            healthText = (int) health + separator + (int) maxHealth;
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

    private void drawPotionStrip(Minecraft minecraft, FontRenderer fontRenderer, PotionStripData potionStrip) {
        if (potionStrip.entries.isEmpty()) {
            return;
        }

        int middleWidth = potionStrip.contentWidth;
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

        for (int index = 0; index < potionStrip.entries.size(); index++) {
            PotionStripEntry entry = potionStrip.entries.get(index);
            int textX = drawX;

            if (entry.potion != null) {
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
            if (index + 1 < potionStrip.entries.size()) {
                drawX += POTION_ENTRY_GAP;
            }
        }
    }

    private void drawStandaloneTexture(int x, int y, int width, int height) {
        Gui.func_146110_a(x, y, 0.0F, 0.0F, width, height, (float) width, (float) height);
    }

    private PotionStripData getPotionStripData(EntityLivingBase target, FontRenderer fontRenderer) {
        if (!Config.hudPotionEffectsEnabled) {
            return PotionStripData.EMPTY;
        }

        PotionStripData cached = this.potionStripCache.get(target);
        if (cached != null && cached.tick == target.ticksExisted && cached.showTime == Config.hudPotionEffectTime) {
            return cached;
        }

        List<EntityPotionEffectsMessage.PotionEntry> syncedEffects = ClientPotionEffectsCache.get(target.getEntityId());
        if (syncedEffects != null) {
            PotionStripData synced = buildPotionStripFromSynced(syncedEffects, fontRenderer, target.ticksExisted);
            this.potionStripCache.put(target, synced);
            return synced;
        }

        Collection<?> activeEffects = target.getActivePotionEffects();
        PotionStripData fallback = buildPotionStripFromVanilla(activeEffects, fontRenderer, target.ticksExisted);
        this.potionStripCache.put(target, fallback);
        return fallback;
    }

    private PotionStripData buildPotionStripFromSynced(List<EntityPotionEffectsMessage.PotionEntry> syncedEffects,
        FontRenderer fontRenderer, int tick) {
        if (syncedEffects.isEmpty()) {
            return PotionStripData.empty(tick, Config.hudPotionEffectTime);
        }

        List<PotionStripEntry> entries = new ArrayList<>();
        for (EntityPotionEffectsMessage.PotionEntry syncedEffect : syncedEffects) {
            Potion potion = null;
            if (syncedEffect.hasType() && syncedEffect.potionId >= 0
                && syncedEffect.potionId < Potion.potionTypes.length) {
                Potion resolved = Potion.potionTypes[syncedEffect.potionId];
                if (PotionIconRenderer.hasIcon(resolved)) {
                    potion = resolved;
                }
            }

            String timeText = Config.hudPotionEffectTime && syncedEffect.hasDuration()
                ? formatPotionDuration(syncedEffect.duration)
                : "";
            int entryWidth = 0;
            if (potion != null) {
                entryWidth += POTION_ICON_SIZE;
            }
            if (!timeText.isEmpty()) {
                if (entryWidth > 0) {
                    entryWidth += POTION_TIME_GAP;
                }
                entryWidth += fontRenderer.getStringWidth(timeText);
            }
            if (entryWidth <= 0) {
                continue;
            }

            PotionEffect effect = potion == null ? null
                : new PotionEffect(potion.id, syncedEffect.hasDuration() ? syncedEffect.duration : 0, 0);
            entries.add(
                new PotionStripEntry(
                    potion,
                    effect,
                    timeText,
                    entryWidth,
                    potion != null ? potion.id : Integer.MAX_VALUE));
        }

        return buildPotionStripData(entries, tick);
    }

    private PotionStripData buildPotionStripFromVanilla(Collection<?> activeEffects, FontRenderer fontRenderer,
        int tick) {
        if (activeEffects == null || activeEffects.isEmpty()) {
            return PotionStripData.empty(tick, Config.hudPotionEffectTime);
        }

        List<PotionStripEntry> entries = new ArrayList<>();
        for (Object effectObject : activeEffects) {
            if (!(effectObject instanceof PotionEffect)) {
                continue;
            }

            PotionEffect effect = (PotionEffect) effectObject;
            if (effect.getPotionID() < 0 || effect.getPotionID() >= Potion.potionTypes.length) {
                continue;
            }

            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (!PotionIconRenderer.hasIcon(potion)) {
                continue;
            }

            String timeText = Config.hudPotionEffectTime ? formatPotionDuration(effect.getDuration()) : "";
            int entryWidth = POTION_ICON_SIZE;
            if (Config.hudPotionEffectTime) {
                entryWidth += POTION_TIME_GAP + fontRenderer.getStringWidth(timeText);
            }
            entries.add(new PotionStripEntry(potion, effect, timeText, entryWidth, potion.id));
        }

        return buildPotionStripData(entries, tick);
    }

    private PotionStripData buildPotionStripData(List<PotionStripEntry> entries, int tick) {
        if (entries.isEmpty()) {
            return PotionStripData.empty(tick, Config.hudPotionEffectTime);
        }

        Collections.sort(entries, Comparator.comparingInt(entry -> entry.sortOrder));

        int contentWidth = POTION_CONTENT_PADDING_LEFT + POTION_CONTENT_PADDING_RIGHT;
        for (int index = 0; index < entries.size(); index++) {
            contentWidth += entries.get(index).width;
            if (index + 1 < entries.size()) {
                contentWidth += POTION_ENTRY_GAP;
            }
        }

        PotionStripData built = new PotionStripData(
            entries,
            POTION_LEFT_WIDTH + contentWidth + POTION_RIGHT_WIDTH,
            contentWidth,
            tick,
            Config.hudPotionEffectTime);
        return built;
    }

    private String formatPotionDuration(int duration) {
        return StringUtils.ticksToElapsedTime(Math.max(0, duration));
    }

    private String formatHealth(float value) {
        float rounded = Math.round(value * 5.0F) / 5.0F;
        String text = Float.toString(rounded);
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }

    private void logPreviewSettings(EntityLivingBase target, MobTypes mobType,
        VarInstanceCommon.EntityOverride override, HudPreviewMath.PreviewSettings preview) {
        if (!VintageDamageIndicators.DEBUG_MODE && !Config.debugMode) {
            return;
        }
        if (target.ticksExisted % 20 != 0) {
            return;
        }

        VintageDamageIndicators.debug(
            "Preview " + target.getClass()
                .getName()
                + " type="
                + mobType
                + " bbox="
                + target.width
                + "x"
                + target.height
                + " eye="
                + target.getEyeHeight()
                + " autoScale="
                + preview.autoScale
                + " builtIn="
                + preview.builtIn.scaleMultiplier
                + "/"
                + preview.builtIn.xOffset
                + "/"
                + preview.builtIn.yOffset
                + " finalScale="
                + preview.finalScale
                + " finalPos="
                + preview.x
                + ","
                + preview.y
                + " angle="
                + preview.yaw
                + "/"
                + preview.pitch
                + "/"
                + preview.roll
                + " override="
                + HudPreviewMath.describeOverride(override));
    }

    private static final class PotionStripData {

        private static final PotionStripData EMPTY = new PotionStripData(
            Collections.<PotionStripEntry>emptyList(),
            0,
            0,
            -1,
            false);

        private final List<PotionStripEntry> entries;
        private final int stripWidth;
        private final int contentWidth;
        private final int tick;
        private final boolean showTime;

        private PotionStripData(List<PotionStripEntry> entries, int stripWidth, int contentWidth, int tick,
            boolean showTime) {
            this.entries = entries;
            this.stripWidth = stripWidth;
            this.contentWidth = contentWidth;
            this.tick = tick;
            this.showTime = showTime;
        }

        private static PotionStripData empty(int tick, boolean showTime) {
            return new PotionStripData(Collections.<PotionStripEntry>emptyList(), 0, 0, tick, showTime);
        }
    }

    private static final class PotionStripEntry {

        private final Potion potion;
        private final PotionEffect effect;
        private final String timeText;
        private final int width;
        private final int sortOrder;

        private PotionStripEntry(Potion potion, PotionEffect effect, String timeText, int width, int sortOrder) {
            this.potion = potion;
            this.effect = effect;
            this.timeText = timeText;
            this.width = width;
            this.sortOrder = sortOrder;
        }
    }
}
