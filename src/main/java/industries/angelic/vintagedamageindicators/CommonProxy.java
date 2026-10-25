package industries.angelic.vintagedamageindicators;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import industries.angelic.vintagedamageindicators.event.PotionSyncEventHandler;
import industries.angelic.vintagedamageindicators.network.VDINetwork;
import industries.angelic.vintagedamageindicators.util.MobUtil;
import industries.angelic.vintagedamageindicators.varinstances.VarInstanceCommon;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        VintageDamageIndicators.varInstanceCommon = new VarInstanceCommon();
        VintageDamageIndicators.LOG.info("I am " + VintageDamageIndicators.MODID + " at version " + Tags.VERSION);
        Config.loadConfig(VintageDamageIndicators.confFile);
        VDINetwork.initCommon();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        PotionSyncEventHandler potionSyncEventHandler = new PotionSyncEventHandler();
        MinecraftForge.EVENT_BUS.register(potionSyncEventHandler);
        FMLCommonHandler.instance()
            .bus()
            .register(potionSyncEventHandler);
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        if (Config.printMobs) {
            MobUtil.printMobNames();
        }

        VintageDamageIndicators.varInstanceCommon.postInitHook();
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}
}
