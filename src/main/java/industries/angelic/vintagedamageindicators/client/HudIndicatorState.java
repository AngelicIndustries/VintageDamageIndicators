package industries.angelic.vintagedamageindicators.client;

import net.minecraft.entity.EntityLivingBase;

import industries.angelic.vintagedamageindicators.MobTypes;

public class HudIndicatorState {

    private EntityLivingBase targetEntity;
    private MobTypes currentMobType = MobTypes.UNKNOWN;
    private int lingerTicks;
    private boolean renderModelOnly;

    public EntityLivingBase getTargetEntity() {
        return targetEntity;
    }

    public MobTypes getCurrentMobType() {
        return currentMobType;
    }

    public int getLingerTicks() {
        return lingerTicks;
    }

    public boolean isRenderModelOnly() {
        return renderModelOnly;
    }

    public void setTarget(EntityLivingBase targetEntity, MobTypes currentMobType, int lingerTicks,
        boolean renderModelOnly) {
        this.targetEntity = targetEntity;
        this.currentMobType = currentMobType == null ? MobTypes.UNKNOWN : currentMobType;
        this.lingerTicks = Math.max(lingerTicks, 0);
        this.renderModelOnly = renderModelOnly;
    }

    public void clearTarget() {
        targetEntity = null;
        currentMobType = MobTypes.UNKNOWN;
        lingerTicks = 0;
        renderModelOnly = false;
    }

    public void setLingerTicks(int lingerTicks) {
        this.lingerTicks = Math.max(lingerTicks, 0);
    }
}
