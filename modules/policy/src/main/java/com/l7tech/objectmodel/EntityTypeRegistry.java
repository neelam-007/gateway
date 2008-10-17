package com.l7tech.objectmodel;

import java.util.Map;
import java.util.HashMap;

public class EntityTypeRegistry {

    private static class EntityTypeRegistryHolder {
        private static final Map<EntityType,Class<? extends Entity>> REGISTRY = getInitValue();
    }

    // only initialization needs to be synchronized
    private static final Object INIT_LOCK = new Object();
    private static Map<EntityType,Class<? extends Entity>> tempRegistry;

    private static Map<EntityType,Class<? extends Entity>> getInitValue() {
        synchronized (INIT_LOCK) {
            if (tempRegistry == null)
                throw new IllegalStateException("Initialization data not provided for the entity type registry.");
            return tempRegistry;
        }
    }

    public EntityTypeRegistry(Map<String,String> registry) throws ClassNotFoundException {

        if (registry == null)
            throw new IllegalArgumentException("Null registry map given to constructor.");

        synchronized (INIT_LOCK) {
            tempRegistry = new HashMap<EntityType, Class<? extends Entity>>();
            for (String type : registry.keySet()) {
                Class clazz = Class.forName(registry.get(type));
                if (Entity.class.isAssignableFrom(clazz)) {
                    tempRegistry.put(EntityType.valueOf(type), clazz);
                }
            }

            // trigger holder instance initialization
            if (EntityTypeRegistryHolder.REGISTRY != tempRegistry) {
                throw new IllegalStateException("Entity type registry already initialized.");
            }
        }
    }

    public static Class<? extends Entity> getEntityClass(EntityType type) {
        return EntityTypeRegistryHolder.REGISTRY.get(type);
    }
}
