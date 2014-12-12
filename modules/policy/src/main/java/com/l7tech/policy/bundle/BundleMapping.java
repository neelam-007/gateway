package com.l7tech.policy.bundle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public enum Type {JDBC_CONNECTION_NAME, ENCAPSULATE_ASSERTION_GUID}

    public void addMapping(Type type, String oldValue, String newValue) {
        switch (type) {
            case JDBC_CONNECTION_NAME:
                oldToNewJdbcConnName.put(oldValue, newValue);
                break;
            case ENCAPSULATE_ASSERTION_GUID:
                oldToNewEncassGuid.put(oldValue, newValue);
                break;
            default:
                //coding error
                throw new RuntimeException("Unsupported mapping type: " + type);
        }
    }

    @NotNull
    public Map<String, String> getMappings(final Type type) {
        switch (type) {
            case JDBC_CONNECTION_NAME:
                return Collections.unmodifiableMap(new HashMap<>(oldToNewJdbcConnName));
            case ENCAPSULATE_ASSERTION_GUID:
                return Collections.unmodifiableMap(new HashMap<>(oldToNewEncassGuid));
            default:
                //coding error
                throw new RuntimeException("Unsupported mapping type: " + type);
        }
    }

    @Nullable
    public String getMapping(final Type type, final String oldValue) {
        return getMappings(type).get(oldValue);
    }

    // - PRIVATE

    private final Map<String,String> oldToNewJdbcConnName = new HashMap<>();
    private final Map<String,String> oldToNewEncassGuid = new HashMap<>();

}
