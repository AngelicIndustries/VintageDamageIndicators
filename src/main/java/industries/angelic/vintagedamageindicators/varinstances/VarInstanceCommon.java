package industries.angelic.vintagedamageindicators.varinstances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import industries.angelic.vintagedamageindicators.Config;
import industries.angelic.vintagedamageindicators.MobTypes;
import industries.angelic.vintagedamageindicators.VintageDamageIndicators;
import industries.angelic.vintagedamageindicators.util.XSTR;

public class VarInstanceCommon {

    public XSTR rand = new XSTR();

    public HashMap<Class<?>, EntityOverride> entityOverrides;
    public LinkedHashMap<String, EntityOverride> entityOverridesByClassName;
    public ArrayList<String> preservedEntityOverrideEntries;

    public void postInitHook() {
        buildOverrideList();
    }

    public void buildOverrideList() {
        ensureOverrideState();
        entityOverrides.clear();
        entityOverridesByClassName.clear();
        preservedEntityOverrideEntries.clear();

        for (EntityOverride source : Config.getEntityOverrides()) {
            if (source == null || source.className == null
                || source.className.trim()
                    .isEmpty()) {
                continue;
            }
            EntityOverride eo = source.copy();
            try {
                entityOverridesByClassName.put(eo.className, eo);

                if (!eo.enable) {
                    continue;
                }
                Class<?> cls = Class.forName(eo.className);
                entityOverrides.put(cls, eo);
            } catch (ClassNotFoundException e) {
                VintageDamageIndicators.debug("Skipping unknown override class: " + eo.className);
                preservedEntityOverrideEntries.add(eo.serialize());
            }
        }
    }

    public EntityOverride getEntityOverride(Class<?> entityClass) {
        if (entityOverrides == null || entityClass == null) {
            return null;
        }
        return entityOverrides.get(entityClass);
    }

    public EntityOverride getEntityOverride(String className) {
        if (entityOverridesByClassName == null || className == null) {
            return null;
        }
        return entityOverridesByClassName.get(className);
    }

    public boolean isPopoffEnabled(Class<?> entityClass) {
        if (entityOverridesByClassName == null || entityClass == null) {
            return true;
        }
        EntityOverride override = entityOverridesByClassName.get(entityClass.getName());
        return override == null || override.popoffEnabled;
    }

    public LinkedHashMap<String, EntityOverride> copyEntityOverridesByClassName() {
        LinkedHashMap<String, EntityOverride> copy = new LinkedHashMap<>();
        if (entityOverridesByClassName == null) {
            return copy;
        }
        for (Map.Entry<String, EntityOverride> entry : entityOverridesByClassName.entrySet()) {
            copy.put(
                entry.getKey(),
                entry.getValue()
                    .copy());
        }
        return copy;
    }

    public ArrayList<String> copyPreservedEntityOverrideEntries() {
        if (preservedEntityOverrideEntries == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(preservedEntityOverrideEntries);
    }

    public void replaceEntityOverrides(Map<String, EntityOverride> overridesByClassName,
        List<String> preservedEntries) {
        ensureOverrideState();
        entityOverrides.clear();
        entityOverridesByClassName.clear();
        preservedEntityOverrideEntries.clear();

        ArrayList<EntityOverride> toPersist = new ArrayList<>();

        if (overridesByClassName != null) {
            for (Map.Entry<String, EntityOverride> entry : overridesByClassName.entrySet()) {
                EntityOverride override = entry.getValue();
                if (override == null) {
                    continue;
                }
                EntityOverride stored = override.copy();
                if (stored.className == null || stored.className.isEmpty()) {
                    stored.className = entry.getKey();
                }
                if (stored.className == null || stored.className.isEmpty()) {
                    continue;
                }

                entityOverridesByClassName.put(stored.className, stored);
                toPersist.add(stored.copy());

                if (!stored.enable) {
                    continue;
                }

                try {
                    Class<?> cls = Class.forName(stored.className);
                    entityOverrides.put(cls, stored);
                } catch (ClassNotFoundException e) {
                    VintageDamageIndicators.debug("Skipping unknown override class while saving: " + stored.className);
                }
            }
        }

        if (preservedEntries != null) {
            for (String serialized : preservedEntries) {
                if (serialized == null || serialized.trim()
                    .isEmpty()) {
                    continue;
                }
                EntityOverride parsed = EntityOverride.deserialize(serialized);
                if (parsed.className == null || parsed.className.trim()
                    .isEmpty()) {
                    continue;
                }
                if (entityOverridesByClassName.containsKey(parsed.className)) {
                    continue;
                }
                preservedEntityOverrideEntries.add(serialized);
                entityOverridesByClassName.put(parsed.className, parsed);
                toPersist.add(parsed.copy());
                if (!parsed.enable) {
                    continue;
                }
                try {
                    Class<?> cls = Class.forName(parsed.className);
                    entityOverrides.put(cls, parsed);
                } catch (ClassNotFoundException e) {
                    VintageDamageIndicators
                        .debug("Skipping unknown preserved override class while saving: " + parsed.className);
                }
            }
        }

        Config.setEntityOverrides(toPersist);
    }

    private void ensureOverrideState() {
        if (entityOverrides == null) {
            entityOverrides = new HashMap<>();
        }
        if (entityOverridesByClassName == null) {
            entityOverridesByClassName = new LinkedHashMap<>();
        }
        if (preservedEntityOverrideEntries == null) {
            preservedEntityOverrideEntries = new ArrayList<>();
        }
    }

    public static class EntityOverride {

        public String className;
        public String displayName;
        public boolean appendBabyName;
        public float babyScaleModifier;
        public float babyScaleFactor;
        public float scaleFactor;
        public float sizeModifier;
        public float xOffset;
        public float yOffset;
        public float babyXOffset;
        public float babyYOffset;
        public float yawOffset;
        public float pitchOffset;
        public float rollOffset;
        public MobTypes type;
        public boolean popoffEnabled;
        public boolean enable;

        public EntityOverride() {
            this.type = MobTypes.UNKNOWN;
            this.popoffEnabled = true;
            this.enable = true;
        }

        public EntityOverride(String className, String displayName, boolean appendBabyName, float babyScaleModifier,
            float babyScaleFactor, float scaleFactor, float sizeModifier, float xOffset, float yOffset,
            float babyXOffset, float babyYOffset, float yawOffset, float pitchOffset, float rollOffset, MobTypes type,
            boolean popoffEnabled, boolean enable) {
            this.className = className;
            this.displayName = displayName;
            this.appendBabyName = appendBabyName;
            this.babyScaleModifier = babyScaleModifier;
            this.babyScaleFactor = babyScaleFactor;
            this.scaleFactor = scaleFactor;
            this.sizeModifier = sizeModifier;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.babyXOffset = babyXOffset;
            this.babyYOffset = babyYOffset;
            this.yawOffset = yawOffset;
            this.pitchOffset = pitchOffset;
            this.rollOffset = rollOffset;
            this.type = type;
            this.popoffEnabled = popoffEnabled;
            this.enable = enable;
        }

        public EntityOverride copy() {
            EntityOverride copy = new EntityOverride();
            copy.className = className;
            copy.displayName = displayName;
            copy.appendBabyName = appendBabyName;
            copy.babyScaleModifier = babyScaleModifier;
            copy.babyScaleFactor = babyScaleFactor;
            copy.scaleFactor = scaleFactor;
            copy.sizeModifier = sizeModifier;
            copy.xOffset = xOffset;
            copy.yOffset = yOffset;
            copy.babyXOffset = babyXOffset;
            copy.babyYOffset = babyYOffset;
            copy.yawOffset = yawOffset;
            copy.pitchOffset = pitchOffset;
            copy.rollOffset = rollOffset;
            copy.type = type;
            copy.popoffEnabled = popoffEnabled;
            copy.enable = enable;
            return copy;
        }

        public String serialize() {
            return String.format(
                "[%s]: <Name>: %s, <Append Baby Name>: %b, <Baby Scale Modifier>: %f, <Baby Scale Factor>: %f, <Scale Factor>: %f, <Size Modifier>: %f, <X Offset>: %f, <Y Offset>: %f, <Baby X Offset>: %f, <Baby Y Offset>: %f, <Yaw Offset>: %f, <Pitch Offset>: %f, <Roll Offset>: %f, <Type>: %s, <Popoff>: %b, <Enable>: %b",
                className,
                displayName == null ? "" : displayName,
                appendBabyName,
                babyScaleModifier,
                babyScaleFactor,
                scaleFactor,
                sizeModifier,
                xOffset,
                yOffset,
                babyXOffset,
                babyYOffset,
                yawOffset,
                pitchOffset,
                rollOffset,
                type.toString(),
                popoffEnabled,
                enable);
        }

        public static EntityOverride deserialize(String serialized) {
            EntityOverride entity = new EntityOverride();
            if (serialized == null) {
                return entity;
            }

            serialized = serialized.trim();
            if (serialized.isEmpty()) {
                return entity;
            }

            int classNameEndIndex = serialized.indexOf("]:");
            if (classNameEndIndex == -1) {
                return entity;
            }
            entity.className = serialized.substring(1, classNameEndIndex)
                .trim();

            String data = serialized.substring(classNameEndIndex + 2)
                .trim();

            String[] parts = data.split(",\\s*");

            for (String part : parts) {
                if (part.startsWith("<Name>:")) {
                    entity.displayName = part.substring("<Name>:".length())
                        .trim();
                } else if (part.startsWith("<Append Baby Name>:")) {
                    entity.appendBabyName = Boolean.parseBoolean(
                        part.substring("<Append Baby Name>:".length())
                            .trim());
                } else if (part.startsWith("<Baby Scale Modifier>:")) {
                    entity.babyScaleModifier = Float.parseFloat(
                        part.substring("<Baby Scale Modifier>:".length())
                            .trim());
                } else if (part.startsWith("<Baby Scale Factor>:")) {
                    entity.babyScaleFactor = Float.parseFloat(
                        part.substring("<Baby Scale Factor>:".length())
                            .trim());
                } else if (part.startsWith("<Scale Factor>:")) {
                    entity.scaleFactor = Float.parseFloat(
                        part.substring("<Scale Factor>:".length())
                            .trim());
                } else if (part.startsWith("<Size Modifier>:")) {
                    entity.sizeModifier = Float.parseFloat(
                        part.substring("<Size Modifier>:".length())
                            .trim());
                } else if (part.startsWith("<X Offset>:")) {
                    entity.xOffset = Float.parseFloat(
                        part.substring("<X Offset>:".length())
                            .trim());
                } else if (part.startsWith("<Y Offset>:")) {
                    entity.yOffset = Float.parseFloat(
                        part.substring("<Y Offset>:".length())
                            .trim());
                } else if (part.startsWith("<Baby X Offset>:")) {
                    entity.babyXOffset = Float.parseFloat(
                        part.substring("<Baby X Offset>:".length())
                            .trim());
                } else if (part.startsWith("<Baby Y Offset>:")) {
                    entity.babyYOffset = Float.parseFloat(
                        part.substring("<Baby Y Offset>:".length())
                            .trim());
                } else if (part.startsWith("<Yaw Offset>:")) {
                    entity.yawOffset = Float.parseFloat(
                        part.substring("<Yaw Offset>:".length())
                            .trim());
                } else if (part.startsWith("<Pitch Offset>:")) {
                    entity.pitchOffset = Float.parseFloat(
                        part.substring("<Pitch Offset>:".length())
                            .trim());
                } else if (part.startsWith("<Roll Offset>:")) {
                    entity.rollOffset = Float.parseFloat(
                        part.substring("<Roll Offset>:".length())
                            .trim());
                } else if (part.startsWith("<Type>:")) {
                    String mobType = part.substring("<Type>:".length())
                        .trim();
                    try {
                        entity.type = MobTypes.valueOf(mobType);
                    } catch (IllegalArgumentException ignored) {
                        //
                    }
                } else if (part.startsWith("<Popoff>:")) {
                    entity.popoffEnabled = Boolean.parseBoolean(
                        part.substring("<Popoff>:".length())
                            .trim());
                } else if (part.startsWith("<Enable>:")) {
                    entity.enable = Boolean.parseBoolean(
                        part.substring("<Enable>:".length())
                            .trim());
                }
            }

            return entity;
        }
    }
}
