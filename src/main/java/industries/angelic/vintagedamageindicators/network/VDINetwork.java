package industries.angelic.vintagedamageindicators.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class VDINetwork {

    public static final String CHANNEL_NAME = "VDI";

    private static SimpleNetworkWrapper channel;
    private static boolean clientRegistered;

    private VDINetwork() {}

    public static void initCommon() {
        if (channel == null) {
            channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
        }
    }

    public static void initClient() {
        initCommon();
        if (!clientRegistered) {
            channel.registerMessage(
                EntityPotionEffectsMessage.Handler.class,
                EntityPotionEffectsMessage.class,
                0,
                Side.CLIENT);
            clientRegistered = true;
        }
    }

    public static SimpleNetworkWrapper getChannel() {
        initCommon();
        return channel;
    }
}
