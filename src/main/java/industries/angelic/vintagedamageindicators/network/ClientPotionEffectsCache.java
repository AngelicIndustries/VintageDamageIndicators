package industries.angelic.vintagedamageindicators.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPotionEffectsCache {

    private static final Map<Integer, List<CachedPotionEntry>> EFFECTS_BY_ENTITY = new ConcurrentHashMap<>();

    private ClientPotionEffectsCache() {}

    public static void put(int entityId, List<EntityPotionEffectsMessage.PotionEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            EFFECTS_BY_ENTITY.put(Integer.valueOf(entityId), Collections.<CachedPotionEntry>emptyList());
            return;
        }

        List<CachedPotionEntry> cachedEntries = new ArrayList<>(entries.size());
        for (EntityPotionEffectsMessage.PotionEntry entry : entries) {
            cachedEntries.add(new CachedPotionEntry(entry));
        }
        EFFECTS_BY_ENTITY.put(Integer.valueOf(entityId), cachedEntries);
    }

    public static List<EntityPotionEffectsMessage.PotionEntry> get(int entityId) {
        List<CachedPotionEntry> cachedEntries = EFFECTS_BY_ENTITY.get(Integer.valueOf(entityId));
        if (cachedEntries == null) {
            return null;
        }
        if (cachedEntries.isEmpty()) {
            return Collections.emptyList();
        }

        List<EntityPotionEffectsMessage.PotionEntry> entries = new ArrayList<>(cachedEntries.size());
        for (CachedPotionEntry cachedEntry : cachedEntries) {
            entries.add(cachedEntry.toMessageEntry());
        }
        return entries;
    }

    public static void tick() {
        for (List<CachedPotionEntry> entries : EFFECTS_BY_ENTITY.values()) {
            for (CachedPotionEntry entry : entries) {
                entry.tick();
            }
        }
    }

    public static void clear() {
        EFFECTS_BY_ENTITY.clear();
    }

    private static final class CachedPotionEntry {

        private final boolean showType;
        private final int potionId;
        private final boolean showDuration;
        private int duration;

        private CachedPotionEntry(EntityPotionEffectsMessage.PotionEntry entry) {
            this.showType = entry.showType;
            this.potionId = entry.potionId;
            this.showDuration = entry.showDuration;
            this.duration = entry.duration;
        }

        private void tick() {
            if (this.showDuration && this.duration > 0) {
                this.duration--;
            }
        }

        private EntityPotionEffectsMessage.PotionEntry toMessageEntry() {
            return new EntityPotionEffectsMessage.PotionEntry(
                this.showType,
                this.potionId,
                this.showDuration,
                this.duration);
        }
    }
}
