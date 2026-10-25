package industries.angelic.vintagedamageindicators.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class EntityPotionEffectsMessage implements IMessage {

    private int entityId;
    private List<PotionEntry> effects = new ArrayList<>();

    public EntityPotionEffectsMessage() {}

    public EntityPotionEffectsMessage(int entityId, List<PotionEntry> effects) {
        this.entityId = entityId;
        if (effects != null) {
            this.effects = effects;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        int size = buf.readUnsignedByte();
        this.effects = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            boolean showType = buf.readBoolean();
            int potionId = showType ? buf.readUnsignedByte() : -1;
            boolean showDuration = buf.readBoolean();
            int duration = showDuration ? buf.readInt() : -1;
            this.effects.add(new PotionEntry(showType, potionId, showDuration, duration));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeByte(this.effects.size());
        for (PotionEntry entry : this.effects) {
            buf.writeBoolean(entry.showType);
            if (entry.showType) {
                buf.writeByte(entry.potionId);
            }
            buf.writeBoolean(entry.showDuration);
            if (entry.showDuration) {
                buf.writeInt(entry.duration);
            }
        }
    }

    public static final class Handler implements IMessageHandler<EntityPotionEffectsMessage, IMessage> {

        @Override
        public IMessage onMessage(final EntityPotionEffectsMessage message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(() -> applyMessage(message));
            return null;
        }

        private static void applyMessage(EntityPotionEffectsMessage message) {
            ClientPotionEffectsCache.put(message.entityId, message.effects);
        }
    }

    public static final class PotionEntry {

        public final boolean showType;
        public final int potionId;
        public final boolean showDuration;
        public final int duration;

        public PotionEntry(boolean showType, int potionId, boolean showDuration, int duration) {
            this.showType = showType;
            this.potionId = potionId;
            this.showDuration = showDuration;
            this.duration = duration;
        }

        public boolean hasType() {
            return this.showType && this.potionId >= 0;
        }

        public boolean hasDuration() {
            return this.showDuration && this.duration >= 0;
        }

        public static PotionEntry fromServerEffect(net.minecraft.potion.PotionEffect effect, boolean includeType,
            boolean includeDuration) {
            return new PotionEntry(
                includeType,
                includeType ? effect.getPotionID() : -1,
                includeDuration,
                includeDuration ? effect.getDuration() : -1);
        }
    }
}
