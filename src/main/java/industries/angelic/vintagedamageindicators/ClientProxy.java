package industries.angelic.vintagedamageindicators;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import industries.angelic.vintagedamageindicators.command.OpenOverrideEditorCommand;
import industries.angelic.vintagedamageindicators.event.HudEventHandler;
import industries.angelic.vintagedamageindicators.event.ParticleEventHandler;
import industries.angelic.vintagedamageindicators.network.VDINetwork;
import industries.angelic.vintagedamageindicators.varinstances.VarInstanceClient;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        VintageDamageIndicators.varInstanceClient = new VarInstanceClient();
        VDINetwork.initClient();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        ParticleEventHandler particleEventHandler = new ParticleEventHandler();
        MinecraftForge.EVENT_BUS.register(particleEventHandler);

        HudEventHandler hudEventHandler = new HudEventHandler();
        MinecraftForge.EVENT_BUS.register(hudEventHandler);
        FMLCommonHandler.instance()
            .bus()
            .register(hudEventHandler);

        OpenOverrideEditorCommand openOverrideEditorCommand = new OpenOverrideEditorCommand();
        ClientCommandHandler.instance.registerCommand(openOverrideEditorCommand);
        FMLCommonHandler.instance()
            .bus()
            .register(openOverrideEditorCommand);
    }
}
