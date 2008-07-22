package com.l7tech.server;

import java.util.Map;
import java.util.HashMap;

import com.l7tech.server.cluster.ClusterPropertyCache;

/**
 * Stub mode ServerConfig.
 */
public class ServerConfigStub extends ServerConfig {
    private final Map<String, String> overrides = new HashMap<String, String>();

    public String getPropertyUncached(String propName, boolean includeClusterProperties) {
        if (overrides != null && overrides.containsKey(propName)) return overrides.get(propName);
        return super.getPropertyUncached(propName, includeClusterProperties);
    }

    public void setClusterPropertyCache(final ClusterPropertyCache clusterPropertyCache) {
        super.setClusterPropertyCache(clusterPropertyCache);
        try {
            ServerConfig.getInstance().setClusterPropertyCache(clusterPropertyCache);
        } catch(IllegalStateException ise) {
            // ignore already set exception
        }
    }

    /**
     * Specify a property for testing.  Overrides all other ways of getting a property value.
     *
     * @param name   the property name
     * @param value  the value to force it to have from now on
     */
    public boolean putProperty(String name, String value) {
        super.putProperty(name, value);
        overrides.put(name, value);
        invalidateCachedProperty(name);
        return true;
    }
}
