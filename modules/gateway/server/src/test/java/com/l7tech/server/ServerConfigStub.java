package com.l7tech.server;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.optional;

/**
 * Stub mode ServerConfig.
 */
public class ServerConfigStub extends ServerConfig {
    private final Map<String, String> overrides = new HashMap<String, String>();

    public ServerConfigStub() {
        super( new Properties( ), 0L );
    }

    @Override
    protected Option<String> getConfigPropertyDirect( final String propertyName ) {
        if (overrides != null && overrides.containsKey(propertyName)) return optional(overrides.get(propertyName));
        return super.getConfigPropertyDirect( propertyName );
    }

    @Override
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
    @Override
    public boolean putProperty(String name, String value) {
        super.putProperty(name, value);
        overrides.put(name, value);
        ConfigFactory.clearCachedConfig();
        return true;
    }
}
