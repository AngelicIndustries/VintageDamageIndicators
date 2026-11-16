package industries.angelic.vintagedamageindicators.client;

import java.lang.reflect.Method;

import net.minecraft.entity.EntityLivingBase;

import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.varinstances.VarInstanceCommon;

public final class HudPreviewMath {

    private static final String THAUMCRAFT_GIANT_TAINTACLE = "thaumcraft.common.entities.monster.boss.EntityTaintacleGiant";

    public static final int PANEL_WIDTH = 208;
    public static final int PANEL_HEIGHT = 78;
    public static final int PREVIEW_CLIP_X = 4;
    public static final int PREVIEW_CLIP_Y = 4;
    public static final int PREVIEW_CLIP_WIDTH = 70;
    public static final int PREVIEW_CLIP_HEIGHT = 58;
    public static final int ENTITY_X = 45;
    public static final int ENTITY_Y = 58;
    public static final int ICON_X = 5;
    public static final int ICON_Y = 55;
    public static final int ICON_SIZE = 18;
    public static final int HEALTH_BAR_X = 81;
    public static final int HEALTH_BAR_Y = 25;
    public static final int HEALTH_BAR_WIDTH = 124;
    public static final int HEALTH_BAR_HEIGHT = 18;
    public static final int NAME_CENTER_X = 139;
    public static final int NAME_Y = 7;
    public static final float NAME_MAX_WIDTH = 113.0F;
    public static final int HEALTH_TEXT_CENTER_X = 136;
    public static final int HEALTH_TEXT_Y = 30;
    public static final float HEALTH_TEXT_MAX_WIDTH = 88.0F;
    public static final float DEFAULT_ADULT_SCALE_MULTIPLIER = 0.9F;
    public static final float DEFAULT_CHILD_SCALE_MODIFIER = 0.5F;
    public static final int DEFAULT_CHILD_Y_OFFSET = -4;
    /** The part of the preview window a measured mob is fitted into, inside its frame. */
    public static final int FIT_LEFT = 17;
    public static final int FIT_RIGHT = 73;
    public static final int FIT_TOP = 6;
    public static final int FIT_FLOOR = 59;
    /** A measured child is drawn this much smaller than the window it would fill, so it reads as young. */
    public static final float FIT_CHILD_SCALE = 0.6F;

    private HudPreviewMath() {}

    public static PreviewSettings resolvePreviewSettings(EntityLivingBase target,
        VarInstanceCommon.EntityOverride override) {
        return resolvePreviewSettings(target, override, Config.hudEntityYaw, Config.hudEntityPitch);
    }

    public static PreviewSettings resolvePreviewSettings(EntityLivingBase target,
        VarInstanceCommon.EntityOverride override, float baseYaw, float basePitch) {
        boolean vanillaChild = target.isChild();
        boolean customChild = isCustomChild(target);
        boolean child = vanillaChild || customChild;
        PreviewSettings fitted = resolveFittedSettings(target, override, child, baseYaw, basePitch);
        if (fitted != null) {
            return fitted;
        }
        PreviewTuning builtIn = getBuiltInPreviewTuning(target);
        float autoScale = getAutoScale(target);
        float scale = autoScale * builtIn.scaleMultiplier;
        float yaw = baseYaw;
        float pitch = basePitch;
        float roll = 0.0F;

        if (!child) {
            float adultScaleMultiplier = DEFAULT_ADULT_SCALE_MULTIPLIER;
            if (override != null && override.scaleFactor > 0.0F) {
                adultScaleMultiplier = override.scaleFactor;
            }
            scale *= adultScaleMultiplier;
        }

        if (override != null) {
            if (override.sizeModifier != 0.0F) {
                float sizeAdjustment = (3.0F - target.getEyeHeight()) * override.sizeModifier;
                scale += scale * sizeAdjustment;
            }
            if (child) {
                if (override.babyScaleFactor > 0.0F) {
                    scale *= override.babyScaleFactor;
                } else if (override.babyScaleModifier > 0.0F) {
                    scale *= override.babyScaleModifier;
                } else if (!customChild) {
                    scale *= DEFAULT_CHILD_SCALE_MODIFIER;
                }
            }
        } else if (vanillaChild) {
            scale *= DEFAULT_CHILD_SCALE_MODIFIER;
        }

        int x = ENTITY_X + builtIn.xOffset;
        int y = ENTITY_Y + builtIn.yOffset;
        if (override != null) {
            if (child) {
                x += Math.round(override.babyXOffset);
                if (override.babyYOffset != 0.0F) {
                    y += Math.round(override.babyYOffset);
                } else if (vanillaChild) {
                    y += DEFAULT_CHILD_Y_OFFSET;
                }
            } else {
                x += Math.round(override.xOffset);
                y += Math.round(override.yOffset);
            }
            yaw += override.yawOffset;
            pitch += override.pitchOffset;
            roll += override.rollOffset;
        } else if (vanillaChild) {
            y += DEFAULT_CHILD_Y_OFFSET;
        }

        return new PreviewSettings(x, y, Math.max(0.1F, scale), yaw, pitch, roll, autoScale, scale, builtIn);
    }

    /**
     * The mob fitted into the window from how big it really draws (PreviewFit): as large as fits, centered across, its
     * lowest point on the window's floor. Overrides scale and nudge it from there. Null when it can't be measured.
     */
    private static PreviewSettings resolveFittedSettings(EntityLivingBase target,
        VarInstanceCommon.EntityOverride override, boolean child, float baseYaw, float basePitch) {
        float yaw = baseYaw + (override != null ? override.yawOffset : 0.0F);
        float pitch = basePitch + (override != null ? override.pitchOffset : 0.0F);
        float roll = override != null ? override.rollOffset : 0.0F;
        PreviewFit.Bounds bounds = PreviewFit.get(target, child, yaw, pitch, roll);
        if (bounds == null || bounds.width() <= 0.0F || bounds.height() <= 0.0F) {
            return null;
        }
        float fitScale = Math.min((FIT_RIGHT - FIT_LEFT) / bounds.width(), (FIT_FLOOR - FIT_TOP) / bounds.height());
        float scale = fitScale;
        float xOffset = 0.0F;
        float yOffset = 0.0F;
        if (child) {
            if (override != null && override.babyScaleFactor > 0.0F) {
                scale *= override.babyScaleFactor;
            } else if (override != null && override.babyScaleModifier > 0.0F) {
                scale *= override.babyScaleModifier;
            } else {
                scale *= FIT_CHILD_SCALE;
            }
            if (override != null) {
                xOffset = override.babyXOffset;
                yOffset = override.babyYOffset;
            }
        } else if (override != null) {
            if (override.scaleFactor > 0.0F) {
                scale *= override.scaleFactor;
            }
            xOffset = override.xOffset;
            yOffset = override.yOffset;
        }
        int x = Math.round((FIT_LEFT + FIT_RIGHT) / 2.0F - bounds.centerX() * scale + xOffset);
        int y = Math.round(FIT_FLOOR - bounds.bottom * scale + yOffset);
        return new PreviewSettings(x, y, scale, yaw, pitch, roll, fitScale, scale, PreviewTuning.DEFAULT);
    }

    private static boolean isCustomChild(EntityLivingBase target) {
        Boolean adult = invokeBooleanGetter(target, "getIsAdult");
        return adult != null && !adult.booleanValue();
    }

    private static Boolean invokeBooleanGetter(EntityLivingBase target, String methodName) {
        try {
            Method method = target.getClass()
                .getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }

    public static float getAutoScale(EntityLivingBase target) {
        float width = Math.max(target.width, 0.1F);
        float height = Math.max(target.height, 0.1F);
        float biggestDimension = Math.max(width * 1.2F + 0.3F, height * 0.9F) * 0.85F;
        float scale = Config.hudEntitySize;
        if (biggestDimension > 0.5F) {
            scale /= biggestDimension;
        }
        return scale;
    }

    public static PreviewTuning getBuiltInPreviewTuning(EntityLivingBase target) {
        if (THAUMCRAFT_GIANT_TAINTACLE.equals(
            target.getClass()
                .getName())) {
            // Thaumcraft applies an extra 1.33x renderer scale on top of the entity's 6-block height.
            return new PreviewTuning(0.75F, 0, 0);
        }
        return PreviewTuning.DEFAULT;
    }

    public static String describeOverride(VarInstanceCommon.EntityOverride override) {
        if (override == null) {
            return "none";
        }
        return "scale=" + override.scaleFactor
            + ", size="
            + override.sizeModifier
            + ", babyScale="
            + override.babyScaleFactor
            + ", babyScaleModifier="
            + override.babyScaleModifier
            + ", x="
            + override.xOffset
            + ", y="
            + override.yOffset
            + ", babyX="
            + override.babyXOffset
            + ", babyY="
            + override.babyYOffset
            + ", yaw="
            + override.yawOffset
            + ", pitch="
            + override.pitchOffset
            + ", roll="
            + override.rollOffset;
    }

    public static final class PreviewSettings {

        public final int x;
        public final int y;
        public final float scale;
        public final float yaw;
        public final float pitch;
        public final float roll;
        public final float autoScale;
        public final float finalScale;
        public final PreviewTuning builtIn;

        public PreviewSettings(int x, int y, float scale, float yaw, float pitch, float roll, float autoScale,
            float finalScale, PreviewTuning builtIn) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
            this.autoScale = autoScale;
            this.finalScale = finalScale;
            this.builtIn = builtIn;
        }
    }

    public static final class PreviewTuning {

        public static final PreviewTuning DEFAULT = new PreviewTuning(1.0F, 0, 0);

        public final float scaleMultiplier;
        public final int xOffset;
        public final int yOffset;

        public PreviewTuning(float scaleMultiplier, int xOffset, int yOffset) {
            this.scaleMultiplier = scaleMultiplier;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }
    }
}
