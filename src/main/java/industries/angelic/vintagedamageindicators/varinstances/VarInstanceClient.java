package industries.angelic.vintagedamageindicators.varinstances;

import net.minecraft.util.ResourceLocation;

import industries.angelic.vintagedamageindicators.VintageDamageIndicators;
import industries.angelic.vintagedamageindicators.client.HudIndicatorState;

public class VarInstanceClient {

    public final HudIndicatorState hudIndicatorState = new HudIndicatorState();

    public static ResourceLocation FRAME = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/damage_indicator.png");
    public static ResourceLocation BACKGROUND = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/damage_indicator_background.png");
    public static ResourceLocation HEALTH = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/damage_indicator_health.png");

    public static ResourceLocation ICON_AMBIENT = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/ambient.png");
    public static ResourceLocation ICON_ANIMAL = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/animal.png");
    public static ResourceLocation ICON_ARTHROPOD = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/arthropod.png");
    public static ResourceLocation ICON_ARTHROPOD_MONSTER = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/arthropod_monster.png");
    public static ResourceLocation ICON_BOSS = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/boss.png");
    public static ResourceLocation ICON_GOLEM = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/golem.png");
    public static ResourceLocation ICON_ILLAGER = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/illager.png");
    public static ResourceLocation ICON_MONSTER = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/monster.png");
    public static ResourceLocation ICON_PLAYER = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/plaer.png");
    public static ResourceLocation ICON_UNDEAD = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/undead.png");
    public static ResourceLocation ICON_UNDEAD_ANIMAL = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/undead_animal.png");
    public static ResourceLocation ICON_UNKNOWN = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/unknown.png");
    public static ResourceLocation ICON_VILLAGER = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/villager.png");
    public static ResourceLocation ICON_WATER_ANIMAL = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/water_animal.png");
    public static ResourceLocation ICON_WATER_ARTHROPOD = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/water_arthropod.png");
    public static ResourceLocation ICON_WATER_MONSTER = new ResourceLocation(
        VintageDamageIndicators.MODID,
        "textures/gui/mob_types/water_monster.png");
}
