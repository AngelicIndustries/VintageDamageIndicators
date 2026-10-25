package industries.angelic.vintagedamageindicators.util;

import net.minecraft.entity.EntityList;

import industries.angelic.vintagedamageindicators.VintageDamageIndicators;

public class MobUtil {

    public static void printMobNames() {
        VintageDamageIndicators.LOG.info("=========Mob List=========");
        VintageDamageIndicators.LOG.info(
            "The printing of this list is for you to know which mob has which name. You can disable this print in the configs.");
        for (Object e : EntityList.stringToClassMapping.keySet()) {
            VintageDamageIndicators.LOG.info(e + " (" + EntityList.stringToClassMapping.get(e) + ")");
        }
        VintageDamageIndicators.LOG.info("=============================");
    }

    public static String getClassByName(String name) {
        Object res = EntityList.stringToClassMapping.get(name);
        if (res != null) {

            return ((Class) res).getCanonicalName();
        }
        return null;
    }
}
