package industries.angelic.vintagedamageindicators;

import java.util.Locale;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import industries.angelic.vintagedamageindicators.varinstances.VarInstanceCommon;

public enum MobTypes {

    PLAYER,
    UNDEAD,
    UNDEAD_ANIMAL,
    ANIMAL,
    WATER_ANIMAL,
    AMBIENT,
    MONSTER,
    WATER_MONSTER,
    ARTHROPOD,
    ARTHROPOD_MONSTER,
    WATER_ARTHROPOD,
    GOLEM,
    MONSTER_GOLEM,
    VILLAGER,
    ILLAGER,
    UNKNOWN,
    BOSS;

    private final ResourceLocation texture;

    MobTypes() {
        texture = new ResourceLocation(
            "vintagedamageindicators:textures/gui/mob_types/" + name().toLowerCase(Locale.ROOT) + ".png");
    }

    public static MobTypes getTypeFor(EntityLivingBase entity) {
        if (VintageDamageIndicators.varInstanceCommon != null) {
            VarInstanceCommon.EntityOverride eo = VintageDamageIndicators.varInstanceCommon
                .getEntityOverride(entity.getClass());
            if (eo != null) {
                return eo.type;
            }
        }

        if (entity instanceof EntityPlayer) {
            return PLAYER;
        }
        if (entity instanceof IBossDisplayData) {
            return BOSS;
        }
        if (entity instanceof EntityVillager) {
            return VILLAGER;
        }
        if (isIllagerLike(entity)) {
            return ILLAGER;
        }
        if (entity instanceof EntityGolem && entity instanceof IMob) {
            return MONSTER_GOLEM;
        }
        if (entity instanceof EntityGolem) {
            return GOLEM;
        }

        boolean hostile = entity instanceof IMob;
        boolean water = entity instanceof EntityWaterMob;
        EnumCreatureAttribute creatureAttribute = entity.getCreatureAttribute();

        if (creatureAttribute == EnumCreatureAttribute.UNDEAD) {
            return hostile ? UNDEAD : UNDEAD_ANIMAL;
        }
        if (creatureAttribute == EnumCreatureAttribute.ARTHROPOD) {
            if (water) {
                return WATER_ARTHROPOD;
            }
            return hostile ? ARTHROPOD_MONSTER : ARTHROPOD;
        }
        if (water) {
            return hostile ? WATER_MONSTER : WATER_ANIMAL;
        }
        if (hostile) {
            return MONSTER;
        }
        if (entity instanceof EntityAmbientCreature) {
            return AMBIENT;
        }
        if (entity instanceof EntityAnimal) {
            return ANIMAL;
        }

        return UNKNOWN;
    }

    private static boolean isIllagerLike(EntityLivingBase entity) {
        String className = entity.getClass()
            .getName()
            .toLowerCase(Locale.ROOT);
        return (className.contains("illager") && !className.contains("villager")) || className.contains("pillager")
            || className.contains("evoker")
            || className.contains("vindicator")
            || className.contains("illusioner")
            || className.contains("ravager");
    }

    public ResourceLocation getTexture() {
        return texture;
    }
}
