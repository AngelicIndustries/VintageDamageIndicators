package industries.angelic.vintagedamageindicators.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.World;

/**
 * The potion effects the server sent for each entity. Durations count down from the client tick they arrived on, so
 * nothing has to be walked each tick, and entities that left the world are let go now and then: the server sends a
 * snapshot for every living entity a player starts tracking, so a long session would otherwise keep them all.
 */
public final class ClientPotionEffectsCache {

    /** How often, in client ticks, entries of entities that left the world are let go. */
    private static final int PRUNE_INTERVAL = 100;
    /** An entry this new is kept even before its entity shows up: the snapshot can come before the spawn. */
    private static final int SPAWN_GRACE = 20;

    private static final Map<Integer, Snapshot> EFFECTS_BY_ENTITY = new ConcurrentHashMap<>();
    private static long ticks;

    private ClientPotionEffectsCache() {}

    public static void put(int entityId, List<EntityPotionEffectsMessage.PotionEntry> entries) {
        EFFECTS_BY_ENTITY.put(
            Integer.valueOf(entityId),
            new Snapshot(
                entries == null || entries.isEmpty() ? Collections.<EntityPotionEffectsMessage.PotionEntry>emptyList()
                    : new ArrayList<>(entries),
                ticks));
    }

    public static List<EntityPotionEffectsMessage.PotionEntry> get(int entityId) {
        Snapshot snapshot = EFFECTS_BY_ENTITY.get(Integer.valueOf(entityId));
        if (snapshot == null) {
            return null;
        }
        if (snapshot.entries.isEmpty()) {
            return Collections.emptyList();
        }

        int elapsed = (int) Math.min(Integer.MAX_VALUE, ticks - snapshot.receivedAt);
        List<EntityPotionEffectsMessage.PotionEntry> entries = new ArrayList<>(snapshot.entries.size());
        for (EntityPotionEffectsMessage.PotionEntry entry : snapshot.entries) {
            int duration = entry.showDuration && entry.duration > 0 ? Math.max(0, entry.duration - elapsed)
                : entry.duration;
            entries.add(
                new EntityPotionEffectsMessage.PotionEntry(
                    entry.showType,
                    entry.potionId,
                    entry.showDuration,
                    duration));
        }
        return entries;
    }

    public static void tick(World world) {
        ticks++;
        if (ticks % PRUNE_INTERVAL == 0) {
            EFFECTS_BY_ENTITY.entrySet()
                .removeIf(
                    entry -> ticks - entry.getValue().receivedAt > SPAWN_GRACE
                        && world.getEntityByID(entry.getKey()) == null);
        }
    }

    public static void clear() {
        EFFECTS_BY_ENTITY.clear();
    }

    private static final class Snapshot {

        private final List<EntityPotionEffectsMessage.PotionEntry> entries;
        private final long receivedAt;

        private Snapshot(List<EntityPotionEffectsMessage.PotionEntry> entries, long receivedAt) {
            this.entries = entries;
            this.receivedAt = receivedAt;
        }
    }
}
