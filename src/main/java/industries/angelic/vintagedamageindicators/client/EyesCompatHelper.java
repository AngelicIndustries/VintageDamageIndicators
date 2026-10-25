package industries.angelic.vintagedamageindicators.client;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;

import org.fentanylsolutions.eyesintheshadows.entity.entities.EntityEyes;

import cpw.mods.fml.common.Loader;

public final class EyesCompatHelper {

    private static final boolean EYES_IN_THE_SHADOWS_LOADED = Loader.isModLoaded("eyesintheshadows");

    private EyesCompatHelper() {}

    public static boolean isEyesEntity(EntityLivingBase entity) {
        return EYES_IN_THE_SHADOWS_LOADED && EyesBridge.isEyesEntity(entity);
    }

    public static PreviewRenderState beginPreviewRender(RenderManager renderManager, EntityLivingBase entity) {
        if (!isEyesEntity(entity)) {
            return PreviewRenderState.NONE;
        }

        EntityLivingBase oldLivingPlayer = renderManager.livingPlayer;
        renderManager.livingPlayer = entity;
        int oldTargetId = EyesBridge.setTargetId(entity, -1);
        return new PreviewRenderState(oldLivingPlayer, oldTargetId);
    }

    public static void endPreviewRender(RenderManager renderManager, EntityLivingBase entity,
        PreviewRenderState state) {
        if (state == null || state == PreviewRenderState.NONE) {
            return;
        }

        renderManager.livingPlayer = state.oldLivingPlayer;
        if (isEyesEntity(entity)) {
            EyesBridge.setTargetId(entity, state.oldTargetId);
        }
    }

    public static final class PreviewRenderState {

        public static final PreviewRenderState NONE = new PreviewRenderState(null, -1);

        private final EntityLivingBase oldLivingPlayer;
        private final int oldTargetId;

        private PreviewRenderState(EntityLivingBase oldLivingPlayer, int oldTargetId) {
            this.oldLivingPlayer = oldLivingPlayer;
            this.oldTargetId = oldTargetId;
        }
    }

    private static final class EyesBridge {

        // Eyes in the Shadows 0.0.3 stores the render target in DataWatcher slot 23.
        private static final int TARGET_ID_WATCHER_INDEX = 23;

        private EyesBridge() {}

        private static boolean isEyesEntity(EntityLivingBase entity) {
            return entity instanceof EntityEyes;
        }

        private static int setTargetId(EntityLivingBase entity, int targetId) {
            EntityEyes eyes = (EntityEyes) entity;
            int oldTargetId = eyes.getTargetId();
            eyes.getDataWatcher()
                .updateObject(TARGET_ID_WATCHER_INDEX, targetId);
            return oldTargetId;
        }
    }
}
