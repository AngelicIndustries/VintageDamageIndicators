package industries.angelic.vintagedamageindicators.client;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import industries.angelic.vintagedamageindicators.VintageDamageIndicators;

public final class HudEntityRenderer {

    private static final Set<Class<?>> RENDER_FAILED_CLASSES = new HashSet<>();
    private static final java.nio.FloatBuffer MATRIX_BUF = BufferUtils.createFloatBuffer(16);
    private static final java.nio.FloatBuffer MODELVIEW_BACKUP = BufferUtils.createFloatBuffer(16);
    private static final java.nio.FloatBuffer PROJECTION_BACKUP = BufferUtils.createFloatBuffer(16);
    private static final java.nio.FloatBuffer TEXTURE_BACKUP = BufferUtils.createFloatBuffer(16);
    private static EntityClientPlayerMP dummyPlayer;

    private HudEntityRenderer() {}

    private static void restoreStackDepth(int matrixMode, int stackDepthConstant, int targetDepth) {
        GL11.glMatrixMode(matrixMode);
        int currentDepth = GL11.glGetInteger(stackDepthConstant);
        while (currentDepth < targetDepth) {
            GL11.glPushMatrix();
            currentDepth++;
        }
        while (currentDepth > targetDepth) {
            GL11.glPopMatrix();
            currentDepth--;
        }
    }

    private static EntityClientPlayerMP getOrCreateDummyPlayer(Minecraft minecraft, World world) {
        if (dummyPlayer == null || dummyPlayer.worldObj != world) {
            dummyPlayer = new EntityClientPlayerMP(
                minecraft,
                world,
                minecraft.getSession(),
                null,
                new StatFileWriter());
        }
        return dummyPlayer;
    }

    public static void clearRenderFailedCache() {
        RENDER_FAILED_CLASSES.clear();
    }

    public static void drawEntity(int x, int y, float scale, float yawInput, float pitchInput, float rollInput,
        EntityLivingBase entity) {
        if (entity == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        RenderManager renderManager = RenderManager.instance;
        World oldWorld = renderManager.worldObj;
        TextureManager oldRenderEngine = renderManager.renderEngine;
        GameSettings oldOptions = renderManager.options;
        EntityLivingBase oldLivingPlayer = renderManager.livingPlayer;
        Entity oldViewEntity = renderManager.field_147941_i;
        double oldViewerPosX = renderManager.viewerPosX;
        double oldViewerPosY = renderManager.viewerPosY;
        double oldViewerPosZ = renderManager.viewerPosZ;
        float oldPlayerViewX = renderManager.playerViewX;
        float oldPlayerViewY = RenderManager.instance.playerViewY;
        int oldModelViewStackDepth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        int oldProjectionStackDepth = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
        int oldTextureStackDepth = GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH);
        float oldBossHealthScale = BossStatus.healthScale;
        int oldBossStatusBarTime = BossStatus.statusBarTime;
        String oldBossName = BossStatus.bossName;
        boolean oldBossHasColorModifier = BossStatus.hasColorModifier;
        float oldPrevRenderYawOffset = entity.prevRenderYawOffset;
        float oldRenderYawOffset = entity.renderYawOffset;
        float oldRotationYaw = entity.rotationYaw;
        float oldPrevRotationYaw = entity.prevRotationYaw;
        float oldRotationPitch = entity.rotationPitch;
        float oldPrevRotationPitch = entity.prevRotationPitch;
        float oldPrevRotationYawHead = entity.prevRotationYawHead;
        float oldRotationYawHead = entity.rotationYawHead;
        int oldDragonRingBufferIndex = 0;
        double[][] oldDragonRingBuffer = null;
        EntityEnderCrystal oldDragonHealingCrystal = null;
        HEEBossDragonBridge.PreviewState oldHEEDragonState = HEEBossDragonBridge.PreviewState.NONE;
        SpecialMobsBridge.PreviewState oldSpecialMobState = SpecialMobsBridge.PreviewState.NONE;
        double oldPosY = entity.posY;
        EntityClientPlayerMP oldPlayer = minecraft.thePlayer;
        EntityLivingBase oldRenderViewEntity = minecraft.renderViewEntity;

        World renderWorld = minecraft.theWorld != null ? minecraft.theWorld : entity.worldObj;
        EntityClientPlayerMP viewLiving = minecraft.thePlayer != null ? minecraft.thePlayer
            : getOrCreateDummyPlayer(minecraft, renderWorld);
        minecraft.thePlayer = viewLiving;
        Entity viewEntity = minecraft.renderViewEntity != null ? minecraft.renderViewEntity : viewLiving;
        minecraft.renderViewEntity = viewLiving;
        renderManager.cacheActiveRenderInfo(
            renderWorld,
            minecraft.getTextureManager(),
            minecraft.fontRenderer,
            viewLiving,
            viewEntity,
            minecraft.gameSettings,
            1.0F);

        MODELVIEW_BACKUP.clear();
        PROJECTION_BACKUP.clear();
        TEXTURE_BACKUP.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW_BACKUP);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION_BACKUP);
        GL11.glGetFloat(GL11.GL_TEXTURE_MATRIX, TEXTURE_BACKUP);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // Read the current modelview to compute absolute screen position. The modelview may
        // contain GUI widget offsets, a HUD scale, and the -2000 Z translation. We transform
        // the point (x, y, 50) through the full matrix to get absolute coordinates, then
        // replace the modelview with glLoadIdentity so the projection scale only affects our
        // known coordinates, not the GUI's pre-existing transforms.
        MATRIX_BUF.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX_BUF);
        float absX = MATRIX_BUF.get(0) * x + MATRIX_BUF.get(4) * y + MATRIX_BUF.get(8) * 50.0F + MATRIX_BUF.get(12);
        float absY = MATRIX_BUF.get(1) * x + MATRIX_BUF.get(5) * y + MATRIX_BUF.get(9) * 50.0F + MATRIX_BUF.get(13);
        float absZ = MATRIX_BUF.get(2) * x + MATRIX_BUF.get(6) * y + MATRIX_BUF.get(10) * 50.0F + MATRIX_BUF.get(14);
        // Extract pre-existing XY scale from the modelview (e.g. HUD indicator size) so
        // the entity renders at the correct size within the GUI's coordinate space.
        float mvScale = (float) Math
            .sqrt(MATRIX_BUF.get(0) * MATRIX_BUF.get(0) + MATRIX_BUF.get(1) * MATRIX_BUF.get(1));
        float effectiveScale = scale * mvScale;
        // Apply scale via projection matrix so renderers that strip modelview rotations
        // (billboard-style rendering) still render at the correct size.
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glScalef(effectiveScale, effectiveScale, 1.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(absX / effectiveScale, absY / effectiveScale, absZ);
        GL11.glScalef(-1.0F, 1.0F, 1.0F);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rollInput, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(-(float) Math.atan(pitchInput / 40.0F) * 20.0F, 1.0F, 0.0F, 0.0F);
        float previewBodyYaw = (float) Math.atan(yawInput / 40.0F) * 20.0F;
        float previewRotationYaw = (float) Math.atan(yawInput / 40.0F) * 40.0F;
        float previewRotationPitch = -((float) Math.atan(pitchInput / 40.0F)) * 20.0F;

        if (entity instanceof EntityDragon || HEEBossDragonBridge.isDragon(entity)) {
            previewBodyYaw += 180.0F;
            previewRotationYaw += 180.0F;
        }

        entity.prevRenderYawOffset = previewBodyYaw;
        entity.renderYawOffset = previewBodyYaw;
        entity.prevRotationYaw = previewRotationYaw;
        entity.rotationYaw = previewRotationYaw;
        entity.prevRotationPitch = previewRotationPitch;
        entity.rotationPitch = previewRotationPitch;
        entity.rotationYawHead = previewRotationYaw;
        entity.prevRotationYawHead = previewRotationYaw;

        if (entity instanceof EntityDragon dragon) {
            oldDragonRingBufferIndex = dragon.ringBufferIndex;
            oldDragonRingBuffer = new double[dragon.ringBuffer.length][3];
            for (int i = 0; i < dragon.ringBuffer.length; i++) {
                System.arraycopy(dragon.ringBuffer[i], 0, oldDragonRingBuffer[i], 0, dragon.ringBuffer[i].length);
            }
            dragon.ringBufferIndex = 0;
            for (int i = 0; i < dragon.ringBuffer.length; i++) {
                dragon.ringBuffer[i][0] = previewRotationYaw;
                dragon.ringBuffer[i][1] = entity.posY;
                dragon.ringBuffer[i][2] = 0.0D;
            }
            oldDragonHealingCrystal = dragon.healingEnderCrystal;
            dragon.healingEnderCrystal = null;
        }
        oldHEEDragonState = HEEBossDragonBridge.beginPreview(entity, previewRotationYaw);
        oldSpecialMobState = SpecialMobsBridge.beginPreview(entity);

        HudPreviewWorld previewWorld = entity.worldObj instanceof HudPreviewWorld ? (HudPreviewWorld) entity.worldObj
            : null;
        World oldEntityWorld = entity.worldObj;
        if (previewWorld == null) {
            entity.worldObj = HudPreviewWorld.INSTANCE;
        }
        boolean oldForceDark = previewWorld != null && previewWorld.isForceDark();
        boolean isEyesEntity = EyesCompatHelper.isEyesEntity(entity);
        boolean shouldForceDark = previewWorld != null && isEyesEntity;
        if (previewWorld != null && oldForceDark != shouldForceDark) {
            previewWorld.setForceDark(shouldForceDark);
        }
        EyesCompatHelper.PreviewRenderState eyesPreviewState = EyesCompatHelper
            .beginPreviewRender(renderManager, entity);

        GL11.glTranslatef(0.0F, entity.yOffset, 0.0F);
        RenderManager.instance.playerViewY = 180.0F;

        float oldRotationX = ActiveRenderInfo.rotationX;
        float oldRotationZ = ActiveRenderInfo.rotationZ;
        float oldRotationYZ = ActiveRenderInfo.rotationYZ;
        float oldRotationXY = ActiveRenderInfo.rotationXY;
        float oldRotationXZ = ActiveRenderInfo.rotationXZ;
        float previewYaw = 180.0F;
        float previewPitch = 0.0F;
        ActiveRenderInfo.rotationX = MathHelper.cos(previewYaw * (float) Math.PI / 180.0F);
        ActiveRenderInfo.rotationZ = MathHelper.sin(previewYaw * (float) Math.PI / 180.0F);
        ActiveRenderInfo.rotationYZ = -ActiveRenderInfo.rotationZ
            * MathHelper.sin(previewPitch * (float) Math.PI / 180.0F);
        ActiveRenderInfo.rotationXY = ActiveRenderInfo.rotationX
            * MathHelper.sin(previewPitch * (float) Math.PI / 180.0F);
        ActiveRenderInfo.rotationXZ = MathHelper.cos(previewPitch * (float) Math.PI / 180.0F);

        boolean noWorld = minecraft.theWorld == null;
        PreviewRenderPatches.PatchState patchState = PreviewRenderPatches.applyPatches(entity, noWorld);
        GL11.glDisable(GL11.GL_CULL_FACE);
        try {
            renderManager.livingPlayer = entity;
            if (!RENDER_FAILED_CLASSES.contains(entity.getClass())) {
                RenderManager.instance.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
            }
            PreviewRenderPatches.renderPostEffects(entity, noWorld, oldPrevRenderYawOffset, oldRenderYawOffset);
        } catch (Throwable e) {
            RENDER_FAILED_CLASSES.add(entity.getClass());
            VintageDamageIndicators.LOG.warn(
                "Preview render failed for {}, skipping future attempts.",
                entity.getClass()
                    .getName(),
                e);
        } finally {
            GL11.glEnable(GL11.GL_CULL_FACE);
            BossStatus.healthScale = oldBossHealthScale;
            BossStatus.statusBarTime = oldBossStatusBarTime;
            BossStatus.bossName = oldBossName;
            BossStatus.hasColorModifier = oldBossHasColorModifier;
            SpecialMobsBridge.endPreview(entity, oldSpecialMobState);
            PreviewRenderPatches.revertPatches(entity, patchState);
            EyesCompatHelper.endPreviewRender(renderManager, entity, eyesPreviewState);
            if (previewWorld != null && oldForceDark != shouldForceDark) {
                previewWorld.setForceDark(oldForceDark);
            }
        }
        entity.worldObj = oldEntityWorld;
        entity.prevRenderYawOffset = oldPrevRenderYawOffset;
        entity.renderYawOffset = oldRenderYawOffset;
        entity.prevRotationYaw = oldPrevRotationYaw;
        entity.rotationYaw = oldRotationYaw;
        entity.prevRotationPitch = oldPrevRotationPitch;
        entity.rotationPitch = oldRotationPitch;
        entity.prevRotationYawHead = oldPrevRotationYawHead;
        entity.rotationYawHead = oldRotationYawHead;
        entity.posY = oldPosY;

        if (entity instanceof EntityDragon dragon && oldDragonRingBuffer != null) {
            dragon.ringBufferIndex = oldDragonRingBufferIndex;
            for (int i = 0; i < dragon.ringBuffer.length; i++) {
                System.arraycopy(oldDragonRingBuffer[i], 0, dragon.ringBuffer[i], 0, dragon.ringBuffer[i].length);
            }
            dragon.healingEnderCrystal = oldDragonHealingCrystal;
        }
        HEEBossDragonBridge.endPreview(entity, oldHEEDragonState);

        ActiveRenderInfo.rotationX = oldRotationX;
        ActiveRenderInfo.rotationZ = oldRotationZ;
        ActiveRenderInfo.rotationYZ = oldRotationYZ;
        ActiveRenderInfo.rotationXY = oldRotationXY;
        ActiveRenderInfo.rotationXZ = oldRotationXZ;
        minecraft.thePlayer = oldPlayer;
        minecraft.renderViewEntity = oldRenderViewEntity;
        renderManager.worldObj = oldWorld;
        renderManager.renderEngine = oldRenderEngine;
        renderManager.options = oldOptions;
        renderManager.livingPlayer = oldLivingPlayer;
        renderManager.field_147941_i = oldViewEntity;
        renderManager.viewerPosX = oldViewerPosX;
        renderManager.viewerPosY = oldViewerPosY;
        renderManager.viewerPosZ = oldViewerPosZ;
        renderManager.playerViewX = oldPlayerViewX;
        renderManager.playerViewY = oldPlayerViewY;
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        restoreStackDepth(GL11.GL_MODELVIEW, GL11.GL_MODELVIEW_STACK_DEPTH, oldModelViewStackDepth);
        restoreStackDepth(GL11.GL_PROJECTION, GL11.GL_PROJECTION_STACK_DEPTH, oldProjectionStackDepth);
        restoreStackDepth(GL11.GL_TEXTURE, GL11.GL_TEXTURE_STACK_DEPTH, oldTextureStackDepth);
        MODELVIEW_BACKUP.rewind();
        PROJECTION_BACKUP.rewind();
        TEXTURE_BACKUP.rewind();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadMatrix(MODELVIEW_BACKUP);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadMatrix(PROJECTION_BACKUP);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadMatrix(TEXTURE_BACKUP);
        GL11.glPopAttrib();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private static final class HEEBossDragonBridge {

        private static final String ENTITY_BOSS_DRAGON_CLASS = "chylex.hee.entity.boss.EntityBossDragon";
        private static final Class<?> dragonClass = findDragonClass();
        private static final Field movementBufferField = findField("movementBuffer");
        private static final Field movementBufferIndexField = findField("movementBufferIndex");
        private static final Field healingEnderCrystalField = findField("healingEnderCrystal");

        private HEEBossDragonBridge() {}

        static boolean isDragon(EntityLivingBase entity) {
            return dragonClass != null && entity != null && dragonClass.isInstance(entity);
        }

        static PreviewState beginPreview(EntityLivingBase entity, float previewRotationYaw) {
            if (!isDragon(entity)) {
                return PreviewState.NONE;
            }

            try {
                int oldIndex = -1;
                double[][] backup = new double[0][];

                if (movementBufferField != null && movementBufferIndexField != null) {
                    double[][] movementBuffer = (double[][]) movementBufferField.get(entity);
                    if (movementBuffer != null && movementBuffer.length > 0) {
                        oldIndex = movementBufferIndexField.getInt(entity);
                        backup = new double[movementBuffer.length][];
                        for (int i = 0; i < movementBuffer.length; i++) {
                            double[] entry = movementBuffer[i];
                            if (entry == null) {
                                continue;
                            }
                            backup[i] = entry.clone();
                            if (entry.length > 0) {
                                entry[0] = previewRotationYaw;
                            }
                            if (entry.length > 1) {
                                entry[1] = entity.posY;
                            }
                        }
                        movementBufferIndexField.setInt(entity, 0);
                    }
                }

                EntityEnderCrystal oldHealingCrystal = null;
                if (healingEnderCrystalField != null) {
                    oldHealingCrystal = (EntityEnderCrystal) healingEnderCrystalField.get(entity);
                    healingEnderCrystalField.set(entity, null);
                }
                return new PreviewState(oldIndex, backup, oldHealingCrystal);
            } catch (ReflectiveOperationException | ClassCastException e) {
                VintageDamageIndicators.LOG.warn("Failed to patch Hardcore Ender Expansion dragon preview state.", e);
                return PreviewState.NONE;
            }
        }

        static void endPreview(EntityLivingBase entity, PreviewState state) {
            if (!isDragon(entity) || state == null || state == PreviewState.NONE) {
                return;
            }

            try {
                if (healingEnderCrystalField != null) {
                    healingEnderCrystalField.set(entity, state.oldHealingCrystal);
                }

                if (movementBufferField != null && movementBufferIndexField != null && state.oldIndex >= 0) {
                    double[][] movementBuffer = (double[][]) movementBufferField.get(entity);
                    if (movementBuffer == null) {
                        return;
                    }
                    movementBufferIndexField.setInt(entity, state.oldIndex);
                    for (int i = 0; i < movementBuffer.length && i < state.oldBuffer.length; i++) {
                        double[] backup = state.oldBuffer[i];
                        if (backup == null || movementBuffer[i] == null) {
                            continue;
                        }
                        System.arraycopy(
                            backup,
                            0,
                            movementBuffer[i],
                            0,
                            Math.min(backup.length, movementBuffer[i].length));
                    }
                }
            } catch (ReflectiveOperationException | ClassCastException e) {
                VintageDamageIndicators.LOG.warn("Failed to restore Hardcore Ender Expansion dragon preview state.", e);
            }
        }

        private static Class<?> findDragonClass() {
            try {
                return Class.forName(ENTITY_BOSS_DRAGON_CLASS);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        private static Field findField(String fieldName) {
            if (dragonClass == null) {
                return null;
            }
            try {
                Field field = dragonClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        private static final class PreviewState {

            private static final PreviewState NONE = new PreviewState(-1, new double[0][], null);

            private final int oldIndex;
            private final double[][] oldBuffer;
            private final EntityEnderCrystal oldHealingCrystal;

            private PreviewState(int oldIndex, double[][] oldBuffer, EntityEnderCrystal oldHealingCrystal) {
                this.oldIndex = oldIndex;
                this.oldBuffer = oldBuffer;
                this.oldHealingCrystal = oldHealingCrystal;
            }
        }
    }

    private static final class SpecialMobsBridge {

        private static final String SPECIAL_MOB_CLASS = "toast.specialMobs.entity.ISpecialMob";
        private static final String SPECIAL_MOB_DATA_CLASS = "toast.specialMobs.entity.SpecialMobData";
        private static final float HUD_RENDER_SCALE = 1.0F;
        private static final Class<?> specialMobClass = findClass(SPECIAL_MOB_CLASS);
        private static final int scaleWatcherId = findScaleWatcherId();

        private SpecialMobsBridge() {}

        static PreviewState beginPreview(EntityLivingBase entity) {
            if (!isSpecialMob(entity) || scaleWatcherId < 0) {
                return PreviewState.NONE;
            }

            try {
                float oldScale = entity.getDataWatcher()
                    .getWatchableObjectFloat(scaleWatcherId);
                if (oldScale != HUD_RENDER_SCALE) {
                    entity.getDataWatcher()
                        .updateObject(scaleWatcherId, Float.valueOf(HUD_RENDER_SCALE));
                }
                return new PreviewState(oldScale);
            } catch (RuntimeException e) {
                VintageDamageIndicators.LOG.warn("Failed to patch Special Mobs render scale for HUD preview.", e);
                return PreviewState.NONE;
            }
        }

        static void endPreview(EntityLivingBase entity, PreviewState state) {
            if (!isSpecialMob(entity) || scaleWatcherId < 0 || state == null || state == PreviewState.NONE) {
                return;
            }

            try {
                entity.getDataWatcher()
                    .updateObject(scaleWatcherId, Float.valueOf(state.oldScale));
            } catch (RuntimeException e) {
                VintageDamageIndicators.LOG.warn("Failed to restore Special Mobs render scale after HUD preview.", e);
            }
        }

        private static boolean isSpecialMob(EntityLivingBase entity) {
            return specialMobClass != null && entity != null && specialMobClass.isInstance(entity);
        }

        private static int findScaleWatcherId() {
            Class<?> specialMobDataClass = findClass(SPECIAL_MOB_DATA_CLASS);
            if (specialMobDataClass == null) {
                return -1;
            }
            try {
                Field field = specialMobDataClass.getDeclaredField("SCALE");
                field.setAccessible(true);
                return field.getByte(null);
            } catch (ReflectiveOperationException e) {
                return -1;
            }
        }

        private static Class<?> findClass(String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        private static final class PreviewState {

            private static final PreviewState NONE = new PreviewState(HUD_RENDER_SCALE);

            private final float oldScale;

            private PreviewState(float oldScale) {
                this.oldScale = oldScale;
            }
        }
    }
}
