package industries.angelic.vintagedamageindicators.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.network.ClientPotionEffectsCache;
import industries.angelic.vintagedamageindicators.network.EntityPotionEffectsMessage;
import industries.angelic.vintagedamageindicators.network.EntityPotionEffectsMessage.PotionEntry;
import industries.angelic.vintagedamageindicators.network.VDINetwork;

public class PotionSyncEventHandler {

    private final WeakHashMap<EntityLivingBase, Integer> potionIdentityHashes = new WeakHashMap<>();
    private final WeakHashMap<EntityLivingBase, Integer> potionDurationHashes = new WeakHashMap<>();
    private final Map<NetworkManager, Boolean> clientsWithChannel = Collections.synchronizedMap(new WeakHashMap<>());

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.entityLiving;
        if (entity == null || entity.worldObj == null
            || entity.worldObj.isRemote
            || !(entity.worldObj instanceof WorldServer)) {
            return;
        }

        Collection<PotionEffect> effects = entity.getActivePotionEffects();
        int identityHash = computeIdentityHash(effects);
        int durationHash = computeDurationHash(effects);
        Integer previousIdentityHash = this.potionIdentityHashes.get(entity);
        Integer previousDurationHash = this.potionDurationHashes.get(entity);
        boolean identityChanged = previousIdentityHash == null || previousIdentityHash.intValue() != identityHash;
        boolean durationChanged = previousDurationHash == null || previousDurationHash.intValue() != durationHash;
        if (!identityChanged && !durationChanged) {
            return;
        }

        this.potionIdentityHashes.put(entity, identityHash);
        this.potionDurationHashes.put(entity, durationHash);
        syncToTrackingPlayers(entity, (WorldServer) entity.worldObj, identityChanged, durationChanged);
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.target instanceof EntityLivingBase) || !(event.entityPlayer instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        if (!isChannelAvailable(player.playerNetServerHandler.netManager)) {
            return;
        }

        sendSnapshot((EntityLivingBase) event.target, player);
    }

    @SubscribeEvent
    public void onChannelRegistration(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.side != Side.SERVER) {
            return;
        }

        if ("REGISTER".equals(event.operation)) {
            this.clientsWithChannel.put(event.manager, event.registrations.contains(VDINetwork.CHANNEL_NAME));
        } else if ("UNREGISTER".equals(event.operation)) {
            this.clientsWithChannel.remove(event.manager);
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ServerDisconnectionFromClientEvent event) {
        this.clientsWithChannel.remove(event.manager);
    }

    @SubscribeEvent
    public void onServerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientPotionEffectsCache.clear();
    }

    private void syncToTrackingPlayers(EntityLivingBase entity, WorldServer world, boolean identityChanged,
        boolean durationChanged) {
        Set<EntityPlayer> trackingPlayers = world.getEntityTracker()
            .getTrackingPlayers(entity);
        if (trackingPlayers == null || trackingPlayers.isEmpty()) {
            return;
        }

        EntityPotionEffectsMessage allHidden = null;
        EntityPotionEffectsMessage typeOnly = null;
        EntityPotionEffectsMessage durationOnly = null;
        EntityPotionEffectsMessage typeAndDuration = null;

        for (EntityPlayer trackingPlayer : trackingPlayers) {
            if (trackingPlayer instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP) trackingPlayer;
                if (isChannelAvailable(player.playerNetServerHandler.netManager)) {
                    boolean operator = isPlayerOperator(player);
                    boolean sendTypes = Config.shouldSendPotionTypes(operator);
                    boolean sendDurations = Config.shouldSendPotionDurations(operator);
                    if (sendDurations) {
                        if (!identityChanged && !durationChanged) {
                            continue;
                        }
                        if (sendTypes) {
                            if (typeAndDuration == null) {
                                typeAndDuration = buildSnapshot(entity, true, true);
                            }
                            VDINetwork.getChannel()
                                .sendTo(typeAndDuration, player);
                        } else {
                            if (durationOnly == null) {
                                durationOnly = buildSnapshot(entity, false, true);
                            }
                            VDINetwork.getChannel()
                                .sendTo(durationOnly, player);
                        }
                    } else if (sendTypes) {
                        if (!identityChanged) {
                            continue;
                        }
                        if (typeOnly == null) {
                            typeOnly = buildSnapshot(entity, true, false);
                        }
                        VDINetwork.getChannel()
                            .sendTo(typeOnly, player);
                    } else {
                        if (allHidden == null) {
                            allHidden = buildSnapshot(entity, false, false);
                        }
                        VDINetwork.getChannel()
                            .sendTo(allHidden, player);
                    }
                }
            }
        }
    }

    private void sendSnapshot(EntityLivingBase entity, EntityPlayerMP player) {
        boolean operator = isPlayerOperator(player);
        VDINetwork.getChannel()
            .sendTo(
                buildSnapshot(
                    entity,
                    Config.shouldSendPotionTypes(operator),
                    Config.shouldSendPotionDurations(operator)),
                player);
    }

    private boolean isChannelAvailable(NetworkManager manager) {
        Boolean known = this.clientsWithChannel.get(manager);
        return Boolean.TRUE.equals(known) || manager.isLocalChannel();
    }

    private boolean isPlayerOperator(EntityPlayerMP player) {
        return player.canCommandSenderUseCommand(2, "vdi");
    }

    private EntityPotionEffectsMessage buildSnapshot(EntityLivingBase entity, boolean includeTypes,
        boolean includeDurations) {
        if (!includeTypes && !includeDurations) {
            return new EntityPotionEffectsMessage(entity.getEntityId(), Collections.<PotionEntry>emptyList());
        }

        List<PotionEntry> entries = new ArrayList<>();
        for (Object effectObject : entity.getActivePotionEffects()) {
            if (effectObject instanceof PotionEffect) {
                entries.add(PotionEntry.fromServerEffect((PotionEffect) effectObject, includeTypes, includeDurations));
            }
        }
        return new EntityPotionEffectsMessage(entity.getEntityId(), entries);
    }

    private int computeIdentityHash(Collection<PotionEffect> effects) {
        int hash = 1;
        for (PotionEffect effect : effects) {
            hash = 31 * hash + effect.getPotionID();
        }
        return hash;
    }

    private int computeDurationHash(Collection<PotionEffect> effects) {
        int hash = 1;
        for (PotionEffect effect : effects) {
            hash = 31 * hash + effect.getPotionID();
            hash = 31 * hash + effect.getDuration();
        }
        return hash;
    }
}
