package industries.angelic.vintagedamageindicators.client;

import java.lang.reflect.Constructor;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import industries.angelic.vintagedamageindicators.VintageDamageIndicators;

public final class PreviewEntityFactory {

    private PreviewEntityFactory() {}

    public static EntityLivingBase create(String registryName, World world, String context) {
        Object classMapping = EntityList.stringToClassMapping.get(registryName);
        if (!(classMapping instanceof Class<?>)) {
            VintageDamageIndicators.LOG
                .warn("Failed to construct preview entity '{}' in {}: no class mapping found.", registryName, context);
            return null;
        }

        Class<?> entityClass = (Class<?>) classMapping;
        try {
            Constructor<?> constructor = entityClass.getConstructor(World.class);
            Object created = constructor.newInstance(world);
            if (created instanceof EntityLivingBase) {
                if (created instanceof EntityLiving living) {
                    try {
                        living.onSpawnWithEgg(null);
                    } catch (Exception e) {
                        VintageDamageIndicators.LOG.debug(
                            "onSpawnWithEgg failed for preview entity '{}' in {}, continuing without spawn init.",
                            registryName,
                            context,
                            e);
                    }
                }
                PreviewRenderPatches.adjustPreviewEntity((EntityLivingBase) created);
                return (EntityLivingBase) created;
            }

            VintageDamageIndicators.LOG.warn(
                "Failed to construct preview entity '{}' ({}) in {}: entity is not living.",
                registryName,
                entityClass.getName(),
                context);
            return null;
        } catch (Throwable throwable) {
            VintageDamageIndicators.LOG.warn(
                "Failed to construct preview entity '{}' ({}) in {}.",
                registryName,
                entityClass.getName(),
                context,
                throwable);
            return null;
        }
    }
}
