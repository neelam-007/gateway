package com.l7tech.external.assertions.remotecacheassertion;

import java.util.ArrayList;
import java.util.List;

public enum RemoteCacheTypes {
    /**
     * Current cache types supported by RemoteCache
     * The ENUM values refer to the entity types that were originally implemented as Static Strings. The values are not modified to ensure upgrades will work with no issues
     */
    Memcached("memcached"), Terracotta("terracotta"), Coherence("coherence"), GemFire("gemfire"), Redis("redis");
    private String entityType;

    private RemoteCacheTypes(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityType() {
        return entityType;
    }

    /**
     * returns ENUM based on the given entity type
     *
     * @param type - entity type
     * @return RemoteCacheTypes for the given entity type
     */
    public static RemoteCacheTypes getEntityEnumType(String type) {
        for (RemoteCacheTypes cacheType : values()) {
            if (cacheType.getEntityType().equals(type)) {
                return cacheType;
            }
        }
        throw new IllegalArgumentException(type);
    }

    /**
     * @return An array of entity types
     */
    public static String[] getEntityTypes() {
        List<String> entityTypes = new ArrayList<>();
        for (RemoteCacheTypes type : RemoteCacheTypes.values()) {
            entityTypes.add(type.getEntityType());
        }
        return entityTypes.toArray(new String[entityTypes.size()]);
    }
}
