package org.fentanylsolutions.vintagedamageindicators;

import java.io.File;

import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.vintagedamageindicators.varinstances.VarInstanceClient;
import org.fentanylsolutions.vintagedamageindicators.varinstances.VarInstanceCommon;
import org.joml.Quaternionf;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = VintageDamageIndicators.MODID,
    version = Tags.VERSION,
    name = "Vintage Damage Indicators",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    guiFactory = "org.fentanylsolutions.vintagedamageindicators.gui.GuiFactory",
    customProperties = { @Mod.CustomProperty(k = "license", v = "LGPLv3"),
        @Mod.CustomProperty(
            k = "issueTrackerUrl",
            v = "https://github.com/JackOfNoneTrades/VintageDamageIndicators/issues"),
        @Mod.CustomProperty(k = "iconFile", v = "assets/vintagedamageindicators/icon.png"),
        @Mod.CustomProperty(k = "backgroundFile", v = "assets/vintagedamageindicators/background.png") })
public class VintageDamageIndicators {

    public static final String MODID = "vintagedamageindicators";
    public static final String MODGROUP = "org.fentanylsolutions";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static boolean DEBUG_MODE;

    public static File confFile;

    @SidedProxy(
        clientSide = MODGROUP + "." + MODID + ".ClientProxy",
        serverSide = MODGROUP + "." + MODID + ".CommonProxy")
    public static CommonProxy proxy;

    public static VarInstanceCommon varInstanceCommon;
    public static VarInstanceClient varInstanceClient;

    private static final ResourceLocation DAMAGE_INDICATOR_TEXTURE = new ResourceLocation(
        MODID,
        "textures/gui/damage_indicator.png");
    private static final ResourceLocation DAMAGE_INDICATOR_BACKGROUND_TEXTURE = new ResourceLocation(
        MODID,
        "textures/gui/damage_indicator_background.png");
    private static final ResourceLocation DAMAGE_INDICATOR_HEALTH_TEXTURE = new ResourceLocation(
        MODID,
        "textures/gui/damage_indicator_health.png");
    private static final Quaternionf ENTITY_ROTATION = (new Quaternionf())
        .rotationXYZ((float) Math.toRadians(30), (float) Math.toRadians(130), (float) Math.PI);

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        String debugVar = System.getenv("MCMODDING_DEBUG_MODE");
        DEBUG_MODE = debugVar != null;
        VintageDamageIndicators.LOG.info("Debugmode: {}", DEBUG_MODE);
        File configDir = new File(event.getModConfigurationDirectory(), MODID);
        if (!configDir.exists() && !configDir.mkdirs()) {
            LOG.warn("Could not create config directory: {}", configDir.getAbsolutePath());
        }
        confFile = new File(configDir, MODID + ".cfg");
        proxy.preInit(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    public static void debug(String message) {
        if (DEBUG_MODE || Config.debugMode) {
            LOG.info("DEBUG: " + message);
        }
    }
}
