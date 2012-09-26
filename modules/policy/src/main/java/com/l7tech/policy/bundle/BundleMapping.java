package com.l7tech.policy.bundle;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides mapping of a reference from a bundle to a new value before installing on a Gateway
 */
public class BundleMapping implements Serializable {

    /**
     * All supported types
     */
    public enum EntityType{JDBC_CONNECTION}

    public void addMapping(EntityType entityType, String oldValue, String newValue) {
        switch (entityType) {
            case JDBC_CONNECTION:
                oldJdbcToNewConnRef.put(oldValue, newValue);
                break;
            default:
                //coding error
                throw new RuntimeException("Unsupported entity type: " + entityType);
        }
    }

    @NotNull
    public Map<String, String> getJdbcMappings() {
        return Collections.unmodifiableMap(new HashMap<String, String>(oldJdbcToNewConnRef));
    }

    // - PRIVATE

    private final Map<String,String> oldJdbcToNewConnRef = new HashMap<String, String>();

}
